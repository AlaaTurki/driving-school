package com.drivingschool.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import com.drivingschool.candidate.CandidateRepository;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class AuthFlowIntegrationTest {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:17-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        properties.add("spring.datasource.username", POSTGRES::getUsername);
        properties.add("spring.datasource.password", POSTGRES::getPassword);
        properties.add("app.jwt.secret", () -> java.util.Base64.getEncoder().encodeToString(new byte[32]));
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private CandidateRepository candidateRepository;

    @Test
    void registerLoginAndRefreshRotateHashedRefreshTokens() throws Exception {
        String email = "candidate-" + UUID.randomUUID() + "@example.com";
        String password = "strong-password";
        String registration = objectMapper.writeValueAsString(java.util.Map.of(
                "fullName", "Integration Candidate",
                "email", email,
                "phone", "+1 555 123 4567",
                "password", password
        ));

        JsonNode registered = objectMapper.readTree(mockMvc.perform(post("/api/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content(registration))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());
        assertThat(registered.path("accessToken").asText()).isNotBlank();
        assertThat(registered.path("refreshToken").asText()).isNotBlank();
        assertThat(candidateRepository.findByUser_Id(UUID.fromString(registered.path("userId").asText())))
                .isPresent();
        Instant accessExpiresAt = Instant.parse(registered.path("expiresAt").asText());
        assertThat(accessExpiresAt).isAfter(Instant.now().plus(Duration.ofMinutes(14)));
        assertThat(accessExpiresAt).isBefore(Instant.now().plus(Duration.ofMinutes(15)).plusSeconds(1));

        String login = objectMapper.writeValueAsString(java.util.Map.of(
                "email", email,
                "password", password
        ));
        JsonNode loggedIn = objectMapper.readTree(mockMvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content(login))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        String originalRefreshToken = loggedIn.path("refreshToken").asText();
        java.util.List<String> storedHashes = jdbcTemplate.queryForList(
                "SELECT token_hash FROM refresh_tokens WHERE user_id = (SELECT id FROM users WHERE email = ?)",
                String.class,
                email
        );
        assertThat(storedHashes).hasSize(2).allSatisfy(hash ->
                assertThat(hash).hasSize(64).isNotEqualTo(originalRefreshToken));

        String refreshRequest = objectMapper.writeValueAsString(java.util.Map.of(
                "refreshToken", originalRefreshToken
        ));
        JsonNode refreshed = objectMapper.readTree(mockMvc.perform(post("/api/auth/refresh")
                        .contentType(APPLICATION_JSON)
                        .content(refreshRequest))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        assertThat(refreshed.path("accessToken").asText()).isNotEqualTo(loggedIn.path("accessToken").asText());
        assertThat(refreshed.path("refreshToken").asText()).isNotEqualTo(originalRefreshToken);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(APPLICATION_JSON)
                        .content(refreshRequest))
                .andExpect(status().isUnauthorized());

        String logoutRequest = objectMapper.writeValueAsString(java.util.Map.of(
                "refreshToken", refreshed.path("refreshToken").asText()
        ));
        mockMvc.perform(post("/api/auth/logout")
                        .contentType(APPLICATION_JSON)
                        .content(logoutRequest))
                .andExpect(status().isNoContent());
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(APPLICATION_JSON)
                        .content(logoutRequest))
                .andExpect(status().isUnauthorized());
    }
}
