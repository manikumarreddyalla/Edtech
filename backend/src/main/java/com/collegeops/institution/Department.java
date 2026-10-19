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
@Table(name = "departments", uniqueConstraints = {
        @UniqueConstraint(name = "uq_departments_institution_code", columnNames = {"institution_id", "code"})
})
public class Department extends AuditedEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "institution_id", nullable = false)
    private Institution institution;

    @Column(nullable = false, length = 180)
    private String name;

    @Column(nullable = false, length = 40)
    private String code;

    @Column(nullable = false, length = 20)
    private String status;

    protected Department() {
    }

    public UUID getId() {
        return id;
    }

    public Institution getInstitution() {
        return institution;
    }
}
