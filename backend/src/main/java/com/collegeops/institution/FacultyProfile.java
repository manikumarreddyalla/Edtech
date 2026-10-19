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
@Table(name = "faculty_profiles", uniqueConstraints = {
        @UniqueConstraint(name = "uq_faculty_institution_user", columnNames = {"institution_id", "user_id"})
})
public class FacultyProfile extends AuditedEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "institution_id", nullable = false)
    private Institution institution;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    protected FacultyProfile() {
    }

    public UUID getId() {
        return id;
    }
}
