package com.drivingschool.skills.dto;

import com.drivingschool.skills.CandidateSkill;
import com.drivingschool.skills.Skill;

import java.time.Instant;
import java.util.UUID;

public record CandidateSkillResponse(
        UUID skillId,
        String name,
        int displayOrder,
        int level,
        UUID updatedBy,
        Instant updatedAt
) {

    public static CandidateSkillResponse from(Skill skill, CandidateSkill candidateSkill) {
        return new CandidateSkillResponse(
                skill.getId(),
                skill.getName(),
                skill.getDisplayOrder(),
                candidateSkill == null ? 0 : candidateSkill.getLevel(),
                candidateSkill == null || candidateSkill.getUpdatedBy() == null
                        ? null : candidateSkill.getUpdatedBy().getId(),
                candidateSkill == null ? null : candidateSkill.getUpdatedAt()
        );
    }
}
