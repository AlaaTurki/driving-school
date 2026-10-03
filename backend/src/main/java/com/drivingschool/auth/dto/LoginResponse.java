package com.drivingschool.auth.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record LoginResponse(
        String accessToken,
        String tokenType,
        Instant expiresAt,
        UUID userId,
        String email,
        List<String> roles
) {
}
