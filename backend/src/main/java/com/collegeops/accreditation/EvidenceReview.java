package com.collegeops.accreditation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "evidence_reviews")
public class EvidenceReview {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "submission_id", nullable = false)
    private EvidenceSubmission submission;

    @Column(name = "institution_id", nullable = false)
    private UUID institutionId;

    @Column(name = "reviewer_user_id", nullable = false)
    private UUID reviewerUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EvidenceReviewDecision decision;

    @Column(nullable = false, length = 2000)
    private String comment;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected EvidenceReview() {
    }

    public EvidenceReview(EvidenceSubmission submission, UUID reviewerUserId,
                          EvidenceReviewDecision decision, String comment) {
        this.id = UUID.randomUUID();
        this.submission = submission;
        this.institutionId = submission.getInstitutionId();
        this.reviewerUserId = reviewerUserId;
        this.decision = decision;
        this.comment = comment;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public EvidenceSubmission getSubmission() {
        return submission;
    }

    public EvidenceReviewDecision getDecision() {
        return decision;
    }

    public String getComment() {
        return comment;
    }

    public UUID getReviewerUserId() {
        return reviewerUserId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
