package com.drivingschool.instructor.dto;

import com.drivingschool.instructor.InstructorStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateInstructorRequest(
        @NotBlank @Size(max = 120) String firstName,
        @NotBlank @Size(max = 120) String lastName,
        @NotBlank @Size(max = 30) String phone,
        @NotBlank @Email @Size(max = 320) String email,
        @NotBlank @Size(max = 100) String licenseNumber,
        @NotNull InstructorStatus status
) {
}
