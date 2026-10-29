package com.drivingschool.drivingsession;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public interface BillableSessionProjection {

    UUID getSessionId();

    LocalDate getSessionDate();

    LocalTime getStartTime();

    LocalTime getEndTime();

    BigDecimal getPrice();

    DrivingSessionStatus getStatus();
}
