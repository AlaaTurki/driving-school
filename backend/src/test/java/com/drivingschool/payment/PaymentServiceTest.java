package com.drivingschool.payment;

import com.drivingschool.candidate.Candidate;
import com.drivingschool.candidate.CandidateNotFoundException;
import com.drivingschool.candidate.CandidateRepository;
import com.drivingschool.candidate.CandidateStatus;
import com.drivingschool.payment.dto.SavePaymentRequest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentServiceTest {

    private final PaymentRepository paymentRepository = mock(PaymentRepository.class);
    private final CandidateRepository candidateRepository = mock(CandidateRepository.class);
    private final PaymentService service = new PaymentService(paymentRepository, candidateRepository);

    @Test
    void createsPaymentForExistingCandidate() {
        UUID candidateId = UUID.randomUUID();
        Candidate candidate = new Candidate(null, "Ada", "Lovelace", "123", "ada@example.com", null,
                null, LocalDate.now(), CandidateStatus.ACTIVE, null);
        org.springframework.test.util.ReflectionTestUtils.setField(candidate, "id", candidateId);
        when(candidateRepository.findById(candidateId)).thenReturn(Optional.of(candidate));
        when(paymentRepository.saveAndFlush(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.create(new SavePaymentRequest(
                candidateId,
                new BigDecimal("56.000"),
                LocalDate.of(2026, 1, 1),
                PaymentMethod.BANK_TRANSFER,
                "  monthly  "
        ));

        assertThat(response.candidateId()).isEqualTo(candidateId);
        assertThat(response.amount()).isEqualByComparingTo("56.000");
        assertThat(response.notes()).isEqualTo("monthly");
    }

    @Test
    void paymentForUnknownCandidateReturnsNotFound() {
        UUID candidateId = UUID.randomUUID();
        when(candidateRepository.findById(candidateId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(new SavePaymentRequest(
                candidateId,
                BigDecimal.ONE,
                LocalDate.now(),
                PaymentMethod.CASH,
                null
        ))).isInstanceOf(CandidateNotFoundException.class);
        verify(paymentRepository, never()).saveAndFlush(any(Payment.class));
    }

    @Test
    void rejectsInvertedDateFilter() {
        assertThatThrownBy(() -> service.findAll(
                null,
                LocalDate.of(2026, 2, 1),
                LocalDate.of(2026, 1, 1),
                null,
                org.springframework.data.domain.PageRequest.of(0, 10)
        )).isInstanceOf(InvalidPaymentException.class);
    }
}
