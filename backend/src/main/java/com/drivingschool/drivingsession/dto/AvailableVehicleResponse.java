package com.drivingschool.drivingsession.dto;

import com.drivingschool.vehicle.VehicleType;

import java.util.UUID;

public record AvailableVehicleResponse(
        UUID id,
        String registrationNumber,
        String brand,
        String model,
        VehicleType type
) {
}
