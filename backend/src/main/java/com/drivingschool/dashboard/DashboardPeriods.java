package com.drivingschool.dashboard;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;

public record DashboardPeriods(
        LocalDate today,
        LocalDate weekStart,
        LocalDate weekEnd,
        LocalDate daysStart,
        LocalDate monthStart,
        LocalDate nextMonthStart
) {

    public static final ZoneId TIME_ZONE = ZoneId.of("Africa/Tunis");
    public static final int DAYS_IN_CHART = 14;
    public static final int MONTHS_IN_CHART = 6;

    public static DashboardPeriods current(Clock clock) {
        return from(LocalDate.now(clock.withZone(TIME_ZONE)));
    }

    public static LocalTime currentTime(Clock clock) {
        return LocalTime.now(clock.withZone(TIME_ZONE));
    }

    public static DashboardPeriods from(LocalDate today) {
        LocalDate monthStart = today.withDayOfMonth(1).minusMonths(MONTHS_IN_CHART - 1);
        LocalDate thisMonthStart = today.withDayOfMonth(1);
        return new DashboardPeriods(
                today,
                today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),
                today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)),
                today.minusDays(DAYS_IN_CHART - 1),
                monthStart,
                thisMonthStart.plusMonths(1)
        );
    }
}
