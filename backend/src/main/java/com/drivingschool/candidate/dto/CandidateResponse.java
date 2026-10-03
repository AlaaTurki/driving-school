package com.drivingschool.candidate.dto;

import com.drivingschool.candidate.CandidateStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CandidateResponse(
        UUID id,
        UUID userId,
        String firstName,
        String lastName,
        String phone,
        String email,
        LocalDate dateOfBirth,
        String address,
        LocalDate registrationDate,
        CandidateStatus status,
        String notes,
        Instant createdAt,
        Instant updatedAt,
        List<String> roles
) {
}
