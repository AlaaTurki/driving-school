package com.drivingschool.skills;

import com.drivingschool.candidate.Candidate;
import com.drivingschool.candidate.CandidateRepository;
import com.drivingschool.candidate.CandidateStatus;
import com.drivingschool.drivingsession.DrivingSessionRepository;
import com.drivingschool.security.AppUserDetails;
import com.drivingschool.skills.dto.CandidateSkillsResponse;
import com.drivingschool.skills.dto.SkillLevelRequest;
import com.drivingschool.skills.dto.UpdateCandidateSkillsRequest;
import com.drivingschool.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SkillsServiceTest {

    private final SkillRepository skillRepository = mock(SkillRepository.class);
    private final CandidateSkillRepository candidateSkillRepository = mock(CandidateSkillRepository.class);
    private final CandidateRepository candidateRepository = mock(CandidateRepository.class);
    private final DrivingSessionRepository drivingSessionRepository = mock(DrivingSessionRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final SkillsService service = new SkillsService(
            skillRepository,
            candidateSkillRepository,
            candidateRepository,
            drivingSessionRepository,
            userRepository
    );

    private final UUID candidateId = UUID.randomUUID();
    private final UUID adminId = UUID.randomUUID();
    private Candidate candidate;
    private List<Skill> skills;

    @BeforeEach
    void setUp() {
        candidate = new Candidate(null, "Ada", "Lovelace", "123", "ada@example.com", null,
                null, LocalDate.now(), CandidateStatus.ACTIVE, null);
        ReflectionTestUtils.setField(candidate, "id", candidateId);
        skills = new ArrayList<>();
        for (int index = 1; index <= 9; index++) {
            Skill skill = new Skill();
            ReflectionTestUtils.setField(skill, "id", UUID.randomUUID());
            ReflectionTestUtils.setField(skill, "name", "Skill " + index);
            ReflectionTestUtils.setField(skill, "displayOrder", index);
            skills.add(skill);
        }
        when(candidateRepository.findById(candidateId)).thenReturn(Optional.of(candidate));
        when(skillRepository.findAllByOrderByDisplayOrderAsc()).thenReturn(skills);
        when(candidateSkillRepository.findAllByCandidate_Id(candidateId)).thenReturn(List.of());
        when(skillRepository.findAllById(any())).thenAnswer(invocation -> {
            Iterable<UUID> ids = invocation.getArgument(0);
            List<Skill> found = new ArrayList<>();
            ids.forEach(id -> skills.stream().filter(skill -> skill.getId().equals(id)).findFirst().ifPresent(found::add));
            return found;
        });
    }

    @Test
    void returnsRoundedMeanAcrossAllNineSkillsAndDefaultsMissingLevelsToZero() {
        when(candidateSkillRepository.findAllByCandidate_Id(candidateId)).thenReturn(List.of(
                new CandidateSkill(candidate, skills.get(0), 50, null),
                new CandidateSkill(candidate, skills.get(1), 51, null)
        ));

        CandidateSkillsResponse result = service.getCandidateSkills(candidateId, admin());

        assertThat(result.skills()).hasSize(9);
        assertThat(result.skills()).extracting("level").containsExactly(50, 51, 0, 0, 0, 0, 0, 0, 0);
        assertThat(result.overallProgress()).isEqualTo(11);
    }

    @Test
    void rejectsLevelAboveOneHundred() {
        assertThatThrownBy(() -> service.updateSkills(candidateId,
                new UpdateCandidateSkillsRequest(List.of(new SkillLevelRequest(skills.getFirst().getId(), 101))),
                admin()))
                .isInstanceOf(InvalidCandidateSkillsException.class);
    }

    @Test
    void returnsNotFoundForUnknownSkill() {
        UUID missingSkillId = UUID.randomUUID();

        assertThatThrownBy(() -> service.updateSkills(candidateId,
                new UpdateCandidateSkillsRequest(List.of(new SkillLevelRequest(missingSkillId, 20))),
                admin()))
                .isInstanceOf(SkillNotFoundException.class)
                .hasMessageContaining(missingSkillId.toString());
    }

    private AppUserDetails admin() {
        return new AppUserDetails(
                adminId,
                "admin@example.com",
                "hash",
                true,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
    }
}
