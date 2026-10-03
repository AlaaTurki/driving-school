package com.drivingschool.candidate;

import com.drivingschool.candidate.dto.SaveCandidateRequest;
import com.drivingschool.candidate.dto.CandidateResponse;
import com.drivingschool.candidate.dto.UpdateCandidateRolesRequest;
import com.drivingschool.security.AppUserDetails;
import com.drivingschool.user.Role;
import com.drivingschool.user.User;
import com.drivingschool.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CandidateServiceTest {

    private final CandidateRepository candidateRepository = mock(CandidateRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final CandidateService service = new CandidateService(candidateRepository, userRepository);

    @Test
    void createsCandidateWithoutAnAccountAndNormalizesFields() {
        when(candidateRepository.existsByEmailIgnoreCase("candidate@example.com")).thenReturn(false);
        when(candidateRepository.save(any(Candidate.class))).thenAnswer(invocation -> invocation.getArgument(0));
        var request = request(null, CandidateStatus.ACTIVE);

        CandidateResponse result = service.create(request);

        var captor = org.mockito.ArgumentCaptor.forClass(Candidate.class);
        verify(candidateRepository).save(captor.capture());
        assertThat(captor.getValue().getFirstName()).isEqualTo("First");
        assertThat(captor.getValue().getLastName()).isEqualTo("Last");
        assertThat(captor.getValue().getEmail()).isEqualTo("candidate@example.com");
        assertThat(captor.getValue().getAddress()).isNull();
        assertThat(result.userId()).isNull();
        assertThat(result.roles()).isEmpty();
    }

    @Test
    void updateStatusKeepsLinkedUserEnabledOnlyForAllowedStatuses() {
        UUID userId = UUID.randomUUID();
        User user = new User("candidate@example.com", "hashed", Set.of(Role.CANDIDATE));
        org.springframework.test.util.ReflectionTestUtils.setField(user, "id", userId);
        Candidate candidate = candidate(user, CandidateStatus.ACTIVE);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(candidateRepository.findById(candidate.getId())).thenReturn(Optional.of(candidate));

        CandidateResponse suspended = service.update(candidate.getId(), request(userId, CandidateStatus.SUSPENDED));
        assertThat(user.isEnabled()).isFalse();
        assertThat(suspended.status()).isEqualTo(CandidateStatus.SUSPENDED);

        CandidateResponse completed = service.update(candidate.getId(), request(userId, CandidateStatus.COMPLETED));
        assertThat(user.isEnabled()).isTrue();
        assertThat(completed.status()).isEqualTo(CandidateStatus.COMPLETED);
    }

    @Test
    void filtersCandidatesAndReturnsPagedResults() {
        Candidate candidate = candidate(null, CandidateStatus.ACTIVE);
        var pageable = PageRequest.of(0, 10, Sort.by("lastName"));
        when(candidateRepository.search("first", CandidateStatus.ACTIVE, pageable))
                .thenReturn(new PageImpl<>(List.of(candidate), pageable, 1));

        var result = service.findAll(" first ", CandidateStatus.ACTIVE, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void normalizesMissingAndBlankSearchToEmptyString() {
        var pageable = PageRequest.of(0, 10);
        when(candidateRepository.search("", null, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        service.findAll(null, null, pageable);
        service.findAll("   ", null, pageable);

        verify(candidateRepository, org.mockito.Mockito.times(2)).search("", null, pageable);
    }

    @Test
    void candidateSelfLookupUsesAuthenticatedUserId() {
        UUID userId = UUID.randomUUID();
        Candidate candidate = candidate(null, CandidateStatus.ACTIVE);
        when(candidateRepository.findByUser_Id(userId)).thenReturn(Optional.of(candidate));
        var principal = new AppUserDetails(userId, "candidate@example.com", "hash", true, List.of());

        service.findForUser(principal);

        verify(candidateRepository).findByUser_Id(userId);
    }

    @Test
    void roleUpdateRequiresCandidateToHaveLinkedAccount() {
        Candidate candidate = candidate(null, CandidateStatus.ACTIVE);
        when(candidateRepository.findById(candidate.getId())).thenReturn(Optional.of(candidate));

        assertThatThrownBy(() -> service.updateRoles(
                candidate.getId(),
                new UpdateCandidateRolesRequest(List.of(Role.ADMIN))
        )).isInstanceOf(CandidateHasNoAccountException.class);
    }

    @Test
    void deleteRemovesOnlyCandidateRecord() {
        Candidate candidate = candidate(null, CandidateStatus.ACTIVE);
        when(candidateRepository.findById(candidate.getId())).thenReturn(Optional.of(candidate));

        service.delete(candidate.getId());

        verify(candidateRepository).delete(candidate);
        verify(userRepository, never()).delete(any(User.class));
    }

    private Candidate candidate(User user, CandidateStatus status) {
        Candidate candidate = new Candidate(
                user,
                "First",
                "Last",
                "+21612345678",
                "candidate@example.com",
                LocalDate.of(2000, 1, 2),
                null,
                LocalDate.of(2026, 1, 1),
                status,
                null
        );
        UUID id = UUID.randomUUID();
        org.springframework.test.util.ReflectionTestUtils.setField(candidate, "id", id);
        org.springframework.test.util.ReflectionTestUtils.setField(candidate, "createdAt", Instant.now());
        org.springframework.test.util.ReflectionTestUtils.setField(candidate, "updatedAt", Instant.now());
        return candidate;
    }

    private SaveCandidateRequest request(UUID userId, CandidateStatus status) {
        return new SaveCandidateRequest(
                " First ",
                " Last ",
                " +21612345678 ",
                " Candidate@Example.com ",
                LocalDate.of(2000, 1, 2),
                "  ",
                LocalDate.of(2026, 1, 1),
                status,
                "  ",
                userId
        );
    }
}
