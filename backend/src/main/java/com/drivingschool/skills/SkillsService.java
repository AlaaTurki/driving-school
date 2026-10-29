package com.drivingschool.skills;

import com.drivingschool.candidate.Candidate;
import com.drivingschool.candidate.CandidateNotFoundException;
import com.drivingschool.candidate.CandidateRepository;
import com.drivingschool.drivingsession.DrivingSessionRepository;
import com.drivingschool.security.AppUserDetails;
import com.drivingschool.skills.dto.CandidateSkillResponse;
import com.drivingschool.skills.dto.CandidateSkillsResponse;
import com.drivingschool.skills.dto.SkillLevelRequest;
import com.drivingschool.skills.dto.SkillResponse;
import com.drivingschool.skills.dto.UpdateCandidateSkillsRequest;
import com.drivingschool.skills.dto.UpdateSkillLevelRequest;
import com.drivingschool.user.Role;
import com.drivingschool.user.User;
import com.drivingschool.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class SkillsService {

    private final SkillRepository skillRepository;
    private final CandidateSkillRepository candidateSkillRepository;
    private final CandidateRepository candidateRepository;
    private final DrivingSessionRepository drivingSessionRepository;
    private final UserRepository userRepository;

    public SkillsService(
            SkillRepository skillRepository,
            CandidateSkillRepository candidateSkillRepository,
            CandidateRepository candidateRepository,
            DrivingSessionRepository drivingSessionRepository,
            UserRepository userRepository
    ) {
        this.skillRepository = skillRepository;
        this.candidateSkillRepository = candidateSkillRepository;
        this.candidateRepository = candidateRepository;
        this.drivingSessionRepository = drivingSessionRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<SkillResponse> getSkills() {
        return skillRepository.findAllByOrderByDisplayOrderAsc().stream()
                .map(SkillResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public CandidateSkillsResponse getCandidateSkills(UUID candidateId, AppUserDetails actor) {
        Candidate candidate = findAuthorizedCandidate(candidateId, actor);
        return toResponse(candidate, skillRepository.findAllByOrderByDisplayOrderAsc(),
                candidateSkillRepository.findAllByCandidate_Id(candidateId));
    }

    @Transactional(readOnly = true)
    public CandidateSkillsResponse getMySkills(AppUserDetails actor) {
        Candidate candidate = candidateRepository.findByUser_Id(actor.id())
                .orElseThrow(() -> new CandidateNotFoundException(actor.id()));
        return toResponse(candidate, skillRepository.findAllByOrderByDisplayOrderAsc(),
                candidateSkillRepository.findAllByCandidate_Id(candidate.getId()));
    }

    @Transactional
    public CandidateSkillsResponse updateSkill(
            UUID candidateId,
            UUID skillId,
            UpdateSkillLevelRequest request,
            AppUserDetails actor
    ) {
        return updateSkills(candidateId, new UpdateCandidateSkillsRequest(
                List.of(new SkillLevelRequest(skillId, request.level()))
        ), actor);
    }

    @Transactional
    public CandidateSkillsResponse updateSkills(
            UUID candidateId,
            UpdateCandidateSkillsRequest request,
            AppUserDetails actor
    ) {
        Candidate candidate = findAuthorizedCandidate(candidateId, actor);
        List<SkillLevelRequest> updates = request.skills();
        Set<UUID> requestedIds = new HashSet<>();
        for (SkillLevelRequest update : updates) {
            if (update.level() < 0 || update.level() > 100) {
                throw new InvalidCandidateSkillsException("Skill level must be between 0 and 100");
            }
            if (!requestedIds.add(update.skillId())) {
                throw new InvalidCandidateSkillsException("A skill may only be updated once per request");
            }
        }

        Map<UUID, Skill> skillsById = new HashMap<>();
        skillRepository.findAllById(requestedIds).forEach(skill -> skillsById.put(skill.getId(), skill));
        for (UUID skillId : requestedIds) {
            if (!skillsById.containsKey(skillId)) {
                throw new SkillNotFoundException(skillId);
            }
        }

        User updatedBy = userRepository.findById(actor.id())
                .orElseThrow(() -> new CandidateNotFoundException(actor.id()));
        Map<UUID, CandidateSkill> existingBySkillId = candidateSkillRepository
                .findAllByCandidate_Id(candidateId).stream()
                .collect(Collectors.toMap(candidateSkill -> candidateSkill.getSkill().getId(), Function.identity()));

        for (SkillLevelRequest update : updates) {
            CandidateSkill candidateSkill = existingBySkillId.get(update.skillId());
            if (candidateSkill == null) {
                candidateSkill = new CandidateSkill(candidate, skillsById.get(update.skillId()), update.level(), updatedBy);
                existingBySkillId.put(update.skillId(), candidateSkill);
            } else {
                candidateSkill.update(update.level(), updatedBy);
            }
        }
        candidateSkillRepository.saveAll(updates.stream()
                .map(update -> existingBySkillId.get(update.skillId()))
                .toList());
        return toResponse(candidate, skillRepository.findAllByOrderByDisplayOrderAsc(),
                new ArrayList<>(existingBySkillId.values()));
    }

    private Candidate findAuthorizedCandidate(UUID candidateId, AppUserDetails actor) {
        Candidate candidate = candidateRepository.findById(candidateId)
                .orElseThrow(() -> new CandidateNotFoundException(candidateId));
        boolean admin = actor.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + Role.ADMIN.name()));
        if (!admin && !drivingSessionRepository.existsByCandidate_IdAndInstructor_User_Id(candidateId, actor.id())) {
            throw new InstructorSkillsAccessDeniedException();
        }
        return candidate;
    }

    private static CandidateSkillsResponse toResponse(
            Candidate candidate,
            List<Skill> skills,
            List<CandidateSkill> candidateSkills
    ) {
        Map<UUID, CandidateSkill> bySkillId = new HashMap<>();
        for (CandidateSkill candidateSkill : candidateSkills) {
            bySkillId.put(candidateSkill.getSkill().getId(), candidateSkill);
        }
        List<CandidateSkillResponse> responses = skills.stream()
                .map(skill -> CandidateSkillResponse.from(skill, bySkillId.get(skill.getId())))
                .toList();
        int total = responses.stream().mapToInt(CandidateSkillResponse::level).sum();
        int average = responses.isEmpty() ? 0 : (total + responses.size() / 2) / responses.size();
        return new CandidateSkillsResponse(candidate.getId(), average, responses);
    }
}
