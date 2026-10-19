package com.collegeops.accreditation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.UUID;

@Entity
@Table(name = "evidence_attachments", uniqueConstraints = {
        @UniqueConstraint(name = "uq_evidence_attachment_version",
                columnNames = {"submission_id", "document_id", "version_number"})
})
public class EvidenceAttachment {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "submission_id", nullable = false)
    private EvidenceSubmission submission;

    @Column(name = "institution_id", nullable = false)
    private UUID institutionId;

    @Column(name = "document_id", nullable = false)
    private UUID documentId;

    @Column(name = "version_number", nullable = false)
    private int versionNumber;

    protected EvidenceAttachment() {
    }

    public EvidenceAttachment(EvidenceSubmission submission, UUID documentId, int versionNumber) {
        this.id = UUID.randomUUID();
        this.submission = submission;
        this.institutionId = submission.getInstitutionId();
        this.documentId = documentId;
        this.versionNumber = versionNumber;
    }

    public UUID getId() {
        return id;
    }

    public EvidenceSubmission getSubmission() {
        return submission;
    }

    public UUID getDocumentId() {
        return documentId;
    }

    public int getVersionNumber() {
        return versionNumber;
    }
}
