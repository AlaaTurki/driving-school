package com.drivingschool.drivingsession;

import com.drivingschool.drivingsession.dto.AvailableInstructorResponse;
import com.drivingschool.drivingsession.dto.AvailableVehicleResponse;
import com.drivingschool.drivingsession.dto.ScheduleAvailabilityResponse;
import com.drivingschool.drivingsession.dto.ScheduleDayResponse;
import com.drivingschool.drivingsession.dto.ScheduleEntryResponse;
import com.drivingschool.instructor.Instructor;
import com.drivingschool.instructor.InstructorRepository;
import com.drivingschool.instructor.InstructorStatus;
import com.drivingschool.security.AppUserDetails;
import com.drivingschool.user.Role;
import com.drivingschool.vehicle.Vehicle;
import com.drivingschool.vehicle.VehicleRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

@Service
public class ScheduleService {

    private static final Sort SCHEDULE_ORDER = Sort.by("sessionDate").ascending()
            .and(Sort.by("startTime").ascending())
            .and(Sort.by("id").ascending());

    private final DrivingSessionRepository sessionRepository;
    private final InstructorRepository instructorRepository;
    private final VehicleRepository vehicleRepository;

    public ScheduleService(
            DrivingSessionRepository sessionRepository,
            InstructorRepository instructorRepository,
            VehicleRepository vehicleRepository
    ) {
        this.sessionRepository = sessionRepository;
        this.instructorRepository = instructorRepository;
        this.vehicleRepository = vehicleRepository;
    }

    @Transactional(readOnly = true)
    public List<ScheduleDayResponse> getSchedule(
            LocalDate from,
            LocalDate to,
            UUID instructorId,
            AppUserDetails actor
    ) {
        validateRange(from, to);
        UUID scopedInstructorId = instructorId;
        if (!hasRole(actor, Role.ADMIN)) {
            Instructor instructor = instructorRepository.findByUser_Id(actor.id())
                    .orElseThrow(InstructorSessionAccessDeniedException::new);
            if (instructorId != null && !instructorId.equals(instructor.getId())) {
                throw new InstructorSessionAccessDeniedException();
            }
            scopedInstructorId = instructor.getId();
        }

        return grouped(sessionRepository.findAll(
                DrivingSessionSpecifications.forSchedule(
                        from, to, scopedInstructorId, null, false, null, null
                ),
                SCHEDULE_ORDER
        ));
    }

    @Transactional(readOnly = true)
    public List<ScheduleDayResponse> getMyUpcomingSchedule(AppUserDetails candidate) {
        LocalDate today = LocalDate.now();
        return grouped(sessionRepository.findAll(
                DrivingSessionSpecifications.forSchedule(
                        today, null, null, candidate.id(), true, today, LocalTime.now()
                ),
                SCHEDULE_ORDER
        ));
    }

    @Transactional(readOnly = true)
    public ScheduleAvailabilityResponse getAvailability(LocalDate date, LocalTime startTime, LocalTime endTime) {
        if (date == null || startTime == null || endTime == null || !endTime.isAfter(startTime)) {
            throw new InvalidDrivingSessionException("endTime must be after startTime");
        }

        List<DrivingSession> existingSessions = sessionRepository.findAll(
                DrivingSessionSpecifications.activeSessionsOnDate(date),
                Sort.by("startTime").ascending()
        );
        List<AvailableInstructorResponse> instructors = instructorRepository.findAllByStatus(
                        InstructorStatus.ACTIVE,
                        Sort.by("lastName").ascending().and(Sort.by("firstName").ascending())
                ).stream()
                .filter(instructor -> existingSessions.stream().noneMatch(session ->
                        instructor.getId().equals(session.getInstructor().getId())
                                && overlaps(date, startTime, endTime, session)))
                .map(instructor -> new AvailableInstructorResponse(
                        instructor.getId(),
                        instructor.getFirstName() + " " + instructor.getLastName(),
                        instructor.getEmail()
                ))
                .toList();
        List<AvailableVehicleResponse> vehicles = vehicleRepository.findAllByActiveTrue(
                        Sort.by("registrationNumber").ascending()
                ).stream()
                .filter(vehicle -> existingSessions.stream().noneMatch(session ->
                        session.getVehicle() != null
                                && vehicle.getId().equals(session.getVehicle().getId())
                                && overlaps(date, startTime, endTime, session)))
                .map(ScheduleService::availableVehicle)
                .toList();
        return new ScheduleAvailabilityResponse(instructors, vehicles);
    }

    private static List<ScheduleDayResponse> grouped(List<DrivingSession> sessions) {
        Map<LocalDate, List<ScheduleEntryResponse>> byDate = new TreeMap<>();
        for (DrivingSession session : sessions) {
            byDate.computeIfAbsent(session.getSessionDate(), ignored -> new ArrayList<>())
                    .add(ScheduleEntryResponse.from(session));
        }
        return byDate.entrySet().stream()
                .map(entry -> new ScheduleDayResponse(entry.getKey(), List.copyOf(entry.getValue())))
                .toList();
    }

    private static AvailableVehicleResponse availableVehicle(Vehicle vehicle) {
        return new AvailableVehicleResponse(
                vehicle.getId(),
                vehicle.getRegistrationNumber(),
                vehicle.getBrand(),
                vehicle.getModel(),
                vehicle.getType()
        );
    }

    private static boolean overlaps(
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime,
            DrivingSession existing
    ) {
        return ScheduleConflictPolicy.overlaps(
                date,
                startTime,
                endTime,
                DrivingSessionStatus.PLANNED,
                existing.getSessionDate(),
                existing.getStartTime(),
                existing.getEndTime(),
                existing.getStatus()
        );
    }

    private static void validateRange(LocalDate from, LocalDate to) {
        if (from == null || to == null || to.isBefore(from)) {
            throw new InvalidDrivingSessionException("from and to are required and to must not be before from");
        }
        if (ChronoUnit.DAYS.between(from, to) > 30) {
            throw new InvalidDrivingSessionException("Schedule range must not exceed 31 days");
        }
    }

    private static boolean hasRole(AppUserDetails user, Role role) {
        return user.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + role.name()));
    }
}
