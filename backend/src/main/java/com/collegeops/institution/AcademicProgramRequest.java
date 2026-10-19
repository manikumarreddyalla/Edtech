package com.collegeops.institution;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record AcademicProgramRequest(
        @NotNull UUID departmentId,
        @NotBlank @Size(max = 200) String name,
        @NotBlank @Size(max = 50) @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9_-]*$") String code) {
}
