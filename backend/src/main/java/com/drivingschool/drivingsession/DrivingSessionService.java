package com.drivingschool.drivingsession;

import com.drivingschool.candidate.Candidate;
import com.drivingschool.candidate.CandidateRepository;
import com.drivingschool.candidate.CandidateStatus;
import com.drivingschool.circuit.Circuit;
import com.drivingschool.circuit.CircuitRepository;
import com.drivingschool.instructor.Instructor;
import com.drivingschool.instructor.InstructorRepository;
import com.drivingschool.instructor.InstructorStatus;
import com.drivingschool.security.AppUserDetails;
import com.drivingschool.user.Role;
import com.drivingschool.vehicle.Vehicle;
import com.drivingschool.vehicle.VehicleRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import com.drivingschool.drivingsession.dto.ChangeDrivingSessionStatusRequest;
import com.drivingschool.drivingsession.dto.CreateDrivingSessionRequest;
import com.drivingschool.drivingsession.dto.DrivingSessionResponse;
import com.drivingschool.drivingsession.dto.UpdateDrivingSessionRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class DrivingSessionService {

    private static final DateTimeFormatter CONFLICT_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter CONFLICT_TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private final DrivingSessionRepository sessionRepository;
    private final CandidateRepository candidateRepository;
    private final InstructorRepository instructorRepository;
    private final VehicleRepository vehicleRepository;
    private final CircuitRepository circuitRepository;
    private final ScheduleConflictLookupService conflictLookupService;

    public DrivingSessionService(
            DrivingSessionRepository sessionRepository,
            CandidateRepository candidateRepository,
            InstructorRepository instructorRepository,
            VehicleRepository vehicleRepository,
            CircuitRepository circuitRepository,
            ScheduleConflictLookupService conflictLookupService
    ) {
        this.sessionRepository = sessionRepository;
        this.candidateRepository = candidateRepository;
        this.instructorRepository = instructorRepository;
        this.vehicleRepository = vehicleRepository;
        this.circuitRepository = circuitRepository;
        this.conflictLookupService = conflictLookupService;
    }

    @Transactional(readOnly = true)
    public Page<DrivingSessionResponse> findAll(
            LocalDate dateFrom,
            LocalDate dateTo,
            UUID candidateId,
            UUID instructorId,
            DrivingSessionStatus status,
            UUID circuitId,
            Pageable pageable,
            AppUserDetails actor
    ) {
        validateDateRange(dateFrom, dateTo);
        boolean admin = hasRole(actor, Role.ADMIN);
        return sessionRepository.search(
                        dateFrom,
                        dateTo,
                        candidateId,
                        admin ? instructorId : null,
                        status,
                        circuitId,
                        admin ? null : actor.id(),
                        null,
                        pageable
                )
                .map(DrivingSessionResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<DrivingSessionResponse> findForCandidate(
            LocalDate dateFrom,
            LocalDate dateTo,
            Pageable pageable,
            AppUserDetails candidate
    ) {
        validateDateRange(dateFrom, dateTo);
        return sessionRepository.search(
                        dateFrom,
                        dateTo,
                        null,
                        null,
                        null,
                        null,
                        null,
                        candidate.id(),
                        pageable
                )
                .map(DrivingSessionResponse::from);
    }

    @Transactional(readOnly = true)
    public DrivingSessionResponse findById(UUID id, AppUserDetails actor) {
        DrivingSession session = loadForActor(id, actor);
        return DrivingSessionResponse.from(session);
    }

    @Transactional
    public DrivingSessionResponse create(CreateDrivingSessionRequest request, AppUserDetails actor) {
        Instructor instructor = resolveInstructorForCreate(request.instructorId(), actor);
        Candidate candidate = resolveCandidate(request.candidateId());
        Vehicle vehicle = resolveVehicle(request.vehicleId());
        Circuit circuit = resolveCircuit(request.circuitId());
        BigDecimal price = request.price() != null ? request.price()
                : circuit == null ? null : circuit.getPrice();
        if (price == null) {
            throw new InvalidDrivingSessionException("price is required when no circuit is selected");
        }
        validatePrice(price);
        validateTimeRange(request.startTime(), request.endTime());
        validateNoOverlap(candidate, instructor, vehicle, request.sessionDate(), request.startTime(), request.endTime(), null);
        DrivingSession session = new DrivingSession(
                candidate,
                instructor,
                vehicle,
                circuit,
                request.sessionDate(),
                request.startTime(),
                request.endTime(),
                price,
                trimToNull(request.notes())
        );
        return DrivingSessionResponse.from(saveWithConflictMapping(session));
    }

    @Transactional
    public DrivingSessionResponse update(UUID id, UpdateDrivingSessionRequest request, AppUserDetails actor) {
        DrivingSession session = loadForActor(id, actor);
        Instructor instructor = resolveInstructorForUpdate(request.instructorId(), actor);
        Candidate candidate = resolveCandidate(request.candidateId());
        Vehicle vehicle = resolveVehicle(request.vehicleId());
        Circuit circuit = resolveCircuit(request.circuitId());
        validateTimeRange(request.startTime(), request.endTime());
        if (session.getStatus() != DrivingSessionStatus.CANCELLED) {
            validateNoOverlap(
                    candidate,
                    instructor,
                    vehicle,
                    request.sessionDate(),
                    request.startTime(),
                    request.endTime(),
                    id
            );
        }
        session.setCandidate(candidate);
        session.setInstructor(instructor);
        session.setVehicle(vehicle);
        session.setCircuit(circuit);
        session.update(
                request.sessionDate(),
                request.startTime(),
                request.endTime(),
                session.getPrice(),
                trimToNull(request.notes())
        );
        return DrivingSessionResponse.from(saveWithConflictMapping(session));
    }

    @Transactional
    public DrivingSessionResponse changeStatus(
            UUID id,
            ChangeDrivingSessionStatusRequest request,
            AppUserDetails actor
    ) {
        DrivingSession session = loadForActor(id, actor);
        DrivingSessionStatus current = session.getStatus();
        DrivingSessionStatus target = request.status();
        boolean admin = hasRole(actor, Role.ADMIN);
        if (current != target) {
            if (current == DrivingSessionStatus.PLANNED) {
                if (target == DrivingSessionStatus.PLANNED) {
                    throw new SessionStatusConflictException("Session status is unchanged");
                }
            } else if (target == DrivingSessionStatus.PLANNED && admin && hasText(request.notes())) {
                // An administrator may reopen a terminal session only with an audit reason.
            } else {
                throw new SessionStatusConflictException(
                        "A terminal session cannot transition to the requested status"
                );
            }
        }
        if ((target == DrivingSessionStatus.DONE || target == DrivingSessionStatus.ABSENT)
                && LocalDateTime.of(session.getSessionDate(), session.getStartTime()).isAfter(LocalDateTime.now())) {
            throw new SessionStatusConflictException("A future session cannot be marked DONE or ABSENT");
        }
        if (current == DrivingSessionStatus.CANCELLED && target != DrivingSessionStatus.CANCELLED) {
            validateNoOverlap(
                    session.getCandidate(),
                    session.getInstructor(),
                    session.getVehicle(),
                    session.getSessionDate(),
                    session.getStartTime(),
                    session.getEndTime(),
                    id
            );
        }
        session.changeStatus(target);
        session.update(
                session.getSessionDate(),
                session.getStartTime(),
                session.getEndTime(),
                session.getPrice(),
                trimToNull(request.notes())
        );
        return DrivingSessionResponse.from(saveWithConflictMapping(session));
    }

    @Transactional
    public void delete(UUID id) {
        DrivingSession session = sessionRepository.findById(id)
                .orElseThrow(() -> new DrivingSessionNotFoundException(id));
        sessionRepository.delete(session);
    }

    private DrivingSession loadForActor(UUID id, AppUserDetails actor) {
        if (hasRole(actor, Role.ADMIN)) {
            return sessionRepository.findById(id).orElseThrow(() -> new DrivingSessionNotFoundException(id));
        }
        return sessionRepository.findByIdAndInstructor_User_Id(id, actor.id())
                .orElseThrow(() -> new InstructorSessionAccessDeniedException());
    }

    private Instructor resolveInstructorForCreate(UUID instructorId, AppUserDetails actor) {
        Instructor instructor;
        if (hasRole(actor, Role.ADMIN)) {
            if (instructorId == null) {
                throw new InvalidDrivingSessionException("instructorId is required");
            }
            instructor = instructorRepository.findById(instructorId)
                    .orElseThrow(() -> new InvalidDrivingSessionException("Instructor not found"));
        } else {
            instructor = instructorRepository.findByUser_Id(actor.id())
                    .orElseThrow(() -> new InstructorSessionAccessDeniedException());
            if (instructorId != null && !instructor.getId().equals(instructorId)) {
                throw new InstructorSessionAccessDeniedException();
            }
        }
        ensureInstructorActive(instructor);
        return instructor;
    }

    private Instructor resolveInstructorForUpdate(UUID instructorId, AppUserDetails actor) {
        if (!hasRole(actor, Role.ADMIN)) {
            Instructor ownInstructor = instructorRepository.findByUser_Id(actor.id())
                    .orElseThrow(() -> new InstructorSessionAccessDeniedException());
            if (!ownInstructor.getId().equals(instructorId)) {
                throw new InstructorSessionAccessDeniedException();
            }
            ensureInstructorActive(ownInstructor);
            return ownInstructor;
        }
        Instructor instructor = instructorRepository.findById(instructorId)
                .orElseThrow(() -> new InvalidDrivingSessionException("Instructor not found"));
        ensureInstructorActive(instructor);
        return instructor;
    }

    private Candidate resolveCandidate(UUID candidateId) {
        Candidate candidate = candidateRepository.findById(candidateId)
                .orElseThrow(() -> new InvalidDrivingSessionException("Candidate not found"));
        if (candidate.getStatus() != CandidateStatus.ACTIVE) {
            throw new DrivingSessionConflictException("Only ACTIVE candidates can receive a new session");
        }
        return candidate;
    }

    private Vehicle resolveVehicle(UUID vehicleId) {
        if (vehicleId == null) {
            return null;
        }
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new InvalidDrivingSessionException("Vehicle not found"));
        if (!vehicle.isActive()) {
            throw new DrivingSessionConflictException("Vehicle must be active");
        }
        return vehicle;
    }

    private Circuit resolveCircuit(UUID circuitId) {
        if (circuitId == null) {
            return null;
        }
        Circuit circuit = circuitRepository.findById(circuitId)
                .orElseThrow(() -> new InvalidDrivingSessionException("Circuit not found"));
        if (!circuit.isActive()) {
            throw new DrivingSessionConflictException("Circuit must be active");
        }
        return circuit;
    }

    private static void ensureInstructorActive(Instructor instructor) {
        if (instructor.getStatus() != InstructorStatus.ACTIVE) {
            throw new DrivingSessionConflictException("Instructor must be active");
        }
    }

    private static void validateDateRange(LocalDate dateFrom, LocalDate dateTo) {
        if (dateFrom != null && dateTo != null && dateTo.isBefore(dateFrom)) {
            throw new InvalidDrivingSessionException("dateTo must not be before dateFrom");
        }
    }

    private static void validateTimeRange(java.time.LocalTime start, java.time.LocalTime end) {
        if (end == null || start == null || !end.isAfter(start)) {
            throw new InvalidDrivingSessionException("endTime must be after startTime");
        }
    }

    private static void validatePrice(BigDecimal price) {
        if (price.signum() < 0 || price.precision() - price.scale() > 7
                || price.stripTrailingZeros().scale() > 3) {
            throw new InvalidDrivingSessionException("price must be non-negative with at most three decimals");
        }
    }

    private void validateNoOverlap(
            Candidate candidate,
            Instructor instructor,
            Vehicle vehicle,
            LocalDate date,
            java.time.LocalTime start,
            java.time.LocalTime end,
            UUID excludedSessionId
    ) {
        List<ScheduleConflictException> conflicts = new ArrayList<>();
        findConflict("instructor", instructor.getId(), "moniteur", date, start, end, excludedSessionId)
                .ifPresent(conflicts::add);
        if (vehicle != null) {
            findConflict("vehicle", vehicle.getId(), "véhicule", date, start, end, excludedSessionId)
                    .ifPresent(conflicts::add);
        }
        findConflict("candidate", candidate.getId(), "candidat", date, start, end, excludedSessionId)
                .ifPresent(conflicts::add);
        if (conflicts.size() == 1) {
            throw conflicts.get(0);
        }
        if (conflicts.size() > 1) {
            ScheduleConflictException first = conflicts.get(0);
            throw new ScheduleConflictException(
                    conflicts.stream().map(ScheduleConflictException::getResource)
                            .collect(java.util.stream.Collectors.joining(", ")),
                    conflicts.stream().map(ScheduleConflictException::getMessage)
                            .collect(java.util.stream.Collectors.joining("\n")),
                    first.getConflictingSession()
            );
        }
    }

    private Optional<ScheduleConflictException> findConflict(
            String resourceProperty,
            UUID resourceId,
            String resourceLabel,
            LocalDate date,
            java.time.LocalTime start,
            java.time.LocalTime end,
            UUID excludedSessionId
    ) {
        List<DrivingSession> sameResource = sessionRepository.findAll(
                DrivingSessionSpecifications.overlappingResourceOnDate(
                        resourceProperty, resourceId, date, excludedSessionId
                ),
                Sort.by("startTime").ascending()
        );
        return sameResource.stream()
                .filter(existing -> ScheduleConflictPolicy.overlaps(
                        date,
                        start,
                        end,
                        DrivingSessionStatus.PLANNED,
                        existing.getSessionDate(),
                        existing.getStartTime(),
                        existing.getEndTime(),
                        existing.getStatus()
                ))
                .findFirst()
                .map(existing -> {
                    ScheduleConflictDetails details = conflictDetails(existing);
                    return new ScheduleConflictException(
                            resourceLabel,
                            conflictMessage(resourceLabel, details),
                            details
                    );
                });
    }

    static String conflictMessage(String resource, ScheduleConflictDetails details) {
        String date = details.date().format(CONFLICT_DATE_FORMAT);
        String start = details.startTime().format(CONFLICT_TIME_FORMAT);
        String end = details.endTime().format(CONFLICT_TIME_FORMAT);
        return switch (resource) {
            case "moniteur" -> "Le moniteur %s est déjà occupé le %s de %s à %s (séance avec le candidat %s)."
                    .formatted(details.instructorName(), date, start, end, details.candidateName());
            case "véhicule" -> "Le véhicule %s est déjà réservé le %s de %s à %s "
                    .concat("(séance de %s avec le moniteur %s).")
                    .formatted(details.vehicleRegistration(), date, start, end,
                            details.candidateName(), details.instructorName());
            case "candidat" -> "Le candidat %s a déjà une séance le %s de %s à %s avec le moniteur %s."
                    .formatted(details.candidateName(), date, start, end, details.instructorName());
            default -> throw new IllegalArgumentException("Unknown schedule resource " + resource);
        };
    }

    private DrivingSession saveWithConflictMapping(DrivingSession session) {
        try {
            return sessionRepository.saveAndFlush(session);
        } catch (DataIntegrityViolationException exception) {
            String constraint = constraintName(exception);
            String resource = resourceForConstraint(constraint);
            String resourceProperty = resourcePropertyForConstraint(constraint);
            if (resource == null && !isExclusionViolation(exception)) {
                throw exception;
            }
            if (resource == null) {
                resource = "planning";
            }
            ScheduleConflictDetails conflictingSession = resourceProperty == null
                    ? null
                    : conflictLookupService.findConflict(
                            resourceProperty,
                            resourceId(session, resourceProperty),
                            session.getSessionDate(),
                            session.getStartTime(),
                            session.getEndTime(),
                            session.getId()
                     ).orElse(null);
            throw new ScheduleConflictException(
                    resource,
                    conflictingSession == null
                            ? "Un conflit de planning a été détecté pour cette ressource."
                            : conflictMessage(resource, conflictingSession),
                    conflictingSession
            );
        }
    }

    private static String constraintName(Throwable exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof ConstraintViolationException violation) {
                return violation.getConstraintName();
            }
        }
        return null;
    }

    private static boolean isExclusionViolation(Throwable exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof ConstraintViolationException violation
                    && "23P01".equals(violation.getSQLState())) {
                return true;
            }
            if (cause instanceof java.sql.SQLException sqlException
                    && "23P01".equals(sqlException.getSQLState())) {
                return true;
            }
        }
        return false;
    }

    private static String resourceForConstraint(String constraint) {
        String property = resourcePropertyForConstraint(constraint);
        if (property == null) {
            return null;
        }
        return switch (property) {
            case "instructor" -> "moniteur";
            case "vehicle" -> "véhicule";
            case "candidate" -> "candidat";
            default -> null;
        };
    }

    private static String resourcePropertyForConstraint(String constraint) {
        if (constraint == null) {
            return null;
        }
        if (constraint.endsWith("_instructor_time")) {
            return "instructor";
        }
        if (constraint.endsWith("_vehicle_time")) {
            return "vehicle";
        }
        if (constraint.endsWith("_candidate_time")) {
            return "candidate";
        }
        return null;
    }

    private static UUID resourceId(DrivingSession session, String resourceProperty) {
        return switch (resourceProperty) {
            case "instructor" -> session.getInstructor().getId();
            case "vehicle" -> session.getVehicle().getId();
            case "candidate" -> session.getCandidate().getId();
            default -> throw new IllegalArgumentException("Unknown schedule resource " + resourceProperty);
        };
    }

    private static ScheduleConflictDetails conflictDetails(DrivingSession session) {
        return ScheduleConflictDetails.from(session);
    }

    private static boolean hasRole(AppUserDetails user, Role role) {
        return user.getAuthorities().stream().anyMatch(authority ->
                authority.getAuthority().equals("ROLE_" + role.name()));
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
