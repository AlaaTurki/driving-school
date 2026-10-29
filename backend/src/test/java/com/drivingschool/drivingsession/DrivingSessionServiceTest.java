package com.drivingschool.drivingsession;

import com.drivingschool.candidate.Candidate;
import com.drivingschool.candidate.CandidateRepository;
import com.drivingschool.candidate.CandidateStatus;
import com.drivingschool.circuit.Circuit;
import com.drivingschool.circuit.CircuitRepository;
import com.drivingschool.drivingsession.dto.ChangeDrivingSessionStatusRequest;
import com.drivingschool.drivingsession.dto.CreateDrivingSessionRequest;
import com.drivingschool.instructor.Instructor;
import com.drivingschool.instructor.InstructorRepository;
import com.drivingschool.instructor.InstructorStatus;
import com.drivingschool.security.AppUserDetails;
import com.drivingschool.user.Role;
import com.drivingschool.user.User;
import com.drivingschool.vehicle.Vehicle;
import com.drivingschool.vehicle.VehicleRepository;
import com.drivingschool.vehicle.VehicleType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DrivingSessionServiceTest {

    private final DrivingSessionRepository sessionRepository = mock(DrivingSessionRepository.class);
    private final CandidateRepository candidateRepository = mock(CandidateRepository.class);
    private final InstructorRepository instructorRepository = mock(InstructorRepository.class);
    private final VehicleRepository vehicleRepository = mock(VehicleRepository.class);
    private final CircuitRepository circuitRepository = mock(CircuitRepository.class);
    private final ScheduleConflictLookupService conflictLookupService = mock(ScheduleConflictLookupService.class);
    private final DrivingSessionService service = new DrivingSessionService(
            sessionRepository,
            candidateRepository,
            instructorRepository,
            vehicleRepository,
            circuitRepository,
            conflictLookupService
    );
    private final UUID instructorId = UUID.randomUUID();
    private final UUID candidateId = UUID.randomUUID();
    private Candidate candidate;
    private Instructor instructor;

    @BeforeEach
    void setUp() {
        candidate = new Candidate(null, "Ada", "Lovelace", "123", "ada@example.com", null,
                null, LocalDate.now(), CandidateStatus.ACTIVE, null);
        org.springframework.test.util.ReflectionTestUtils.setField(candidate, "id", candidateId);
        instructor = new Instructor(
                new User("instructor@example.com", "hash", Set.of(Role.INSTRUCTOR)),
                "Grace",
                "Hopper",
                "123",
                "instructor@example.com",
                "LIC-1",
                InstructorStatus.ACTIVE
        );
        org.springframework.test.util.ReflectionTestUtils.setField(instructor, "id", instructorId);
        when(candidateRepository.findById(candidateId)).thenReturn(Optional.of(candidate));
        when(instructorRepository.findById(instructorId)).thenReturn(Optional.of(instructor));
        when(sessionRepository.findAll(
                org.mockito.ArgumentMatchers.<Specification<DrivingSession>>any(),
                any(Sort.class)
        )).thenReturn(List.of());
        when(sessionRepository.saveAndFlush(any(DrivingSession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void circuitPriceIsCopiedIntoSessionAndDurationIsCalculatedInResponse() {
        Circuit circuit = new Circuit("City loop", null, null, new BigDecimal("45.500"), true);
        UUID circuitId = UUID.randomUUID();
        org.springframework.test.util.ReflectionTestUtils.setField(circuit, "id", circuitId);
        when(circuitRepository.findById(circuitId)).thenReturn(Optional.of(circuit));

        var response = service.create(new CreateDrivingSessionRequest(
                candidateId,
                instructorId,
                null,
                circuitId,
                LocalDate.now().minusDays(1),
                LocalTime.of(9, 0),
                LocalTime.of(10, 30),
                null,
                null
        ), admin());

        assertThat(response.price()).isEqualByComparingTo("45.500");
        assertThat(response.durationHours()).isEqualByComparingTo("1.5000");
        assertThat(response.circuit().name()).isEqualTo("City loop");
    }

    @Test
    void rejectsChangingAFutureSessionToDone() {
        DrivingSession session = new DrivingSession(
                candidate,
                instructor,
                null,
                null,
                LocalDate.now().plusDays(1),
                LocalTime.of(9, 0),
                LocalTime.of(10, 0),
                BigDecimal.TEN,
                null
        );
        UUID sessionId = UUID.randomUUID();
        org.springframework.test.util.ReflectionTestUtils.setField(session, "id", sessionId);
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> service.changeStatus(
                sessionId,
                new ChangeDrivingSessionStatusRequest(DrivingSessionStatus.DONE, null),
                admin()
        )).isInstanceOf(SessionStatusConflictException.class)
                .hasMessageContaining("future session");
    }

    @Test
    void creationReturnsConflictDetailsWhenInstructorIsAlreadyBooked() {
        DrivingSession conflicting = new DrivingSession(
                candidate,
                instructor,
                null,
                null,
                LocalDate.now().plusDays(1),
                LocalTime.of(11, 0),
                LocalTime.of(13, 0),
                BigDecimal.TEN,
                null
        );
        UUID conflictingId = UUID.randomUUID();
        org.springframework.test.util.ReflectionTestUtils.setField(conflicting, "id", conflictingId);
        when(sessionRepository.findAll(
                org.mockito.ArgumentMatchers.<Specification<DrivingSession>>any(),
                any(Sort.class)
        )).thenReturn(List.of(conflicting), List.of());

        assertThatThrownBy(() -> service.create(new CreateDrivingSessionRequest(
                candidateId,
                instructorId,
                null,
                null,
                LocalDate.now().plusDays(1),
                LocalTime.of(10, 0),
                LocalTime.of(12, 0),
                BigDecimal.TEN,
                null
        ), admin()))
                .isInstanceOfSatisfying(ScheduleConflictException.class, exception -> {
                    assertThat(exception.getResource()).isEqualTo("moniteur");
                    assertThat(exception.getMessage()).contains(
                                    LocalDate.now().plusDays(1).format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                                    "11:00", "13:00", "Ada Lovelace")
                            .doesNotContain(conflictingId.toString());
                    assertThat(exception.getConflictingSession().sessionId()).isEqualTo(conflictingId);
                    assertThat(exception.getConflictingSession().candidateName()).isEqualTo("Ada Lovelace");
                });
    }

    @Test
    void creationReturnsAllConflictsForInstructorAndVehicle() {
        UUID vehicleId = UUID.randomUUID();
        UUID otherCandidateId = UUID.randomUUID();
        Candidate otherCandidate = new Candidate(null, "Katherine", "Johnson", "456", "katherine@example.com",
                null, null, LocalDate.now(), CandidateStatus.ACTIVE, null);
        org.springframework.test.util.ReflectionTestUtils.setField(otherCandidate, "id", otherCandidateId);
        Vehicle vehicle = new Vehicle("ABC-123", "Toyota", "Yaris", VehicleType.MANUAL, true);
        org.springframework.test.util.ReflectionTestUtils.setField(vehicle, "id", vehicleId);
        LocalDate conflictDate = LocalDate.now().plusDays(1);
        DrivingSession conflicting = new DrivingSession(
                otherCandidate,
                instructor,
                vehicle,
                null,
                conflictDate,
                LocalTime.of(11, 0),
                LocalTime.of(13, 0),
                BigDecimal.TEN,
                null
        );
        UUID conflictingId = UUID.randomUUID();
        org.springframework.test.util.ReflectionTestUtils.setField(conflicting, "id", conflictingId);
        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicle));
        when(sessionRepository.findAll(
                org.mockito.ArgumentMatchers.<Specification<DrivingSession>>any(),
                any(Sort.class)
        )).thenReturn(List.of(conflicting), List.of(conflicting), List.of());

        assertThatThrownBy(() -> service.create(new CreateDrivingSessionRequest(
                candidateId,
                instructorId,
                vehicleId,
                null,
                conflictDate,
                LocalTime.of(10, 0),
                LocalTime.of(12, 0),
                BigDecimal.TEN,
                null
        ), admin()))
                .isInstanceOfSatisfying(ScheduleConflictException.class, exception -> {
                    assertThat(exception.getResource()).isEqualTo("moniteur, véhicule");
                    assertThat(exception.getMessage().lines()).hasSize(2);
                    assertThat(exception.getMessage()).contains("Le moniteur Grace Hopper", "Le véhicule ABC-123");
                    assertThat(exception.getConflictingSession().sessionId()).isEqualTo(conflictingId);
                });
    }

    @Test
    void creationAcceptsSessionsThatTouchAtTheBoundary() {
        DrivingSession existing = new DrivingSession(
                candidate,
                instructor,
                null,
                null,
                LocalDate.now().plusDays(1),
                LocalTime.of(10, 0),
                LocalTime.of(12, 0),
                BigDecimal.TEN,
                null
        );
        when(sessionRepository.findAll(
                org.mockito.ArgumentMatchers.<Specification<DrivingSession>>any(),
                any(Sort.class)
        )).thenReturn(List.of(existing));

        var response = service.create(new CreateDrivingSessionRequest(
                candidateId,
                instructorId,
                null,
                null,
                LocalDate.now().plusDays(1),
                LocalTime.of(12, 0),
                LocalTime.of(14, 0),
                BigDecimal.TEN,
                null
        ), admin());

        assertThat(response.startTime()).isEqualTo(LocalTime.of(12, 0));
        assertThat(response.endTime()).isEqualTo(LocalTime.of(14, 0));
    }

    @Test
    void databaseExclusionViolationBecomesAResourceSpecificConflict() {
        LocalDate conflictDate = LocalDate.now().plusDays(1);
        org.hibernate.exception.ConstraintViolationException databaseCause =
                new org.hibernate.exception.ConstraintViolationException(
                        "overlap",
                        new SQLException("overlap", "23P01"),
                        "excl_driving_sessions_instructor_time"
                );
        when(sessionRepository.saveAndFlush(any(DrivingSession.class)))
                .thenThrow(new org.springframework.dao.DataIntegrityViolationException(
                        "overlap", databaseCause
                ));
        ScheduleConflictDetails details = new ScheduleConflictDetails(
                UUID.randomUUID(),
                conflictDate,
                LocalTime.of(11, 0),
                LocalTime.of(13, 0),
                "Ada Lovelace",
                "Grace Hopper",
                null
        );
        when(conflictLookupService.findConflict(
                "instructor",
                instructorId,
                conflictDate,
                LocalTime.of(10, 0),
                LocalTime.of(12, 0),
                null
        )).thenReturn(Optional.of(details));

        assertThatThrownBy(() -> service.create(new CreateDrivingSessionRequest(
                candidateId,
                instructorId,
                null,
                null,
                conflictDate,
                LocalTime.of(10, 0),
                LocalTime.of(12, 0),
                BigDecimal.TEN,
                null
        ), admin()))
        .isInstanceOfSatisfying(ScheduleConflictException.class, exception -> {
            assertThat(exception.getResource()).isEqualTo("moniteur");
            assertThat(exception.getMessage()).contains("Le moniteur Grace Hopper", "11:00", "13:00");
            assertThat(exception.getConflictingSession()).isEqualTo(details);
        });
    }

    @Test
    void terminalStatusCannotReturnToPlannedWithoutAdminReason() {
        DrivingSession session = new DrivingSession(
                candidate,
                instructor,
                null,
                null,
                LocalDate.now().minusDays(1),
                LocalTime.of(9, 0),
                LocalTime.of(10, 0),
                BigDecimal.TEN,
                null
        );
        session.changeStatus(DrivingSessionStatus.CANCELLED);
        UUID sessionId = UUID.randomUUID();
        org.springframework.test.util.ReflectionTestUtils.setField(session, "id", sessionId);
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> service.changeStatus(
                sessionId,
                new ChangeDrivingSessionStatusRequest(DrivingSessionStatus.PLANNED, null),
                admin()
        )).isInstanceOf(SessionStatusConflictException.class);
    }

    @Test
    void instructorSessionListIsScopedToAuthenticatedInstructor() {
        UUID userId = UUID.randomUUID();
        var principal = new AppUserDetails(userId, "instructor@example.com", "hash", true,
                List.of(new SimpleGrantedAuthority("ROLE_INSTRUCTOR")));
        when(sessionRepository.search(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(Page.empty());

        service.findAll(null, null, null, null, null, null,
                org.springframework.data.domain.PageRequest.of(0, 20), principal);

        verify(sessionRepository).search(null, null, null, null, null, null, userId, null,
                org.springframework.data.domain.PageRequest.of(0, 20));
    }

    @Test
    void instructorCannotReadAnotherInstructorsSession() {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        var principal = new AppUserDetails(userId, "instructor@example.com", "hash", true,
                List.of(new SimpleGrantedAuthority("ROLE_INSTRUCTOR")));
        when(sessionRepository.findByIdAndInstructor_User_Id(sessionId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(sessionId, principal))
                .isInstanceOf(InstructorSessionAccessDeniedException.class);
        verify(sessionRepository).findByIdAndInstructor_User_Id(sessionId, userId);
    }

    private static AppUserDetails admin() {
        return new AppUserDetails(UUID.randomUUID(), "admin@example.com", "hash", true,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }
}
