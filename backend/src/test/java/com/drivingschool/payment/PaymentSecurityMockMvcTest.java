package com.drivingschool.payment;

import com.drivingschool.balance.BalanceService;
import com.drivingschool.balance.CandidateFinancialAccessDeniedException;
import com.drivingschool.balance.CandidateFinancialController;
import com.drivingschool.balance.MeFinancialController;
import com.drivingschool.balance.dto.BalanceResponse;
import com.drivingschool.common.api.ApiSecurityExceptionHandler;
import com.drivingschool.exception.GlobalExceptionHandler;
import com.drivingschool.payment.dto.PaymentResponse;
import com.drivingschool.payment.dto.SavePaymentRequest;
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
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
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

@WebMvcTest(controllers = {
        PaymentController.class,
        CandidateFinancialController.class,
        MeFinancialController.class
})
@Import({
        com.drivingschool.security.SecurityConfig.class,
        ApiSecurityExceptionHandler.class,
        GlobalExceptionHandler.class,
        PaymentSecurityMockMvcTest.MethodSecurityConfiguration.class
})
class PaymentSecurityMockMvcTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityConfiguration {
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentService paymentService;

    @MockitoBean
    private BalanceService balanceService;

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
    void candidateCannotReadAnotherCandidatesBalanceOrManagePayments() throws Exception {
        UUID candidateId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        var candidate = user("candidate").roles("CANDIDATE");

        mockMvc.perform(get("/api/candidates/{id}/balance", candidateId).with(candidate))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/payments").with(candidate))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/payments").with(candidate)
                        .contentType(APPLICATION_JSON).content(validPaymentJson(candidateId)))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/payments/{id}", paymentId).with(candidate)
                        .contentType(APPLICATION_JSON).content(validPaymentJson(candidateId)))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/payments/{id}", paymentId).with(candidate))
                .andExpect(status().isForbidden());

        verify(balanceService, never()).getBalance(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(paymentService, never()).create(org.mockito.ArgumentMatchers.any());
        verify(paymentService, never()).update(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(paymentService, never()).delete(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void instructorIsDeniedUnassignedCandidateBalance() throws Exception {
        UUID candidateId = UUID.randomUUID();
        AppUserDetails instructor = new AppUserDetails(
                UUID.randomUUID(),
                "instructor@example.com",
                "hash",
                true,
                List.of(new SimpleGrantedAuthority("ROLE_INSTRUCTOR"))
        );
        doThrow(new CandidateFinancialAccessDeniedException())
                .when(balanceService).getBalance(candidateId, instructor);

        mockMvc.perform(get("/api/candidates/{id}/balance", candidateId).with(user(instructor)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/payments").with(user(instructor)))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanManagePaymentsAndCandidateCanReadOwnBalance() throws Exception {
        UUID candidateId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        PaymentResponse response = new PaymentResponse(
                paymentId,
                candidateId,
                "Ada Lovelace",
                new BigDecimal("100.000"),
                LocalDate.of(2026, 1, 1),
                PaymentMethod.CASH,
                null,
                Instant.now(),
                Instant.now()
        );
        when(paymentService.create(org.mockito.ArgumentMatchers.any(SavePaymentRequest.class)))
                .thenReturn(response);
        when(paymentService.update(org.mockito.ArgumentMatchers.eq(paymentId),
                org.mockito.ArgumentMatchers.any(SavePaymentRequest.class))).thenReturn(response);
        when(balanceService.getMyBalance(org.mockito.ArgumentMatchers.any())).thenReturn(
                new BalanceResponse(candidateId, new BigDecimal("100.000"), new BigDecimal("0.000"),
                        new BigDecimal("100.000"), false)
        );

        mockMvc.perform(post("/api/payments").with(user("admin").roles("ADMIN"))
                        .contentType(APPLICATION_JSON).content(validPaymentJson(candidateId)))
                .andExpect(status().isCreated());
        mockMvc.perform(put("/api/payments/{id}", paymentId).with(user("admin").roles("ADMIN"))
                        .contentType(APPLICATION_JSON).content(validPaymentJson(candidateId)))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/payments/{id}", paymentId).with(user("admin").roles("ADMIN")))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/me/balance").with(user("candidate").roles("CANDIDATE")))
                .andExpect(status().isOk());

        verify(paymentService).create(org.mockito.ArgumentMatchers.any(SavePaymentRequest.class));
        verify(paymentService).update(org.mockito.ArgumentMatchers.eq(paymentId),
                org.mockito.ArgumentMatchers.any(SavePaymentRequest.class));
        verify(paymentService).delete(paymentId);
    }

    @Test
    void nonPositivePaymentAmountIsRejected() throws Exception {
        UUID candidateId = UUID.randomUUID();
        String invalidRequest = """
                {
                  "candidateId":"%s",
                  "amount":0,
                  "paymentDate":"2026-01-01",
                  "paymentMethod":"CASH"
                }
                """.formatted(candidateId);

        mockMvc.perform(post("/api/payments").with(user("admin").roles("ADMIN"))
                        .contentType(APPLICATION_JSON).content(invalidRequest))
                .andExpect(status().isBadRequest());
        verify(paymentService, never()).create(org.mockito.ArgumentMatchers.any());
    }

    private static String validPaymentJson(UUID candidateId) {
        return """
                {
                  "candidateId":"%s",
                  "amount":100.000,
                  "paymentDate":"2026-01-01",
                  "paymentMethod":"CASH"
                }
                """.formatted(candidateId);
    }
}
