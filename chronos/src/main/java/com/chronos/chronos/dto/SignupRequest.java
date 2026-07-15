package com.chronos.chronos.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignupRequest(
        @NotBlank(message = "Name is required") String name,
        @Email(message = "Enter a valid email") @NotBlank String email,
        @Size(min = 8, message = "Password must be at least 8 characters") String password
) {}
