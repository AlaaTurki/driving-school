package com.drivingschool.skills.dto;

import com.drivingschool.skills.Skill;

import java.util.UUID;

public record SkillResponse(UUID id, String name, int displayOrder) {

    public static SkillResponse from(Skill skill) {
        return new SkillResponse(skill.getId(), skill.getName(), skill.getDisplayOrder());
    }
}
