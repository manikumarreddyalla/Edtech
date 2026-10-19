package com.collegeops.accreditation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record EvidenceSubmissionRequest(
        @NotEmpty @Size(max = 20) List<@Valid DocumentVersionReference> documents) {
}
