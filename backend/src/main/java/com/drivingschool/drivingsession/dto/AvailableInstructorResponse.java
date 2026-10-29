package com.drivingschool.drivingsession.dto;

import java.util.UUID;

public record AvailableInstructorResponse(UUID id, String name, String email) {
}
