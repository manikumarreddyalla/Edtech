package com.collegeops.accreditation;

import java.time.Instant;
import java.util.UUID;

public record AccreditationFrameworkResponse(UUID id, UUID institutionId, String name,
                                             String versionLabel, String status,
                                             Instant createdAt, Instant updatedAt) {
    static AccreditationFrameworkResponse from(AccreditationFramework framework) {
        return new AccreditationFrameworkResponse(framework.getId(), framework.getInstitution().getId(),
                framework.getName(), framework.getVersionLabel(), framework.getStatus(),
                framework.getCreatedAt(), framework.getUpdatedAt());
    }
}
