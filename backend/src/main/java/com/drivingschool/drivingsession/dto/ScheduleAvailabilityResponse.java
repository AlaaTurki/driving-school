package com.drivingschool.drivingsession.dto;

import java.util.List;

public record ScheduleAvailabilityResponse(
        List<AvailableInstructorResponse> instructors,
        List<AvailableVehicleResponse> vehicles
) {
}
