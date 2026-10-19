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
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "evidence_submissions", uniqueConstraints = {
        @UniqueConstraint(name = "uq_evidence_submissions_number",
                columnNames = {"evidence_assignment_id", "submission_number"}),
        @UniqueConstraint(name = "uq_evidence_submissions_id_tenant", columnNames = {"id", "institution_id"})
})
public class EvidenceSubmission {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evidence_assignment_id", nullable = false)
    private EvidenceAssignment assignment;

    @Column(name = "institution_id", nullable = false)
    private UUID institutionId;

    @Column(name = "submission_number", nullable = false)
    private int submissionNumber;

    @Column(name = "submitter_user_id", nullable = false)
    private UUID submitterUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EvidenceStatus status;

    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt;

    protected EvidenceSubmission() {
    }

    public EvidenceSubmission(EvidenceAssignment assignment, int submissionNumber, UUID submitterUserId) {
        this.id = UUID.randomUUID();
        this.assignment = assignment;
        this.institutionId = assignment.getInstitution().getId();
        this.submissionNumber = submissionNumber;
        this.submitterUserId = submitterUserId;
        this.status = EvidenceStatus.SUBMITTED;
        this.submittedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public EvidenceAssignment getAssignment() {
        return assignment;
    }

    public UUID getInstitutionId() {
        return institutionId;
    }

    public int getSubmissionNumber() {
        return submissionNumber;
    }

    public UUID getSubmitterUserId() {
        return submitterUserId;
    }

    public EvidenceStatus getStatus() {
        return status;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public void setStatus(EvidenceStatus status) {
        this.status = status;
    }
}
