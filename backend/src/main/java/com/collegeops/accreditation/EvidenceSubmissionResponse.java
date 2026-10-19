package com.collegeops.accreditation;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record EvidenceSubmissionResponse(UUID id, UUID evidenceId, int submissionNumber,
                                         UUID submitterUserId, EvidenceStatus status, Instant submittedAt,
                                         List<EvidenceAttachmentResponse> documents) {
}
