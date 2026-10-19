package com.collegeops.institution;

import java.time.Instant;
import java.util.UUID;

public record AcademicProgramResponse(UUID id, UUID institutionId, UUID departmentId, String departmentName,
                                      String name, String code, String status,
                                      Instant createdAt, Instant updatedAt) {
    static AcademicProgramResponse from(AcademicProgram program) {
        return new AcademicProgramResponse(program.getId(), program.getInstitution().getId(),
                program.getDepartment().getId(), program.getDepartment().getName(), program.getName(),
                program.getCode(), program.getStatus(), program.getCreatedAt(), program.getUpdatedAt());
    }
}
