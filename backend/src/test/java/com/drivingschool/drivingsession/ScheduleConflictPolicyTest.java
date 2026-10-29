package com.drivingschool.drivingsession;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ScheduleConflictPolicyTest {

    private static final LocalDate DATE = LocalDate.of(2026, 6, 10);
    private static final DrivingSessionStatus ACTIVE = DrivingSessionStatus.PLANNED;

    @Test
    void identicalSessionsOverlap() {
        assertThat(overlaps(DATE, "10:00", "12:00", ACTIVE, DATE, "10:00", "12:00", ACTIVE)).isTrue();
    }

    @Test
    void partialOverlapIsDetected() {
        assertThat(overlaps(DATE, "10:00", "12:00", ACTIVE, DATE, "11:00", "13:00", ACTIVE)).isTrue();
    }

    @Test
    void containedSessionOverlaps() {
        assertThat(overlaps(DATE, "10:00", "13:00", ACTIVE, DATE, "11:00", "12:00", ACTIVE)).isTrue();
    }

    @Test
    void touchingSessionsDoNotOverlap() {
        assertThat(overlaps(DATE, "10:00", "12:00", ACTIVE, DATE, "12:00", "14:00", ACTIVE)).isFalse();
    }

    @Test
    void sessionsOnDifferentDatesDoNotOverlap() {
        assertThat(overlaps(DATE, "10:00", "12:00", ACTIVE, DATE.plusDays(1), "11:00", "13:00", ACTIVE))
                .isFalse();
    }

    @Test
    void cancelledSessionsAreIgnored() {
        assertThat(overlaps(
                DATE, "10:00", "12:00", DrivingSessionStatus.CANCELLED,
                DATE, "11:00", "13:00", ACTIVE
        )).isFalse();
    }

    @Test
    void plannedDoneAndAbsentSessionsAllReserveTheirSlots() {
        List.of(
                DrivingSessionStatus.PLANNED,
                DrivingSessionStatus.DONE,
                DrivingSessionStatus.ABSENT
        ).forEach(status -> assertThat(overlaps(
                DATE, "10:00", "12:00", ACTIVE,
                DATE, "11:00", "13:00", status
        )).isTrue());
    }

    private static boolean overlaps(
            LocalDate firstDate,
            String firstStart,
            String firstEnd,
            DrivingSessionStatus firstStatus,
            LocalDate secondDate,
            String secondStart,
            String secondEnd,
            DrivingSessionStatus secondStatus
    ) {
        return ScheduleConflictPolicy.overlaps(
                firstDate, LocalTime.parse(firstStart), LocalTime.parse(firstEnd), firstStatus,
                secondDate, LocalTime.parse(secondStart), LocalTime.parse(secondEnd), secondStatus
        );
    }
}
