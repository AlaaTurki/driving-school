package com.drivingschool.skills.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record SkillLevelRequest(
        @NotNull UUID skillId,
        @NotNull @Min(0) @Max(100) Integer level
) {
}
