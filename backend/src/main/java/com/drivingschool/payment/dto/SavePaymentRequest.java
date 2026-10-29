package com.drivingschool.payment.dto;

import com.drivingschool.payment.PaymentMethod;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record SavePaymentRequest(
        @NotNull UUID candidateId,
        @NotNull @Positive @Digits(integer = 7, fraction = 3) BigDecimal amount,
        @NotNull LocalDate paymentDate,
        @NotNull PaymentMethod paymentMethod,
        @Size(max = 10000) String notes
) {
}
