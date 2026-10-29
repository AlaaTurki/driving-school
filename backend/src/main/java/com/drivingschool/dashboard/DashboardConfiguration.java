package com.drivingschool.dashboard;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class DashboardConfiguration {

    @Bean("dashboardClock")
    Clock dashboardClock() {
        return Clock.systemUTC();
    }
}
