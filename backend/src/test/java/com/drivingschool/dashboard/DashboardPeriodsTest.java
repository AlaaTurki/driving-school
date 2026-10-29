package com.drivingschool.dashboard;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class DashboardPeriodsTest {

    @Test
    void calculatesMondayToSundayAndTrailingChartPeriods() {
        DashboardPeriods periods = DashboardPeriods.from(LocalDate.of(2026, 10, 25));

        assertThat(periods.weekStart()).isEqualTo(LocalDate.of(2026, 10, 19));
        assertThat(periods.weekEnd()).isEqualTo(LocalDate.of(2026, 10, 25));
        assertThat(periods.daysStart()).isEqualTo(LocalDate.of(2026, 10, 12));
        assertThat(periods.monthStart()).isEqualTo(LocalDate.of(2026, 5, 1));
        assertThat(periods.nextMonthStart()).isEqualTo(LocalDate.of(2026, 11, 1));
    }

    @Test
    void derivesDashboardDateAndTimeInAfricaTunis() {
        Clock clock = Clock.fixed(Instant.parse("2026-10-28T23:30:00Z"), ZoneOffset.UTC);

        assertThat(DashboardPeriods.current(clock).today()).isEqualTo(LocalDate.of(2026, 10, 29));
        assertThat(DashboardPeriods.currentTime(clock)).isEqualTo(LocalTime.of(0, 30));
    }
}
