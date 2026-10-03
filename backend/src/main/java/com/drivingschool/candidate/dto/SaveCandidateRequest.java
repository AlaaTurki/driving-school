package com.drivingschool.candidate.dto;

import com.drivingschool.candidate.CandidateStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public record SaveCandidateRequest(
        @NotBlank @Size(max = 120) String firstName,
        @NotBlank @Size(max = 120) String lastName,
        @NotBlank @Size(max = 30) String phone,
        @NotBlank @Email @Size(max = 320) String email,
        LocalDate dateOfBirth,
        @Size(max = 500) String address,
        @NotNull LocalDate registrationDate,
        @NotNull CandidateStatus status,
        @Size(max = 10000) String notes,
        UUID userId
) {
}
