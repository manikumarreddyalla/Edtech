package com.collegeops.accreditation;

import com.collegeops.common.AuditedEntity;
import com.collegeops.institution.Department;
import com.collegeops.institution.Institution;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "evidence_assignments", uniqueConstraints = {
        @UniqueConstraint(name = "uq_evidence_assignments_id_tenant", columnNames = {"id", "institution_id"})
})
public class EvidenceAssignment extends AuditedEntity {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "institution_id", nullable = false)
    private Institution institution;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "criterion_id", nullable = false)
    private AccreditationCriterion criterion;

    @Column(name = "academic_year", nullable = false, length = 20)
    private String academicYear;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @Column(name = "owner_user_id", nullable = false)
    private UUID ownerUserId;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EvidenceStatus status;

    protected EvidenceAssignment() {
    }

    public EvidenceAssignment(Institution institution, AccreditationCriterion criterion, String academicYear,
                              Department department, UUID ownerUserId, LocalDate dueDate) {
        this.id = UUID.randomUUID();
        this.institution = institution;
        this.criterion = criterion;
        this.academicYear = academicYear;
        this.department = department;
        this.ownerUserId = ownerUserId;
        this.dueDate = dueDate;
        this.status = EvidenceStatus.NOT_STARTED;
    }

    public UUID getId() {
        return id;
    }

    public Institution getInstitution() {
        return institution;
    }

    public AccreditationCriterion getCriterion() {
        return criterion;
    }

    public String getAcademicYear() {
        return academicYear;
    }

    public Department getDepartment() {
        return department;
    }

    public UUID getOwnerUserId() {
        return ownerUserId;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public EvidenceStatus getStatus() {
        return status;
    }

    public void setStatus(EvidenceStatus status) {
        this.status = status;
    }
}
