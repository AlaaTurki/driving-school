package com.drivingschool.user;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
public class BootstrapAdminService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public BootstrapAdminService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void createFirstAdmin(String email, String rawPassword) {
        if (email == null || email.isBlank() || rawPassword == null
                || rawPassword.length() < 12 || rawPassword.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("A valid admin email and a password between 12 and 72 UTF-8 bytes are required");
        }
        if (userRepository.existsByRolesContaining(Role.ADMIN)) {
            throw new IllegalStateException("An ADMIN already exists; bootstrap creation is disabled");
        }
        String normalizedEmail = email.trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new IllegalStateException("The bootstrap email is already assigned to a user");
        }
        userRepository.save(new User(
                normalizedEmail,
                passwordEncoder.encode(rawPassword),
                Set.of(Role.ADMIN)
        ));
    }
}
