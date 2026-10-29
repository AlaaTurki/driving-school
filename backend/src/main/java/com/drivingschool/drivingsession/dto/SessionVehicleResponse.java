package com.drivingschool.drivingsession.dto;

import java.util.UUID;

public record SessionVehicleResponse(UUID id, String registrationNumber, String brand, String model) {
}
