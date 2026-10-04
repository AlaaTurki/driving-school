package com.drivingschool.circuit.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CircuitResponse(
        UUID id,
        String name,
        String description,
        String location,
        BigDecimal price,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}
