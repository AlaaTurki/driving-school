package com.drivingschool.drivingsession;

import com.drivingschool.instructor.Instructor;
import com.drivingschool.instructor.InstructorRepository;
import com.drivingschool.instructor.InstructorStatus;
import com.drivingschool.security.AppUserDetails;
import com.drivingschool.user.Role;
import com.drivingschool.user.User;
import com.drivingschool.vehicle.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.LocalDate;
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

class ScheduleServiceTest {

    private final DrivingSessionRepository sessionRepository = mock(DrivingSessionRepository.class);
    private final InstructorRepository instructorRepository = mock(InstructorRepository.class);
    private final VehicleRepository vehicleRepository = mock(VehicleRepository.class);
    private final ScheduleService service = new ScheduleService(
            sessionRepository,
            instructorRepository,
            vehicleRepository
    );

    @BeforeEach
    void setUp() {
        when(sessionRepository.findAll(
                org.mockito.ArgumentMatchers.<Specification<DrivingSession>>any(),
                any(Sort.class)
        )).thenReturn(List.of());
    }

    @Test
    void acceptsAnInclusiveRangeOfThirtyOneCalendarDays() {
        assertThat(service.getSchedule(
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 7, 1),
                null,
                admin()
        )).isEmpty();
    }

    @Test
    void rejectsRangesLongerThanThirtyOneCalendarDays() {
        assertThatThrownBy(() -> service.getSchedule(
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 7, 2),
                null,
                admin()
        )).isInstanceOf(InvalidDrivingSessionException.class)
                .hasMessageContaining("31 days");
        verify(sessionRepository, never()).findAll(
                org.mockito.ArgumentMatchers.<Specification<DrivingSession>>any(),
                any(Sort.class)
        );
    }

    @Test
    void instructorIsRestrictedToTheirOwnSchedule() {
        UUID userId = UUID.randomUUID();
        UUID ownInstructorId = UUID.randomUUID();
        UUID otherInstructorId = UUID.randomUUID();
        Instructor ownInstructor = new Instructor(
                new User("instructor@example.com", "hash", Set.of(Role.INSTRUCTOR)),
                "Grace", "Hopper", "123", "instructor@example.com", "LIC-1", InstructorStatus.ACTIVE
        );
        org.springframework.test.util.ReflectionTestUtils.setField(ownInstructor, "id", ownInstructorId);
        when(instructorRepository.findByUser_Id(userId)).thenReturn(Optional.of(ownInstructor));
        AppUserDetails principal = new AppUserDetails(
                userId,
                "instructor@example.com",
                "hash",
                true,
                List.of(new SimpleGrantedAuthority("ROLE_INSTRUCTOR"))
        );

        assertThatThrownBy(() -> service.getSchedule(
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 30),
                otherInstructorId,
                principal
        )).isInstanceOf(InstructorSessionAccessDeniedException.class);
        verify(sessionRepository, never()).findAll(
                org.mockito.ArgumentMatchers.<Specification<DrivingSession>>any(),
                any(Sort.class)
        );
    }

    private static AppUserDetails admin() {
        return new AppUserDetails(
                UUID.randomUUID(),
                "admin@example.com",
                "hash",
                true,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
    }
}
