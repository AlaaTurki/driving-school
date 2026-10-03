package com.drivingschool.candidate;

import com.drivingschool.auth.EmailAlreadyRegisteredException;
import com.drivingschool.auth.dto.RegistrationRequest;
import com.drivingschool.user.Role;
import com.drivingschool.user.User;
import com.drivingschool.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CandidateRegistrationServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final CandidateRepository candidateRepository = mock(CandidateRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final CandidateRegistrationService service = new CandidateRegistrationService(
            userRepository,
            candidateRepository,
            passwordEncoder
    );

    @Test
    void registerCreatesLinkedCandidateAccountAndProfile() {
        when(userRepository.existsByEmailIgnoreCase("candidate@example.com")).thenReturn(false);
        when(candidateRepository.existsByEmailIgnoreCase("candidate@example.com")).thenReturn(false);
        when(passwordEncoder.encode("strong-password")).thenReturn("hashed-password");
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.register(new RegistrationRequest(
                "  Candidate Name ",
                " Candidate@Example.com ",
                " +216 12 345 678 ",
                "strong-password"
        ));

        var userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(userCaptor.capture());
        User user = userCaptor.getValue();
        assertThat(user.getEmail()).isEqualTo("candidate@example.com");
        assertThat(user.getPasswordHash()).isEqualTo("hashed-password");
        assertThat(user.getRoles()).containsExactly(Role.CANDIDATE);

        var candidateCaptor = ArgumentCaptor.forClass(Candidate.class);
        verify(candidateRepository).save(candidateCaptor.capture());
        Candidate candidate = candidateCaptor.getValue();
        assertThat(candidate.getUser()).isSameAs(user);
        assertThat(candidate.getFirstName()).isEqualTo("Candidate");
        assertThat(candidate.getLastName()).isEqualTo("Name");
        assertThat(candidate.getEmail()).isEqualTo("candidate@example.com");
        assertThat(candidate.getPhone()).isEqualTo("+216 12 345 678");
        assertThat(candidate.getStatus()).isEqualTo(CandidateStatus.ACTIVE);
    }

    @Test
    void registerRejectsAnExistingAccountOrUnlinkedCandidate() {
        when(userRepository.existsByEmailIgnoreCase("candidate@example.com")).thenReturn(false);
        when(candidateRepository.existsByEmailIgnoreCase("candidate@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.register(new RegistrationRequest(
                "Candidate Name",
                "Candidate@example.com",
                "+216 12 345 678",
                "strong-password"
        ))).isInstanceOf(EmailAlreadyRegisteredException.class);

        verify(userRepository, never()).saveAndFlush(any(User.class));
    }
}
