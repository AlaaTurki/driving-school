package com.drivingschool.instructor;

import com.drivingschool.auth.EmailAlreadyRegisteredException;
import com.drivingschool.instructor.dto.CreateInstructorRequest;
import com.drivingschool.instructor.dto.InstructorResponse;
import com.drivingschool.instructor.dto.UpdateInstructorRequest;
import com.drivingschool.user.Role;
import com.drivingschool.user.User;
import com.drivingschool.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
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

class InstructorServiceTest {

    private final InstructorRepository instructorRepository = mock(InstructorRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final InstructorService service =
            new InstructorService(instructorRepository, userRepository, passwordEncoder);

    @Test
    void createsInstructorAndLinkedLoginWithEncodedPassword() {
        when(userRepository.existsByEmailIgnoreCase("instructor@example.com")).thenReturn(false);
        when(instructorRepository.existsByEmailIgnoreCase("instructor@example.com")).thenReturn(false);
        when(instructorRepository.existsByLicenseNumberIgnoreCase("LIC-123")).thenReturn(false);
        when(passwordEncoder.encode("long-enough-password")).thenReturn("bcrypt-hash");
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(instructorRepository.saveAndFlush(any(Instructor.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        InstructorResponse response = service.create(createRequest());

        var userCaptor = org.mockito.ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(userCaptor.capture());
        assertThat(userCaptor.getValue().getEmail()).isEqualTo("instructor@example.com");
        assertThat(userCaptor.getValue().getPasswordHash()).isEqualTo("bcrypt-hash");
        assertThat(userCaptor.getValue().getRoles()).containsExactly(Role.INSTRUCTOR);
        assertThat(response.email()).isEqualTo("instructor@example.com");
        assertThat(response.status()).isEqualTo(InstructorStatus.ACTIVE);
        assertThat(response.toString()).doesNotContain("bcrypt-hash", "long-enough-password");
    }

    @Test
    void rejectsEmailAlreadyUsedByAnAccount() {
        when(userRepository.existsByEmailIgnoreCase("instructor@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.create(createRequest()))
                .isInstanceOf(EmailAlreadyRegisteredException.class);

        verify(userRepository, never()).saveAndFlush(any(User.class));
        verify(instructorRepository, never()).saveAndFlush(any(Instructor.class));
    }

    @Test
    void rejectsDuplicateLicenseNumber() {
        when(userRepository.existsByEmailIgnoreCase("instructor@example.com")).thenReturn(false);
        when(instructorRepository.existsByEmailIgnoreCase("instructor@example.com")).thenReturn(false);
        when(instructorRepository.existsByLicenseNumberIgnoreCase("LIC-123")).thenReturn(true);

        assertThatThrownBy(() -> service.create(createRequest()))
                .isInstanceOf(InstructorLicenseAlreadyRegisteredException.class);

        verify(userRepository, never()).saveAndFlush(any(User.class));
    }

    @Test
    void updateChangesInstructorFieldsAndDisablesLinkedAccountWhenInactive() {
        UUID id = UUID.randomUUID();
        User user = new User("instructor@example.com", "unchanged-hash", Set.of(Role.INSTRUCTOR));
        Instructor instructor = new Instructor(
                user, "First", "Last", "+21612345678",
                "instructor@example.com", "LIC-123", InstructorStatus.ACTIVE
        );
        org.springframework.test.util.ReflectionTestUtils.setField(instructor, "id", id);
        org.springframework.test.util.ReflectionTestUtils.setField(instructor, "createdAt", Instant.now());
        org.springframework.test.util.ReflectionTestUtils.setField(instructor, "updatedAt", Instant.now());
        when(instructorRepository.findById(id)).thenReturn(Optional.of(instructor));
        when(userRepository.existsByEmailIgnoreCase("updated@example.com")).thenReturn(false);
        when(instructorRepository.existsByEmailIgnoreCaseAndIdNot("updated@example.com", id)).thenReturn(false);
        when(userRepository.saveAndFlush(user)).thenReturn(user);
        when(instructorRepository.saveAndFlush(instructor)).thenReturn(instructor);

        InstructorResponse response = service.update(id, new UpdateInstructorRequest(
                "Updated", "Instructor", "+21698765432", "updated@example.com",
                "LIC-123", InstructorStatus.INACTIVE
        ));

        assertThat(response.firstName()).isEqualTo("Updated");
        assertThat(response.email()).isEqualTo("updated@example.com");
        assertThat(response.status()).isEqualTo(InstructorStatus.INACTIVE);
        assertThat(user.getEmail()).isEqualTo("updated@example.com");
        assertThat(user.isEnabled()).isFalse();
        assertThat(user.getPasswordHash()).isEqualTo("unchanged-hash");
    }

    @Test
    void trimsSearchAndReturnsPagedResults() {
        var pageable = PageRequest.of(0, 10);
        when(instructorRepository.search("name", InstructorStatus.ACTIVE, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        service.findAll(" name ", InstructorStatus.ACTIVE, pageable);

        verify(instructorRepository).search("name", InstructorStatus.ACTIVE, pageable);
    }

    private CreateInstructorRequest createRequest() {
        return new CreateInstructorRequest(
                " First ",
                " Last ",
                " +21612345678 ",
                " Instructor@Example.com ",
                " LIC-123 ",
                "long-enough-password"
        );
    }
}
