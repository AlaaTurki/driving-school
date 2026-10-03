package com.drivingschool.candidate;

import com.drivingschool.candidate.dto.CandidateResponse;
import com.drivingschool.security.AppUserDetails;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
@PreAuthorize("hasRole('CANDIDATE')")
public class MeController {

    private final CandidateService candidateService;

    public MeController(CandidateService candidateService) {
        this.candidateService = candidateService;
    }

    @GetMapping
    public CandidateResponse getMyProfile(@AuthenticationPrincipal AppUserDetails user) {
        return candidateService.findForUser(user);
    }
}
