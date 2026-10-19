package com.collegeops.accreditation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record EvidenceReviewRequest(
        @NotNull EvidenceReviewDecision decision,
        @NotBlank @Size(max = 2000) String comment) {
}
