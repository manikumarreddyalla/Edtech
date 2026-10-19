package com.collegeops.documents;

import java.time.Instant;
import java.util.UUID;

public record DocumentResponse(UUID id, UUID institutionId, UUID ownerUserId, UUID departmentId,
                               String departmentName, String title, String category, String status,
                               Instant createdAt, Instant updatedAt) {
    static DocumentResponse from(DocumentRecord document) {
        return new DocumentResponse(document.getId(), document.getInstitution().getId(),
                document.getOwnerUserId(),
                document.getDepartment() == null ? null : document.getDepartment().getId(),
                document.getDepartment() == null ? null : document.getDepartment().getName(),
                document.getTitle(), document.getCategory(), document.getStatus(),
                document.getCreatedAt(), document.getUpdatedAt());
    }
}
