package com.drivingschool.candidate;

import com.drivingschool.candidate.dto.CandidatePageResponse;
import com.drivingschool.candidate.dto.CandidateResponse;
import com.drivingschool.candidate.dto.SaveCandidateRequest;
import com.drivingschool.candidate.dto.UpdateCandidateRolesRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
    public CandidatePageResponse findAll(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) CandidateStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        if (page < 0 || size < 1 || size > 100) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Page must be non-negative and size must be between 1 and 100"
            );
        }
        return CandidatePageResponse.from(candidateService.findAll(
                search,
                status,
                PageRequest.of(page, size, Sort.by("lastName").ascending().and(Sort.by("firstName").ascending()))
        ));
    }

    @GetMapping("/{id}")
    public CandidateResponse findById(@PathVariable UUID id) {
        return candidateService.findById(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CandidateResponse> create(@Valid @RequestBody SaveCandidateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(candidateService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public CandidateResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody SaveCandidateRequest request
    ) {
        return candidateService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        candidateService.delete(id);
        return ResponseEntity.noContent().build();
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
