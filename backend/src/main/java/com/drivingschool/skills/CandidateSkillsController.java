package com.drivingschool.skills;

import com.drivingschool.security.AppUserDetails;
import com.drivingschool.skills.dto.CandidateSkillsResponse;
import com.drivingschool.skills.dto.UpdateCandidateSkillsRequest;
import com.drivingschool.skills.dto.UpdateSkillLevelRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/candidates/{candidateId}/skills")
public class CandidateSkillsController {

    private final SkillsService skillsService;

    public CandidateSkillsController(SkillsService skillsService) {
        this.skillsService = skillsService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
    public CandidateSkillsResponse getCandidateSkills(
            @PathVariable UUID candidateId,
            @AuthenticationPrincipal AppUserDetails actor
    ) {
        return skillsService.getCandidateSkills(candidateId, actor);
    }

    @PutMapping("/{skillId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
    public CandidateSkillsResponse updateSkill(
            @PathVariable UUID candidateId,
            @PathVariable UUID skillId,
            @Valid @RequestBody UpdateSkillLevelRequest request,
            @AuthenticationPrincipal AppUserDetails actor
    ) {
        return skillsService.updateSkill(candidateId, skillId, request, actor);
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
    public CandidateSkillsResponse updateSkills(
            @PathVariable UUID candidateId,
            @Valid @RequestBody UpdateCandidateSkillsRequest request,
            @AuthenticationPrincipal AppUserDetails actor
    ) {
        return skillsService.updateSkills(candidateId, request, actor);
    }
}
