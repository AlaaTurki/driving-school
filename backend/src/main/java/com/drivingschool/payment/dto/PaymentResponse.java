package com.drivingschool.payment.dto;

import com.drivingschool.payment.Payment;
import com.drivingschool.payment.PaymentMethod;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        UUID candidateId,
        String candidateName,
        BigDecimal amount,
        LocalDate paymentDate,
        PaymentMethod paymentMethod,
        String notes,
        Instant createdAt,
        Instant updatedAt
) {

    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getCandidate().getId(),
                payment.getCandidate().getFirstName() + " " + payment.getCandidate().getLastName(),
                payment.getAmount(),
                payment.getPaymentDate(),
                payment.getPaymentMethod(),
                payment.getNotes(),
                payment.getCreatedAt(),
                payment.getUpdatedAt()
        );
    }
}
