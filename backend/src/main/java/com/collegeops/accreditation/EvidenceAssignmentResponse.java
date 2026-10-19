package com.collegeops.accreditation;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record EvidenceAssignmentResponse(UUID id, UUID institutionId, UUID criterionId,
                                         String frameworkName, String frameworkVersion,
                                         String criterionCode, String criterionTitle, String category,
                                         String academicYear, UUID departmentId, String departmentName,
                                         UUID ownerUserId, LocalDate dueDate, EvidenceStatus status,
                                         Instant createdAt, Instant updatedAt) {
    static EvidenceAssignmentResponse from(EvidenceAssignment evidence) {
        AccreditationCriterion criterion = evidence.getCriterion();
        return new EvidenceAssignmentResponse(evidence.getId(), evidence.getInstitution().getId(),
                criterion.getId(), criterion.getFramework().getName(),
                criterion.getFramework().getVersionLabel(), criterion.getCode(), criterion.getTitle(),
                criterion.getCategory(), evidence.getAcademicYear(),
                evidence.getDepartment() == null ? null : evidence.getDepartment().getId(),
                evidence.getDepartment() == null ? null : evidence.getDepartment().getName(),
                evidence.getOwnerUserId(), evidence.getDueDate(), evidence.getStatus(),
                evidence.getCreatedAt(), evidence.getUpdatedAt());
    }
}
