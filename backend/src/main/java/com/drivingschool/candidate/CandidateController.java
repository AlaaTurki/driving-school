package com.drivingschool.candidate;

import com.drivingschool.candidate.dto.CandidateResponse;
import com.drivingschool.candidate.dto.UpdateCandidateRolesRequest;
import com.drivingschool.candidate.dto.UpdateCandidateRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/candidates")
@PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
public class CandidateController {

    private final CandidateService candidateService;

    public CandidateController(CandidateService candidateService) {
        this.candidateService = candidateService;
    }

    @GetMapping
    public List<CandidateResponse> findAll() {
        return candidateService.findAll();
    }

    @PutMapping("/{id}")
    public CandidateResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCandidateRequest request
    ) {
        return candidateService.update(id, request);
    }

    @PutMapping("/{id}/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public CandidateResponse updateRoles(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCandidateRolesRequest request
    ) {
        return candidateService.updateRoles(id, request);
    }
}
