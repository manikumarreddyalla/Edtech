package com.collegeops.institution;

import java.time.Instant;
import java.util.UUID;

public record DepartmentResponse(UUID id, UUID institutionId, String name, String code, String status,
                                 Instant createdAt, Instant updatedAt) {
    static DepartmentResponse from(Department department) {
        return new DepartmentResponse(department.getId(), department.getInstitution().getId(),
                department.getName(), department.getCode(), department.getStatus(),
                department.getCreatedAt(), department.getUpdatedAt());
    }
}
