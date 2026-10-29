package com.drivingschool.skills;

import com.drivingschool.security.AppUserDetails;
import com.drivingschool.skills.dto.CandidateSkillsResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me/skills")
@PreAuthorize("hasRole('CANDIDATE')")
public class MeSkillsController {

    private final SkillsService skillsService;

    public MeSkillsController(SkillsService skillsService) {
        this.skillsService = skillsService;
    }

    @GetMapping
    public CandidateSkillsResponse getMySkills(@AuthenticationPrincipal AppUserDetails actor) {
        return skillsService.getMySkills(actor);
    }
}
