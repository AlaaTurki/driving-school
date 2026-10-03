package com.drivingschool.instructor;

import com.drivingschool.auth.EmailAlreadyRegisteredException;
import com.drivingschool.instructor.dto.CreateInstructorRequest;
import com.drivingschool.instructor.dto.InstructorResponse;
import com.drivingschool.instructor.dto.UpdateInstructorRequest;
import com.drivingschool.security.AppUserDetails;
import com.drivingschool.user.Role;
import com.drivingschool.user.User;
import com.drivingschool.user.UserRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class InstructorService {

    private final InstructorRepository instructorRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public InstructorService(
            InstructorRepository instructorRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.instructorRepository = instructorRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public InstructorResponse create(CreateInstructorRequest request) {
        String email = normalizeEmail(request.email());
        String licenseNumber = request.licenseNumber().trim();
        ensureEmailAvailable(email);
        ensureLicenseAvailable(licenseNumber);
        if (request.initialPassword().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new InvalidInstructorPasswordException();
        }

        User user = saveUser(new User(
                email,
                passwordEncoder.encode(request.initialPassword()),
                Set.of(Role.INSTRUCTOR)
        ));
        Instructor instructor = new Instructor(
                user,
                request.firstName().trim(),
                request.lastName().trim(),
                request.phone().trim(),
                email,
                licenseNumber,
                InstructorStatus.ACTIVE
        );
        return toResponse(saveInstructor(instructor));
    }

    @Transactional(readOnly = true)
    public Page<InstructorResponse> findAll(String search, InstructorStatus status, Pageable pageable) {
        String normalizedSearch = search == null ? "" : search.trim();
        return instructorRepository.search(normalizedSearch, status, pageable).map(InstructorService::toResponse);
    }

    @Transactional(readOnly = true)
    public InstructorResponse findById(UUID id) {
        return instructorRepository.findById(id)
                .map(InstructorService::toResponse)
                .orElseThrow(() -> new InstructorNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public InstructorResponse findForUser(AppUserDetails user) {
        return instructorRepository.findByUser_Id(user.id())
                .map(InstructorService::toResponse)
                .orElseThrow(() -> new InstructorNotFoundException(user.id()));
    }

    @Transactional
    public InstructorResponse update(UUID id, UpdateInstructorRequest request) {
        Instructor instructor = instructorRepository.findById(id)
                .orElseThrow(() -> new InstructorNotFoundException(id));
        String email = normalizeEmail(request.email());
        String licenseNumber = request.licenseNumber().trim();
        if (!instructor.getEmail().equalsIgnoreCase(email)) {
            if (userRepository.existsByEmailIgnoreCase(email)) {
                throw new EmailAlreadyRegisteredException();
            }
            if (instructorRepository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
                throw new EmailAlreadyRegisteredException();
            }
        }
        if (!instructor.getLicenseNumber().equalsIgnoreCase(licenseNumber)
                && instructorRepository.existsByLicenseNumberIgnoreCaseAndIdNot(licenseNumber, id)) {
            throw new InstructorLicenseAlreadyRegisteredException();
        }

        User user = instructor.getUser();
        user.setEmail(email);
        saveUser(user);
        instructor.update(
                request.firstName().trim(),
                request.lastName().trim(),
                request.phone().trim(),
                email,
                licenseNumber,
                request.status()
        );
        return toResponse(saveInstructor(instructor));
    }

    @Transactional
    public void delete(UUID id) {
        Instructor instructor = instructorRepository.findById(id)
                .orElseThrow(() -> new InstructorNotFoundException(id));
        // TODO(Phase 5): refuse deletion when sessions exist and advise setting the instructor to INACTIVE.
        User user = instructor.getUser();
        instructorRepository.delete(instructor);
        instructorRepository.flush();
        userRepository.delete(user);
    }

    private User saveUser(User user) {
        try {
            return userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            if (violates(exception, "uk_users_email")) {
                throw new EmailAlreadyRegisteredException();
            }
            throw exception;
        }
    }

    private Instructor saveInstructor(Instructor instructor) {
        try {
            return instructorRepository.saveAndFlush(instructor);
        } catch (DataIntegrityViolationException exception) {
            if (violates(exception, "uk_instructors_email")) {
                throw new EmailAlreadyRegisteredException();
            }
            if (violates(exception, "uk_instructors_license_number")) {
                throw new InstructorLicenseAlreadyRegisteredException();
            }
            throw exception;
        }
    }

    private void ensureEmailAvailable(String email) {
        if (userRepository.existsByEmailIgnoreCase(email) || instructorRepository.existsByEmailIgnoreCase(email)) {
            throw new EmailAlreadyRegisteredException();
        }
    }

    private void ensureLicenseAvailable(String licenseNumber) {
        if (instructorRepository.existsByLicenseNumberIgnoreCase(licenseNumber)) {
            throw new InstructorLicenseAlreadyRegisteredException();
        }
    }

    private static boolean violates(DataIntegrityViolationException exception, String constraintName) {
        Throwable cause = exception;
        while (cause != null) {
            if (cause instanceof ConstraintViolationException violation
                    && constraintName.equalsIgnoreCase(violation.getConstraintName())) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static InstructorResponse toResponse(Instructor instructor) {
        return new InstructorResponse(
                instructor.getId(),
                instructor.getUser().getId(),
                instructor.getFirstName(),
                instructor.getLastName(),
                instructor.getPhone(),
                instructor.getEmail(),
                instructor.getLicenseNumber(),
                instructor.getStatus(),
                instructor.getCreatedAt(),
                instructor.getUpdatedAt()
        );
    }
}
