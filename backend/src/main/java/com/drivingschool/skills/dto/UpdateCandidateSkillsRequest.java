package com.drivingschool.skills.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record UpdateCandidateSkillsRequest(@NotEmpty List<@Valid SkillLevelRequest> skills) {
}
