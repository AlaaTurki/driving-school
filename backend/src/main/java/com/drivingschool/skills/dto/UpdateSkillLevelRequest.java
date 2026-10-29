package com.drivingschool.skills.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateSkillLevelRequest(@NotNull @Min(0) @Max(100) Integer level) {
}
