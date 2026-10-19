package com.collegeops.accreditation;

import com.collegeops.documents.DocumentVersion;

import java.time.Instant;
import java.util.UUID;

public record EvidenceAttachmentResponse(UUID documentId, int versionNumber,
                                         String filename, String contentType, long sizeBytes,
                                         String sha256, Instant uploadedAt) {
    static EvidenceAttachmentResponse from(DocumentVersion version) {
        return new EvidenceAttachmentResponse(version.getDocument().getId(), version.getVersionNumber(),
                version.getOriginalFilename(), version.getContentType(), version.getSizeBytes(),
                version.getSha256(), version.getUploadedAt());
    }
}
