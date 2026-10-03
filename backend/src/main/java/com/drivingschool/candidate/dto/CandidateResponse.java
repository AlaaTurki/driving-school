package com.drivingschool.candidate.dto;

import com.drivingschool.candidate.CandidateStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CandidateResponse(
        UUID id,
        String fullName,
        String email,
        List<String> roles,
        String phone,
        CandidateStatus status,
        Instant registeredAt
) {
}
