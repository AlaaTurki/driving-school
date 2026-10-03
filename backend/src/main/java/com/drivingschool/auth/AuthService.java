package com.drivingschool.auth;

import com.drivingschool.candidate.CandidateRegistrationService;
import com.drivingschool.auth.dto.LoginRequest;
import com.drivingschool.auth.dto.LoginResponse;
import com.drivingschool.auth.dto.RegistrationRequest;
import com.drivingschool.security.AppUserDetails;
import com.drivingschool.security.JwtService;
import com.drivingschool.user.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final CandidateRegistrationService candidateRegistrationService;
    private final Duration tokenExpiration;

    public AuthService(
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            UserRepository userRepository,
            CandidateRegistrationService candidateRegistrationService,
            @Value("${app.jwt.expiration}") Duration tokenExpiration
    ) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.candidateRegistrationService = candidateRegistrationService;
        this.tokenExpiration = tokenExpiration;
    }

    public LoginResponse login(LoginRequest request) {
        var authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email().trim(),
                        request.password()
                )
        );
        var user = (AppUserDetails) authentication.getPrincipal();
        return createSession(user);
    }

    public LoginResponse register(RegistrationRequest request) {
        AppUserDetails user;
        try {
            user = AppUserDetails.from(candidateRegistrationService.register(request));
        } catch (DataIntegrityViolationException exception) {
            if (userRepository.existsByEmailIgnoreCase(request.email().trim())) {
                throw new EmailAlreadyRegisteredException();
            }
            throw exception;
        }
        return createSession(user);
    }

    private LoginResponse createSession(AppUserDetails user) {
        List<String> roles = user.getAuthorities().stream()
                .map(authority -> authority.getAuthority().substring("ROLE_".length()))
                .toList();
        return new LoginResponse(
                jwtService.generateToken(user),
                "Bearer",
                Instant.now().plus(tokenExpiration),
                user.id(),
                user.getUsername(),
                roles
        );
    }
}
