package com.drivingschool.circuit;

import com.drivingschool.common.api.ApiSecurityExceptionHandler;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CircuitController.class)
@Import({
        com.drivingschool.security.SecurityConfig.class,
        ApiSecurityExceptionHandler.class,
        CircuitSecurityMockMvcTest.MethodSecurityConfiguration.class
})
class CircuitSecurityMockMvcTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityConfiguration {
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CircuitService circuitService;

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
    void instructorCanReadButCannotModifyCircuits() throws Exception {
        var instructor = user("instructor").roles("INSTRUCTOR");
        var id = java.util.UUID.randomUUID();
        when(circuitService.findAll(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        )).thenReturn(org.springframework.data.domain.Page.empty());

        mockMvc.perform(get("/api/circuits").with(instructor))
                .andExpect(status().isOk());

        String request = """
                {
                  "name":"City Circuit",
                  "description":"Practice route",
                  "location":"Downtown",
                  "price":12.500,
                  "active":true
                }
                """;
        mockMvc.perform(post("/api/circuits").with(instructor)
                        .contentType(APPLICATION_JSON).content(request))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/circuits/{id}", id).with(instructor)
                        .contentType(APPLICATION_JSON).content(request))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/circuits/{id}", id).with(instructor))
                .andExpect(status().isForbidden());

        verify(circuitService, never()).create(org.mockito.ArgumentMatchers.any());
        verify(circuitService, never()).update(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
        verify(circuitService, never()).delete(id);
    }

    @Test
    void candidateCannotAccessCircuits() throws Exception {
        mockMvc.perform(get("/api/circuits").with(user("candidate").roles("CANDIDATE")))
                .andExpect(status().isForbidden());
        verify(circuitService, never()).findAll(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
    }
}
