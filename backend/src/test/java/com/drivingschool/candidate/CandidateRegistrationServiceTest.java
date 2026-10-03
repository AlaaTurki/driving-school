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
    private final CandidateProfileRepository candidateProfileRepository = mock(CandidateProfileRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final CandidateRegistrationService service = new CandidateRegistrationService(
            userRepository,
            candidateProfileRepository,
            passwordEncoder
    );

    @Test
    void registerCreatesCandidateAccountAndProfile() {
        when(userRepository.existsByEmailIgnoreCase("candidate@example.com")).thenReturn(false);
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
        assertThat(userCaptor.getValue().getEmail()).isEqualTo("candidate@example.com");
        assertThat(userCaptor.getValue().getPasswordHash()).isEqualTo("hashed-password");
        assertThat(userCaptor.getValue().getRoles()).containsExactly(Role.CANDIDATE);

        var profileCaptor = ArgumentCaptor.forClass(CandidateProfile.class);
        verify(candidateProfileRepository).save(profileCaptor.capture());
        assertThat(profileCaptor.getValue().getFullName()).isEqualTo("Candidate Name");
        assertThat(profileCaptor.getValue().getPhone()).isEqualTo("+216 12 345 678");
    }

    @Test
    void registerRejectsAnExistingEmailBeforeCreatingAnAccount() {
        when(userRepository.existsByEmailIgnoreCase("candidate@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.register(new RegistrationRequest(
                "Candidate Name",
                "Candidate@example.com",
                "+216 12 345 678",
                "strong-password"
        ))).isInstanceOf(EmailAlreadyRegisteredException.class);

        verify(userRepository, never()).saveAndFlush(any(User.class));
        verify(candidateProfileRepository, never()).save(any(CandidateProfile.class));
    }
}
