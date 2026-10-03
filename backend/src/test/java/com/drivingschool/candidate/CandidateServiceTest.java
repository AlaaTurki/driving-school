package com.drivingschool.candidate;

import com.drivingschool.candidate.dto.UpdateCandidateRequest;
import com.drivingschool.candidate.dto.UpdateCandidateRolesRequest;
import com.drivingschool.user.Role;
import com.drivingschool.user.User;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

class CandidateServiceTest {

    private final CandidateProfileRepository repository = mock(CandidateProfileRepository.class);
    private final CandidateService service = new CandidateService(repository);

    @Test
    void updateChangesOnlyCandidateProfileFields() {
        var id = UUID.randomUUID();
        var user = spy(new User("candidate@example.com", "encoded-password", Set.of(Role.CANDIDATE)));
        when(user.getId()).thenReturn(id);
        when(user.getCreatedAt()).thenReturn(Instant.parse("2026-01-01T12:00:00Z"));
        var profile = new CandidateProfile(user, "Old Name", "+21612345678");
        when(repository.findById(id)).thenReturn(Optional.of(profile));

        var result = service.update(id, new UpdateCandidateRequest("  New Name  ", " +21698765432 ", CandidateStatus.INACTIVE));

        assertThat(result.id()).isEqualTo(id);
        assertThat(result.fullName()).isEqualTo("New Name");
        assertThat(result.email()).isEqualTo("candidate@example.com");
        assertThat(result.roles()).containsExactly("CANDIDATE");
        assertThat(result.phone()).isEqualTo("+21698765432");
        assertThat(result.status()).isEqualTo(CandidateStatus.INACTIVE);
        assertThat(result.registeredAt()).isEqualTo(Instant.parse("2026-01-01T12:00:00Z"));
    }

    @Test
    void updateReportsWhenCandidateDoesNotExist() {
        var id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.update(id, new UpdateCandidateRequest("Candidate", "+21612345678", CandidateStatus.ACTIVE)))
                .isInstanceOf(CandidateNotFoundException.class);
    }

    @Test
    void updateRolesChangesCandidateUserRoles() {
        var id = UUID.randomUUID();
        var user = spy(new User("candidate@example.com", "encoded-password", Set.of(Role.CANDIDATE)));
        when(user.getId()).thenReturn(id);
        when(user.getCreatedAt()).thenReturn(Instant.parse("2026-01-01T12:00:00Z"));
        var profile = new CandidateProfile(user, "Candidate Name", "+21612345678");
        when(repository.findById(id)).thenReturn(Optional.of(profile));

        var result = service.updateRoles(id, new UpdateCandidateRolesRequest(
                java.util.List.of(Role.INSTRUCTOR, Role.CANDIDATE)
        ));

        assertThat(result.roles()).containsExactly("CANDIDATE", "INSTRUCTOR");
        assertThat(user.getRoles()).containsExactlyInAnyOrder(Role.CANDIDATE, Role.INSTRUCTOR);
    }
}
