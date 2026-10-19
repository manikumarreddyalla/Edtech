package com.collegeops.accreditation;

import java.time.Instant;
import java.util.UUID;

public record AccreditationCriterionResponse(UUID id, UUID institutionId, UUID frameworkId,
                                             String frameworkName, String frameworkVersion,
                                             String code, String title, String category, String description,
                                             Instant createdAt, Instant updatedAt) {
    static AccreditationCriterionResponse from(AccreditationCriterion criterion) {
        return new AccreditationCriterionResponse(criterion.getId(), criterion.getInstitution().getId(),
                criterion.getFramework().getId(), criterion.getFramework().getName(),
                criterion.getFramework().getVersionLabel(), criterion.getCode(), criterion.getTitle(),
                criterion.getCategory(), criterion.getDescription(),
                criterion.getCreatedAt(), criterion.getUpdatedAt());
    }
}
