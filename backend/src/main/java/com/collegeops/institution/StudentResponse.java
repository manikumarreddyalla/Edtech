package com.collegeops.institution;

import java.time.Instant;
import java.util.UUID;

public record StudentResponse(UUID id, UUID institutionId, UUID userId, String studentNumber,
                              UUID departmentId, String departmentName,
                              UUID academicProgramId, String academicProgramName,
                              String status, Instant createdAt, Instant updatedAt) {
    static StudentResponse from(Student student) {
        return new StudentResponse(student.getId(), student.getInstitution().getId(), student.getUserId(),
                student.getStudentNumber(), student.getDepartment().getId(), student.getDepartment().getName(),
                student.getAcademicProgram().getId(), student.getAcademicProgram().getName(),
                student.getStatus(), student.getCreatedAt(), student.getUpdatedAt());
    }
}
