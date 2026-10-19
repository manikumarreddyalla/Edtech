package com.collegeops.accreditation;

import com.collegeops.common.AuditedEntity;
import com.collegeops.institution.Institution;
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
@Table(name = "accreditation_criteria", uniqueConstraints = {
        @UniqueConstraint(name = "uq_criteria_framework_code", columnNames = {"framework_id", "code"}),
        @UniqueConstraint(name = "uq_criteria_id_tenant", columnNames = {"id", "institution_id"})
})
public class AccreditationCriterion extends AuditedEntity {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "institution_id", nullable = false)
    private Institution institution;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "framework_id", nullable = false)
    private AccreditationFramework framework;

    @Column(nullable = false, length = 80)
    private String code;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 100)
    private String category;

    @Column(length = 2000)
    private String description;

    protected AccreditationCriterion() {
    }

    public AccreditationCriterion(Institution institution, AccreditationFramework framework,
                                  String code, String title, String category, String description) {
        this.id = UUID.randomUUID();
        this.institution = institution;
        this.framework = framework;
        this.code = code;
        this.title = title;
        this.category = category;
        this.description = description;
    }

    public UUID getId() {
        return id;
    }

    public Institution getInstitution() {
        return institution;
    }

    public AccreditationFramework getFramework() {
        return framework;
    }

    public String getCode() {
        return code;
    }

    public String getTitle() {
        return title;
    }

    public String getCategory() {
        return category;
    }

    public String getDescription() {
        return description;
    }
}
