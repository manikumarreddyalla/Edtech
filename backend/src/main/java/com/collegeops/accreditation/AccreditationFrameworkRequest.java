package com.collegeops.accreditation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AccreditationFrameworkRequest(
        @NotBlank @Size(max = 200) String name,
        @NotBlank @Size(max = 50) String versionLabel) {
}
