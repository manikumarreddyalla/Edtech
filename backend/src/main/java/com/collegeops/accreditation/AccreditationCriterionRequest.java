package com.collegeops.accreditation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record AccreditationCriterionRequest(
        @NotNull UUID frameworkId,
        @NotBlank @Size(max = 80) String code,
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 100) String category,
        @Size(max = 2000) String description) {
}
