package com.collegeops.documents;

import java.time.Instant;
import java.util.UUID;

public record DocumentVersionResponse(UUID id, UUID documentId, int versionNumber, UUID uploaderUserId,
                                      String originalFilename, String contentType, long sizeBytes,
                                      String sha256, Instant uploadedAt) {
    static DocumentVersionResponse from(DocumentVersion version) {
        return new DocumentVersionResponse(version.getId(), version.getDocument().getId(),
                version.getVersionNumber(), version.getUploaderUserId(), version.getOriginalFilename(),
                version.getContentType(), version.getSizeBytes(), version.getSha256(), version.getUploadedAt());
    }
}
