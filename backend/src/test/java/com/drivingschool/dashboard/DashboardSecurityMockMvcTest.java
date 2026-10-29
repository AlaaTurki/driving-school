package com.drivingschool.dashboard;

import com.drivingschool.common.api.ApiSecurityExceptionHandler;
import com.drivingschool.dashboard.dto.AdminDashboardResponse;
import com.drivingschool.dashboard.dto.InstructorDashboardResponse;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DashboardController.class)
@Import({
        com.drivingschool.security.SecurityConfig.class,
        ApiSecurityExceptionHandler.class,
        DashboardSecurityMockMvcTest.MethodSecurityConfiguration.class
})
class DashboardSecurityMockMvcTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityConfiguration {
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DashboardService dashboardService;

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
        when(dashboardService.getAdminDashboard()).thenReturn(new AdminDashboardResponse(
                0, 0, 0, 0, 0, 0, BigDecimal.ZERO, BigDecimal.ZERO, List.of(), List.of(), List.of()
        ));
        when(dashboardService.getInstructorDashboard(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new InstructorDashboardResponse(
                        List.of(), List.of(), 0,
                        new InstructorDashboardResponse.HoursTaught(BigDecimal.ZERO, BigDecimal.ZERO)
                ));
    }

    @Test
    void adminDashboardIsAdminOnly() throws Exception {
        mockMvc.perform(get("/api/dashboard").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/dashboard").with(user("instructor").roles("INSTRUCTOR")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/dashboard").with(user("candidate").roles("CANDIDATE")))
                .andExpect(status().isForbidden());

        verify(dashboardService).getAdminDashboard();
    }

    @Test
    void instructorDashboardUsesTheAuthenticatedUserIdAndRejectsOtherRoles() throws Exception {
        UUID userId = UUID.randomUUID();
        AppUserDetails principal = new AppUserDetails(
                userId,
                "instructor@example.com",
                "encoded-password",
                true,
                List.of(new SimpleGrantedAuthority("ROLE_INSTRUCTOR"))
        );

        mockMvc.perform(get("/api/dashboard/instructor").with(user(principal)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/dashboard/instructor").with(user("admin").roles("ADMIN")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/dashboard/instructor").with(user("candidate").roles("CANDIDATE")))
                .andExpect(status().isForbidden());

        verify(dashboardService).getInstructorDashboard(userId);
        verify(dashboardService, never()).getInstructorDashboard(org.mockito.ArgumentMatchers.argThat(id -> !id.equals(userId)));
    }
}
