package com.collegeops.institution;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record StudentRequest(
        @NotBlank @Size(max = 80) String studentNumber,
        @NotNull UUID departmentId,
        @NotNull UUID academicProgramId,
        UUID userId) {
}
