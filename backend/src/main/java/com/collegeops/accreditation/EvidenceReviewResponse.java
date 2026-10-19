package com.collegeops.accreditation;

import java.time.Instant;
import java.util.UUID;

public record EvidenceReviewResponse(UUID id, UUID submissionId, UUID reviewerUserId,
                                    EvidenceReviewDecision decision, String comment, Instant createdAt) {
    static EvidenceReviewResponse from(EvidenceReview review) {
        return new EvidenceReviewResponse(review.getId(), review.getSubmission().getId(),
                review.getReviewerUserId(), review.getDecision(), review.getComment(), review.getCreatedAt());
    }
}
