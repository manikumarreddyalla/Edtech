package com.collegeops.security;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank String institutionCode,
        @Email @NotBlank String email,
        @NotBlank String password) {
}
