package com.drivingschool.circuit.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record SaveCircuitRequest(
        @NotBlank @Size(max = 160) String name,
        @Size(max = 10000) String description,
        @Size(max = 200) String location,
        @NotNull @DecimalMin("0.0") @Digits(integer = 7, fraction = 3) BigDecimal price,
        @NotNull Boolean active
) {
}
