package com.drivingschool.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegistrationRequest(
        @NotBlank @Size(max = 120) String fullName,
        @NotBlank @Email @Size(max = 320) String email,
        @NotBlank @Pattern(regexp = "^[+()0-9. -]{7,30}$") String phone,
        @NotBlank @Size(min = 8, max = 72) String password
) {
}
