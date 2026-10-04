package com.drivingschool.candidate;

import com.drivingschool.candidate.dto.CandidateResponse;
import com.drivingschool.security.AppUserDetails;
import com.drivingschool.security.JwtAuthenticationFilter;
import com.drivingschool.common.api.ApiSecurityExceptionHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {CandidateController.class, MeController.class})
@Import({
        com.drivingschool.security.SecurityConfig.class,
        ApiSecurityExceptionHandler.class,
        CandidateSecurityMockMvcTest.MethodSecurityConfiguration.class
})
class CandidateSecurityMockMvcTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityConfiguration {
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CandidateService candidateService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void passThroughJwtFilter() throws Exception {
        doAnswer(invocation -> {
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(jwtAuthenticationFilter).doFilter(
                org.mockito.ArgumentMatchers.any(ServletRequest.class),
                org.mockito.ArgumentMatchers.any(ServletResponse.class),
                org.mockito.ArgumentMatchers.any(FilterChain.class)
        );
    }

    @Test
    void candidateCannotAccessCandidateManagementOrAnotherCandidateProfile() throws Exception {
        UUID otherCandidateId = UUID.randomUUID();

        mockMvc.perform(get("/api/candidates").with(user("candidate").roles("CANDIDATE")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/candidates/{id}", otherCandidateId)
                        .with(user("candidate").roles("CANDIDATE")))
                .andExpect(status().isForbidden());

        verify(candidateService, never()).findAll(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
        verify(candidateService, never()).findById(otherCandidateId);
    }

    @Test
    void instructorCanReadButCannotCreateUpdateOrDeleteCandidates() throws Exception {
        when(candidateService.findAll(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        )).thenReturn(org.springframework.data.domain.Page.empty());

        mockMvc.perform(get("/api/candidates").with(user("instructor").roles("INSTRUCTOR")))
                .andExpect(status().isOk());

        String request = """
                {
                  "firstName":"New",
                  "lastName":"Candidate",
                  "phone":"+21612345678",
                  "email":"new@example.com",
                  "registrationDate":"2026-10-03",
                  "status":"ACTIVE"
                }
                """;
        UUID id = UUID.randomUUID();
        RequestPostProcessor instructor = user("instructor").roles("INSTRUCTOR");

        mockMvc.perform(post("/api/candidates").with(instructor)
                        .contentType(APPLICATION_JSON).content(request))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/candidates/{id}", id).with(instructor)
                        .contentType(APPLICATION_JSON).content(request))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/candidates/{id}", id).with(instructor))
                .andExpect(status().isForbidden());

        verify(candidateService, never()).create(org.mockito.ArgumentMatchers.any());
        verify(candidateService, never()).update(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(candidateService, never()).delete(id);
    }

    @Test
    void adminCanUpdateCandidate() throws Exception {
        UUID id = UUID.randomUUID();
        String request = """
                {
                  "firstName":"Updated",
                  "lastName":"Candidate",
                  "phone":"+21612345678",
                  "email":"updated@example.com",
                  "registrationDate":"2026-10-03",
                  "status":"ACTIVE"
                }
                """;

        mockMvc.perform(put("/api/candidates/{id}", id)
                        .with(user("admin").roles("ADMIN"))
                        .contentType(APPLICATION_JSON).content(request))
                .andExpect(status().isOk());

        verify(candidateService).update(org.mockito.ArgumentMatchers.eq(id),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void candidateMeEndpointUsesOnlyTheAuthenticatedPrincipalId() throws Exception {
        UUID ownUserId = UUID.randomUUID();
        AppUserDetails principal = new AppUserDetails(
                ownUserId,
                "candidate@example.com",
                "encoded-password",
                true,
                List.of(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
        );
        when(candidateService.findForUser(principal)).thenReturn(new CandidateResponse(
                UUID.randomUUID(),
                ownUserId,
                "Candidate",
                "Owner",
                "+21612345678",
                "candidate@example.com",
                LocalDate.of(2000, 1, 1),
                null,
                LocalDate.of(2026, 1, 1),
                CandidateStatus.ACTIVE,
                null,
                Instant.now(),
                Instant.now(),
                List.of("CANDIDATE")
        ));

        mockMvc.perform(get("/api/me").with(user(principal)))
                .andExpect(status().isOk());

        verify(candidateService).findForUser(principal);
    }
}
