package com.collegeops.documents;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "document_versions", uniqueConstraints = {
        @UniqueConstraint(name = "uq_document_versions_number", columnNames = {"document_id", "version_number"})
})
public class DocumentVersion {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_id", nullable = false)
    private DocumentRecord document;

    @Column(name = "institution_id", nullable = false)
    private UUID institutionId;

    @Column(name = "version_number", nullable = false)
    private int versionNumber;

    @Column(name = "uploader_user_id", nullable = false)
    private UUID uploaderUserId;

    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(nullable = false, length = 64)
    private String sha256;

    @Column(name = "storage_key", nullable = false, unique = true, length = 255)
    private String storageKey;

    @Column(name = "uploaded_at", nullable = false)
    private Instant uploadedAt;

    protected DocumentVersion() {
    }

    public DocumentVersion(DocumentRecord document, int versionNumber, UUID uploaderUserId,
                           String originalFilename, String contentType, long sizeBytes,
                           String sha256, String storageKey) {
        this.id = UUID.randomUUID();
        this.document = document;
        this.institutionId = document.getInstitution().getId();
        this.versionNumber = versionNumber;
        this.uploaderUserId = uploaderUserId;
        this.originalFilename = originalFilename;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.sha256 = sha256;
        this.storageKey = storageKey;
        this.uploadedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public DocumentRecord getDocument() {
        return document;
    }

    public int getVersionNumber() {
        return versionNumber;
    }

    public UUID getUploaderUserId() {
        return uploaderUserId;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public String getContentType() {
        return contentType;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public String getSha256() {
        return sha256;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public Instant getUploadedAt() {
        return uploadedAt;
    }
}
