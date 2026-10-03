package com.drivingschool.candidate;

import com.drivingschool.candidate.dto.CandidateResponse;
import com.drivingschool.candidate.dto.UpdateCandidateRolesRequest;
import com.drivingschool.candidate.dto.UpdateCandidateRequest;
import com.drivingschool.user.Role;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class CandidateService {

    private final CandidateProfileRepository candidateProfileRepository;

    public CandidateService(CandidateProfileRepository candidateProfileRepository) {
        this.candidateProfileRepository = candidateProfileRepository;
    }

    @Transactional(readOnly = true)
    public List<CandidateResponse> findAll() {
        return candidateProfileRepository.findAllByOrderByFullNameAsc().stream()
                .map(CandidateService::toResponse)
                .toList();
    }

    @Transactional
    public CandidateResponse update(UUID id, UpdateCandidateRequest request) {
        var profile = candidateProfileRepository.findById(id)
                .orElseThrow(() -> new CandidateNotFoundException(id));
        profile.update(request.fullName().trim(), request.phone().trim(), request.status());
        return toResponse(profile);
    }

    @Transactional
    public CandidateResponse updateRoles(UUID id, UpdateCandidateRolesRequest request) {
        var profile = candidateProfileRepository.findById(id)
                .orElseThrow(() -> new CandidateNotFoundException(id));
        profile.getUser().setRoles(Set.copyOf(request.roles()));
        return toResponse(profile);
    }

    private static CandidateResponse toResponse(CandidateProfile profile) {
        var user = profile.getUser();
        return new CandidateResponse(
                user.getId(),
                profile.getFullName(),
                user.getEmail(),
                user.getRoles().stream().map(Role::name).sorted().toList(),
                profile.getPhone(),
                profile.getStatus(),
                user.getCreatedAt()
        );
    }
}
