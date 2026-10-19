package com.collegeops.institution;

import com.collegeops.common.AuditedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.UUID;

@Entity
@Table(name = "students", uniqueConstraints = {
        @UniqueConstraint(name = "uq_students_institution_number", columnNames = {"institution_id", "student_number"}),
        @UniqueConstraint(name = "uq_students_institution_user", columnNames = {"institution_id", "user_id"})
})
public class Student extends AuditedEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "institution_id", nullable = false)
    private Institution institution;

    @Column(name = "user_id")
    private UUID userId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "academic_program_id", nullable = false)
    private AcademicProgram academicProgram;

    @Column(name = "student_number", nullable = false, length = 80)
    private String studentNumber;

    @Column(nullable = false, length = 20)
    private String status;

    protected Student() {
    }

    public Student(Institution institution, UUID userId, Department department,
                   AcademicProgram academicProgram, String studentNumber) {
        this.id = UUID.randomUUID();
        this.institution = institution;
        this.userId = userId;
        this.department = department;
        this.academicProgram = academicProgram;
        this.studentNumber = studentNumber;
        this.status = "ACTIVE";
    }

    public UUID getId() {
        return id;
    }

    public Institution getInstitution() {
        return institution;
    }

    public UUID getUserId() {
        return userId;
    }

    public Department getDepartment() {
        return department;
    }

    public AcademicProgram getAcademicProgram() {
        return academicProgram;
    }

    public String getStudentNumber() {
        return studentNumber;
    }

    public String getStatus() {
        return status;
    }
}
