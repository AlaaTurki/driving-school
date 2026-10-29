package com.drivingschool.skills;

import com.drivingschool.common.api.ApiSecurityExceptionHandler;
import com.drivingschool.exception.GlobalExceptionHandler;
import com.drivingschool.security.AppUserDetails;
import com.drivingschool.security.JwtAuthenticationFilter;
import com.drivingschool.skills.dto.CandidateSkillsResponse;
import com.drivingschool.skills.dto.UpdateSkillLevelRequest;
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

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {SkillsController.class, CandidateSkillsController.class, MeSkillsController.class})
@Import({
        com.drivingschool.security.SecurityConfig.class,
        ApiSecurityExceptionHandler.class,
        GlobalExceptionHandler.class,
        SkillsSecurityMockMvcTest.MethodSecurityConfiguration.class
})
class SkillsSecurityMockMvcTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityConfiguration {
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SkillsService skillsService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void passThroughJwtFilter() throws Exception {
        doAnswer(invocation -> {
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(jwtAuthenticationFilter).doFilter(any(ServletRequest.class),
                any(ServletResponse.class), any(FilterChain.class));
    }

    @Test
    void instructorCannotReadOrUpdateSkillsForAnotherInstructorsCandidate() throws Exception {
        UUID candidateId = UUID.randomUUID();
        UUID skillId = UUID.randomUUID();
        AppUserDetails instructor = userWithRole("instructor@example.com", "INSTRUCTOR");
        doThrow(new InstructorSkillsAccessDeniedException()).when(skillsService)
                .updateSkill(eq(candidateId), eq(skillId), any(UpdateSkillLevelRequest.class), eq(instructor));
        doThrow(new InstructorSkillsAccessDeniedException()).when(skillsService)
                .getCandidateSkills(eq(candidateId), eq(instructor));

        mockMvc.perform(get("/api/candidates/{candidateId}/skills", candidateId).with(user(instructor)))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/candidates/{candidateId}/skills/{skillId}", candidateId, skillId)
                        .with(user(instructor)).contentType(APPLICATION_JSON).content("{\"level\":40}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void candidateCanReadOnlyOwnProgressAndCannotReadOrModifyCandidateSkills() throws Exception {
        AppUserDetails candidate = userWithRole("candidate@example.com", "CANDIDATE");
        UUID otherCandidateId = UUID.randomUUID();
        UUID skillId = UUID.randomUUID();
        org.mockito.Mockito.when(skillsService.getMySkills(candidate))
                .thenReturn(new CandidateSkillsResponse(UUID.randomUUID(), 0, List.of()));

        mockMvc.perform(get("/api/me/skills").with(user(candidate)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/candidates/{candidateId}/skills", otherCandidateId).with(user(candidate)))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/candidates/{candidateId}/skills/{skillId}", otherCandidateId, skillId)
                        .with(user(candidate)).contentType(APPLICATION_JSON).content("{\"level\":40}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/me/skills").with(user(candidate))
                        .contentType(APPLICATION_JSON).content("{\"skills\":[]}"))
                .andExpect(status().isForbidden());

        verify(skillsService).getMySkills(candidate);
        verify(skillsService, never()).getCandidateSkills(eq(otherCandidateId), any());
        verify(skillsService, never()).updateSkill(eq(otherCandidateId), eq(skillId),
                any(UpdateSkillLevelRequest.class), any());
    }

    @Test
    void rejectsLevelAboveOneHundredWithBadRequest() throws Exception {
        AppUserDetails admin = userWithRole("admin@example.com", "ADMIN");
        mockMvc.perform(put("/api/candidates/{candidateId}/skills/{skillId}",
                        UUID.randomUUID(), UUID.randomUUID())
                        .with(user(admin))
                        .contentType(APPLICATION_JSON)
                        .content("{\"level\":101}"))
                .andExpect(status().isBadRequest());
        verify(skillsService, never()).updateSkill(any(), any(), any(), any());
    }

    private static AppUserDetails userWithRole(String email, String role) {
        return new AppUserDetails(
                UUID.randomUUID(),
                email,
                "hash",
                true,
                List.of(new SimpleGrantedAuthority("ROLE_" + role))
        );
    }
}
