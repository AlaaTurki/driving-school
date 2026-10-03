package com.drivingschool.candidate.dto;

import com.drivingschool.candidate.CandidateStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateCandidateRequest(
        @NotBlank @Size(max = 120) String fullName,
        @NotBlank @Pattern(regexp = "^[+()0-9. -]{7,30}$") String phone,
        @NotNull CandidateStatus status
) {
}
