package com.drivingschool.auth;

import com.drivingschool.auth.dto.LoginRequest;
import com.drivingschool.auth.dto.RegistrationRequest;
import com.drivingschool.candidate.CandidateRegistrationService;
import com.drivingschool.security.AppUserDetails;
import com.drivingschool.security.JwtService;
import com.drivingschool.user.Role;
import com.drivingschool.user.User;
import com.drivingschool.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    private final AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
    private final JwtService jwtService = mock(JwtService.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final CandidateRegistrationService candidateRegistrationService = mock(CandidateRegistrationService.class);
    private final AuthService authService = new AuthService(
            authenticationManager,
            jwtService,
            userRepository,
            candidateRegistrationService,
            Duration.ofHours(1)
    );

    @Test
    void loginAuthenticatesAndReturnsTokenDto() {
        var user = new AppUserDetails(
                UUID.randomUUID(),
                "admin@example.com",
                "encoded-password",
                true,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
        when(authenticationManager.authenticate(any())).thenReturn(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities())
        );
        when(jwtService.generateToken(user)).thenReturn("signed.jwt.token");

        var response = authService.login(new LoginRequest(" admin@example.com ", "correct-password"));

        assertThat(response.accessToken()).isEqualTo("signed.jwt.token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.email()).isEqualTo("admin@example.com");
        assertThat(response.roles()).containsExactly("ADMIN");
        assertThat(response.expiresAt()).isNotNull();
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtService).generateToken(user);
    }

    @Test
    void registerCreatesCandidateWithEncodedPasswordAndReturnsSession() {
        var request = new RegistrationRequest(
                "Candidate Name",
                " Candidate@Example.com ",
                "+1 555 123 4567",
                "strong-password"
        );
        var candidate = mock(User.class);
        when(candidate.getId()).thenReturn(UUID.randomUUID());
        when(candidate.getEmail()).thenReturn("candidate@example.com");
        when(candidate.getRoles()).thenReturn(Set.of(Role.CANDIDATE));
        when(candidate.isEnabled()).thenReturn(true);
        when(candidateRegistrationService.register(request)).thenReturn(candidate);
        when(jwtService.generateToken(any(AppUserDetails.class))).thenReturn("signed.jwt.token");

        var response = authService.register(request);

        assertThat(response.accessToken()).isEqualTo("signed.jwt.token");
        assertThat(response.email()).isEqualTo("candidate@example.com");
        assertThat(response.roles()).containsExactly("CANDIDATE");
        verify(candidateRegistrationService).register(request);
    }

    @Test
    void registerRejectsAnExistingEmail() {
        var request = new RegistrationRequest(
                "Candidate Name",
                "Candidate@example.com",
                "+1 555 123 4567",
                "strong-password"
        );
        when(candidateRegistrationService.register(request))
                .thenThrow(new DataIntegrityViolationException("duplicate email"));
        when(userRepository.existsByEmailIgnoreCase("Candidate@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(EmailAlreadyRegisteredException.class);
    }
}
