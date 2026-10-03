package com.drivingschool.candidate;

import com.drivingschool.auth.EmailAlreadyRegisteredException;
import com.drivingschool.candidate.dto.CandidateResponse;
import com.drivingschool.candidate.dto.SaveCandidateRequest;
import com.drivingschool.candidate.dto.UpdateCandidateRolesRequest;
import com.drivingschool.security.AppUserDetails;
import com.drivingschool.user.Role;
import com.drivingschool.user.User;
import com.drivingschool.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class CandidateService {

    private final CandidateRepository candidateRepository;
    private final UserRepository userRepository;

    public CandidateService(CandidateRepository candidateRepository, UserRepository userRepository) {
        this.candidateRepository = candidateRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public Page<CandidateResponse> findAll(String search, CandidateStatus status, Pageable pageable) {
        String normalizedSearch = search == null ? "" : search.trim();
        // TODO(Phase 5): restrict instructors to assigned candidates once lesson/session ownership exists.
        return candidateRepository.search(normalizedSearch, status, pageable).map(CandidateService::toResponse);
    }

    @Transactional(readOnly = true)
    public CandidateResponse findById(UUID id) {
        return candidateRepository.findById(id)
                .map(CandidateService::toResponse)
                .orElseThrow(() -> new CandidateNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public CandidateResponse findForUser(AppUserDetails user) {
        return candidateRepository.findByUser_Id(user.id())
                .map(CandidateService::toResponse)
                .orElseThrow(() -> new CandidateNotFoundException(user.id()));
    }

    @Transactional
    public CandidateResponse create(SaveCandidateRequest request) {
        String email = normalizeEmail(request.email());
        if (candidateRepository.existsByEmailIgnoreCase(email)) {
            throw new EmailAlreadyRegisteredException();
        }
        User user = resolveUser(request.userId());
        Candidate candidate = new Candidate(
                user,
                request.firstName().trim(),
                request.lastName().trim(),
                request.phone().trim(),
                email,
                request.dateOfBirth(),
                trimToNull(request.address()),
                request.registrationDate(),
                request.status(),
                trimToNull(request.notes())
        );
        return toResponse(candidateRepository.save(candidate));
    }

    @Transactional
    public CandidateResponse update(UUID id, SaveCandidateRequest request) {
        Candidate candidate = candidateRepository.findById(id)
                .orElseThrow(() -> new CandidateNotFoundException(id));
        String email = normalizeEmail(request.email());
        if (!candidate.getEmail().equalsIgnoreCase(email) && candidateRepository.existsByEmailIgnoreCase(email)) {
            throw new EmailAlreadyRegisteredException();
        }
        candidate.setUser(resolveUser(request.userId()));
        candidate.update(
                request.firstName().trim(),
                request.lastName().trim(),
                request.phone().trim(),
                email,
                request.dateOfBirth(),
                trimToNull(request.address()),
                request.registrationDate(),
                request.status(),
                trimToNull(request.notes())
        );
        return toResponse(candidate);
    }

    @Transactional
    public void delete(UUID id) {
        Candidate candidate = candidateRepository.findById(id)
                .orElseThrow(() -> new CandidateNotFoundException(id));
        candidateRepository.delete(candidate);
    }

    @Transactional
    public CandidateResponse updateRoles(UUID id, UpdateCandidateRolesRequest request) {
        Candidate candidate = candidateRepository.findById(id)
                .orElseThrow(() -> new CandidateNotFoundException(id));
        if (candidate.getUser() == null) {
            throw new CandidateHasNoAccountException(id);
        }
        candidate.getUser().setRoles(Set.copyOf(request.roles()));
        return toResponse(candidate);
    }

    private User resolveUser(UUID userId) {
        if (userId == null) {
            return null;
        }
        return userRepository.findById(userId).orElseThrow(() -> new CandidateUserNotFoundException(userId));
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static CandidateResponse toResponse(Candidate candidate) {
        User user = candidate.getUser();
        List<String> roles = user == null
                ? List.of()
                : user.getRoles().stream().map(Role::name).sorted().toList();
        return new CandidateResponse(
                candidate.getId(),
                user == null ? null : user.getId(),
                candidate.getFirstName(),
                candidate.getLastName(),
                candidate.getPhone(),
                candidate.getEmail(),
                candidate.getDateOfBirth(),
                candidate.getAddress(),
                candidate.getRegistrationDate(),
                candidate.getStatus(),
                candidate.getNotes(),
                candidate.getCreatedAt(),
                candidate.getUpdatedAt(),
                roles
        );
    }
}
