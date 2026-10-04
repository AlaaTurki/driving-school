package com.drivingschool.instructor;

import com.drivingschool.auth.EmailAlreadyRegisteredException;
import com.drivingschool.instructor.dto.CreateInstructorRequest;
import com.drivingschool.instructor.dto.UpdateInstructorRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class InstructorIntegrationTest {

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
    private InstructorService instructorService;

    @Autowired
    private InstructorRepository instructorRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void migratesSearchesPaginatesAndEnforcesUniqueEmailAndLicense() {
        String marker = UUID.randomUUID().toString().substring(0, 8);
        var first = instructorService.create(request(
                "Sophie", "Search", "sophie-" + marker + "@example.com", "LIC-" + marker + "-A"
        ));
        var second = instructorService.create(request(
                "Alex", "Search", "alex-" + marker + "@example.com", "LIC-" + marker + "-B"
        ));
        var pageable = PageRequest.of(
                0,
                1,
                Sort.by("lastName").ascending().and(Sort.by("firstName").ascending())
        );

        var allSearchMatches = instructorService.findAll(marker, null, pageable);
        var secondPage = instructorService.findAll(marker, null, pageable.next());
        var nameMatch = instructorService.findAll("sophie search", null, pageable);
        var emailMatch = instructorService.findAll(first.email(), null, pageable);
        var phoneMatch = instructorService.findAll("+21612345678", null, pageable);
        var licenseMatch = instructorService.findAll("lic-" + marker + "-a", null, pageable);
        var inactiveMatches = instructorService.findAll(marker, InstructorStatus.INACTIVE, pageable);

        assertThat(jdbcTemplate.queryForObject("SELECT to_regclass('public.instructors')", String.class))
                .isEqualTo("instructors");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pg_indexes WHERE tablename = 'instructors' "
                        + "AND indexname IN ('idx_instructors_last_first_name', 'idx_instructors_status')",
                Integer.class
        )).isEqualTo(2);
        assertThat(allSearchMatches.getTotalElements()).isEqualTo(2);
        assertThat(allSearchMatches.getTotalPages()).isEqualTo(2);
        assertThat(allSearchMatches.getContent()).hasSize(1);
        assertThat(allSearchMatches.getContent().getFirst().id()).isEqualTo(second.id());
        assertThat(secondPage.getContent()).hasSize(1);
        assertThat(secondPage.getContent().getFirst().id()).isEqualTo(first.id());
        assertThat(nameMatch.getTotalElements()).isEqualTo(1);
        assertThat(emailMatch.getTotalElements()).isEqualTo(1);
        assertThat(phoneMatch.getTotalElements()).isEqualTo(2);
        assertThat(licenseMatch.getTotalElements()).isEqualTo(1);
        assertThat(licenseMatch.getContent().getFirst().id()).isEqualTo(first.id());
        assertThat(inactiveMatches.getTotalElements()).isZero();

        assertThatThrownBy(() -> instructorService.create(request(
                "Duplicate", "Email", first.email(), "LIC-" + marker + "-C"
        ))).isInstanceOf(EmailAlreadyRegisteredException.class);
        assertThatThrownBy(() -> instructorService.create(request(
                "Duplicate", "License", "duplicate-" + marker + "@example.com", first.licenseNumber()
        ))).isInstanceOf(InstructorLicenseAlreadyRegisteredException.class);

        assertThat(instructorRepository.findById(second.id())).isPresent();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT password_hash FROM users WHERE id = ?",
                String.class,
                first.userId()
        )).startsWith("$2");
    }

    @Test
    void inactiveInstructorCannotLogIn() throws Exception {
        String email = "inactive-" + UUID.randomUUID() + "@example.com";
        String password = "integration-password";
        var created = instructorService.create(request("Inactive", "Instructor", email, "LIC-" + UUID.randomUUID()));

        instructorService.update(created.id(), new UpdateInstructorRequest(
                created.firstName(),
                created.lastName(),
                created.phone(),
                created.email(),
                created.licenseNumber(),
                InstructorStatus.INACTIVE
        ));

        String loginRequest = objectMapper.writeValueAsString(java.util.Map.of(
                "email", email,
                "password", password
        ));
        mockMvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content(loginRequest))
                .andExpect(status().isUnauthorized());
    }

    private CreateInstructorRequest request(String firstName, String lastName, String email, String license) {
        return new CreateInstructorRequest(
                firstName,
                lastName,
                "+21612345678",
                email,
                license,
                "integration-password"
        );
    }
}
