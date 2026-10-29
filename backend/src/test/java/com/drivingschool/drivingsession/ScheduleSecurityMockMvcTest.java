package com.drivingschool.drivingsession;

import com.drivingschool.common.api.ApiSecurityExceptionHandler;
import com.drivingschool.exception.GlobalExceptionHandler;
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

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ScheduleController.class)
@Import({
        com.drivingschool.security.SecurityConfig.class,
        ApiSecurityExceptionHandler.class,
        GlobalExceptionHandler.class,
        ScheduleSecurityMockMvcTest.MethodSecurityConfiguration.class
})
class ScheduleSecurityMockMvcTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityConfiguration {
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ScheduleService scheduleService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void passThroughJwtFilter() throws Exception {
        doAnswer(invocation -> {
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(jwtAuthenticationFilter).doFilter(
                any(ServletRequest.class),
                any(ServletResponse.class),
                any(FilterChain.class)
        );
    }

    @Test
    void instructorCannotReadAnotherInstructorsSchedule() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID otherInstructorId = UUID.randomUUID();
        AppUserDetails instructor = new AppUserDetails(
                userId,
                "instructor@example.com",
                "hash",
                true,
                List.of(new SimpleGrantedAuthority("ROLE_INSTRUCTOR"))
        );
        doThrow(new InstructorSessionAccessDeniedException())
                .when(scheduleService).getSchedule(
                        any(LocalDate.class),
                        any(LocalDate.class),
                        eq(otherInstructorId),
                        eq(instructor)
                );

        mockMvc.perform(get("/api/schedule")
                        .param("from", "2026-06-01")
                        .param("to", "2026-06-30")
                        .param("instructorId", otherInstructorId.toString())
                        .with(user(instructor)))
                .andExpect(status().isForbidden());
    }
}
