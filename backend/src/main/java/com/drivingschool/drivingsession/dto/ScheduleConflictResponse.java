package com.drivingschool.drivingsession.dto;

import com.drivingschool.drivingsession.ScheduleConflictDetails;

public record ScheduleConflictResponse(
        String message,
        String resource,
        ScheduleConflictDetails conflictingSession
) {
}
