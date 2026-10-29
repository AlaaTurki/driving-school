package com.drivingschool.skills.dto;

import java.util.List;
import java.util.UUID;

public record CandidateSkillsResponse(UUID candidateId, int overallProgress, List<CandidateSkillResponse> skills) {
}
