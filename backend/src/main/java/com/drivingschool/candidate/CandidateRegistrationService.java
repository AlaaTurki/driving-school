package com.drivingschool.candidate;

import com.drivingschool.auth.EmailAlreadyRegisteredException;
import com.drivingschool.auth.dto.RegistrationRequest;
import com.drivingschool.user.Role;
import com.drivingschool.user.User;
import com.drivingschool.user.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Set;

@Service
public class CandidateRegistrationService {

    private final UserRepository userRepository;
    private final CandidateProfileRepository candidateProfileRepository;
    private final PasswordEncoder passwordEncoder;

    public CandidateRegistrationService(
            UserRepository userRepository,
            CandidateProfileRepository candidateProfileRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.candidateProfileRepository = candidateProfileRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User register(RegistrationRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new EmailAlreadyRegisteredException();
        }

        var user = userRepository.saveAndFlush(new User(
                email,
                passwordEncoder.encode(request.password()),
                Set.of(Role.CANDIDATE)
        ));
        candidateProfileRepository.save(new CandidateProfile(
                user,
                request.fullName().trim(),
                request.phone().trim()
        ));
        return user;
    }
}
