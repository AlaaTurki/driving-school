package com.drivingschool.drivingsession;

import java.time.LocalDate;
import java.time.LocalTime;

public final class ScheduleConflictPolicy {

    private ScheduleConflictPolicy() {
    }

    public static boolean overlaps(
            LocalDate firstDate,
            LocalTime firstStart,
            LocalTime firstEnd,
            DrivingSessionStatus firstStatus,
            LocalDate secondDate,
            LocalTime secondStart,
            LocalTime secondEnd,
            DrivingSessionStatus secondStatus
    ) {
        return firstStatus != DrivingSessionStatus.CANCELLED
                && secondStatus != DrivingSessionStatus.CANCELLED
                && firstDate.equals(secondDate)
                && firstStart.isBefore(secondEnd)
                && secondStart.isBefore(firstEnd);
    }
}
