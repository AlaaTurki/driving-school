package com.drivingschool.candidate.dto;

import com.drivingschool.user.Role;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record UpdateCandidateRolesRequest(
        @NotEmpty List<@NotNull Role> roles
) {
}
