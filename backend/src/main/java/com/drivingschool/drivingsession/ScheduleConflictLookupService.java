package com.drivingschool.drivingsession;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class ScheduleConflictLookupService {

    private final DrivingSessionRepository sessionRepository;

    public ScheduleConflictLookupService(DrivingSessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public Optional<ScheduleConflictDetails> findConflict(
            String resourceProperty,
            UUID resourceId,
            LocalDate date,
            LocalTime start,
            LocalTime end,
            UUID excludedSessionId
    ) {
        return sessionRepository.findAll(
                        DrivingSessionSpecifications.overlappingResourceOnDate(
                                resourceProperty,
                                resourceId,
                                date,
                                excludedSessionId
                        ),
                        Sort.by("startTime").ascending()
                ).stream()
                .filter(session -> ScheduleConflictPolicy.overlaps(
                        date,
                        start,
                        end,
                        DrivingSessionStatus.PLANNED,
                        session.getSessionDate(),
                        session.getStartTime(),
                        session.getEndTime(),
                        session.getStatus()
                ))
                .findFirst()
                .map(ScheduleConflictDetails::from);
    }
}
