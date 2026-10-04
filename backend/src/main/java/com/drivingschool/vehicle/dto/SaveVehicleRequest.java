package com.drivingschool.vehicle.dto;

import com.drivingschool.vehicle.VehicleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SaveVehicleRequest(
        @NotBlank @Size(max = 30) String registrationNumber,
        @NotBlank @Size(max = 120) String brand,
        @NotBlank @Size(max = 120) String model,
        @NotNull VehicleType type,
        @NotNull Boolean active
) {
}
