package com.collegeops.institution;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record DepartmentRequest(
        @NotBlank @Size(max = 180) String name,
        @NotBlank @Size(max = 40) @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9_-]*$") String code) {
}
