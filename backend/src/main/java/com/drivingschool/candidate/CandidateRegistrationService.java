package com.drivingschool.candidate;

import com.drivingschool.auth.EmailAlreadyRegisteredException;
import com.drivingschool.auth.dto.RegistrationRequest;
import com.drivingschool.candidate.dto.SaveCandidateRequest;
import com.drivingschool.user.Role;
import com.drivingschool.user.User;
import com.drivingschool.user.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Locale;
import java.util.Set;

@Service
public class CandidateRegistrationService {

    private final UserRepository userRepository;
    private final CandidateRepository candidateRepository;
    private final PasswordEncoder passwordEncoder;

    public CandidateRegistrationService(
            UserRepository userRepository,
            CandidateRepository candidateRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.candidateRepository = candidateRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User register(RegistrationRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmailIgnoreCase(email) || candidateRepository.existsByEmailIgnoreCase(email)) {
            throw new EmailAlreadyRegisteredException();
        }

        var user = userRepository.saveAndFlush(new User(
                email,
                passwordEncoder.encode(request.password()),
                Set.of(Role.CANDIDATE)
        ));
        String[] names = request.fullName().trim().split("\\s+", 2);
        String lastName = names.length == 2 ? names[1] : "";
        candidateRepository.save(new Candidate(
                user,
                names[0],
                lastName,
                request.phone().trim(),
                email,
                null,
                null,
                LocalDate.now(),
                CandidateStatus.ACTIVE,
                null
        ));
        return user;
    }
}
