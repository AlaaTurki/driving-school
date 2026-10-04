package com.drivingschool.vehicle.dto;

import com.drivingschool.vehicle.VehicleType;

import java.time.Instant;
import java.util.UUID;

public record VehicleResponse(
        UUID id,
        String registrationNumber,
        String brand,
        String model,
        VehicleType type,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}
