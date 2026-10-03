package com.drivingschool.instructor;

import com.drivingschool.common.api.ApiSecurityExceptionHandler;
import com.drivingschool.instructor.dto.CreateInstructorRequest;
import com.drivingschool.instructor.dto.InstructorResponse;
import com.drivingschool.security.AppUserDetails;
import com.drivingschool.security.JwtAuthenticationFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {InstructorController.class, InstructorMeController.class})
@Import({
        com.drivingschool.security.SecurityConfig.class,
        ApiSecurityExceptionHandler.class,
        InstructorSecurityMockMvcTest.MethodSecurityConfiguration.class
})
class InstructorSecurityMockMvcTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityConfiguration {
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InstructorService instructorService;

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
    void candidateIsForbiddenFromInstructorEndpoints() throws Exception {
        UUID id = UUID.randomUUID();
        String request = """
                {
                  "firstName":"New",
                  "lastName":"Instructor",
                  "phone":"+21612345678",
                  "email":"new.instructor@example.com",
                  "licenseNumber":"LIC-12345",
                  "initialPassword":"at-least-twelve"
                }
                """;
        String updateRequest = """
                {
                  "firstName":"New",
                  "lastName":"Instructor",
                  "phone":"+21612345678",
                  "email":"new.instructor@example.com",
                  "licenseNumber":"LIC-12345",
                  "status":"ACTIVE"
                }
                """;

        mockMvc.perform(get("/api/instructors").with(user("candidate").roles("CANDIDATE")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/instructors/{id}", id).with(user("candidate").roles("CANDIDATE")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/instructors/me").with(user("candidate").roles("CANDIDATE")))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/instructors")
                        .with(user("candidate").roles("CANDIDATE"))
                        .contentType(APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/instructors/{id}", id)
                        .with(user("candidate").roles("CANDIDATE"))
                        .contentType(APPLICATION_JSON)
                        .content(updateRequest))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/instructors/{id}", id)
                        .with(user("candidate").roles("CANDIDATE")))
                .andExpect(status().isForbidden());

        verify(instructorService, never()).findAll(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
        verify(instructorService, never()).findById(id);
        verify(instructorService, never()).findForUser(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void instructorCanOnlyReadOwnMeEndpoint() throws Exception {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        AppUserDetails principal = new AppUserDetails(
                userId,
                "instructor@example.com",
                "not-returned",
                true,
                List.of(new SimpleGrantedAuthority("ROLE_INSTRUCTOR"))
        );
        when(instructorService.findForUser(principal)).thenReturn(response(id, userId));

        mockMvc.perform(get("/api/instructors").with(user(principal)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/instructors/{id}", id).with(user(principal)))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/instructors")
                        .with(user(principal))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName":"New",
                                  "lastName":"Instructor",
                                  "phone":"+21612345678",
                                  "email":"new.instructor@example.com",
                                  "licenseNumber":"LIC-12345",
                                  "initialPassword":"at-least-twelve"
                                }
                                """))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/instructors/{id}", id)
                        .with(user(principal))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName":"New",
                                  "lastName":"Instructor",
                                  "phone":"+21612345678",
                                  "email":"new.instructor@example.com",
                                  "licenseNumber":"LIC-12345",
                                  "status":"ACTIVE"
                                }
                                """))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/instructors/{id}", id).with(user(principal)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/instructors/me").with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.initialPassword").doesNotExist());

        verify(instructorService).findForUser(principal);
        verify(instructorService, never()).findById(id);
    }

    @Test
    void adminCanCreateInstructor() throws Exception {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(instructorService.create(org.mockito.ArgumentMatchers.any(CreateInstructorRequest.class)))
                .thenReturn(response(id, userId));
        String request = """
                {
                  "firstName":"New",
                  "lastName":"Instructor",
                  "phone":"+21612345678",
                  "email":"new.instructor@example.com",
                  "licenseNumber":"LIC-12345",
                  "initialPassword":"at-least-twelve"
                }
                """;

        mockMvc.perform(post("/api/instructors")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.initialPassword").doesNotExist());

        verify(instructorService).create(org.mockito.ArgumentMatchers.any(CreateInstructorRequest.class));
    }

    private InstructorResponse response(UUID id, UUID userId) {
        return new InstructorResponse(
                id, userId, "New", "Instructor", "+21612345678",
                "new.instructor@example.com", "LIC-12345", InstructorStatus.ACTIVE,
                Instant.now(), Instant.now()
        );
    }
}
