package com.drivingschool.instructor.dto;

import com.drivingschool.instructor.InstructorStatus;

import java.time.Instant;
import java.util.UUID;

public record InstructorResponse(
        UUID id,
        UUID userId,
        String firstName,
        String lastName,
        String phone,
        String email,
        String licenseNumber,
        InstructorStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
