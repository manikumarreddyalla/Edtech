package com.collegeops.training;

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
import java.util.Locale;

@Entity
@Table(name = "training_skills", uniqueConstraints =
        @UniqueConstraint(name = "uq_training_skills_institution_normalized_name",
                columnNames = {"institution_id", "normalized_name"}))
public class TrainingSkill extends AuditedEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "institution_id", nullable = false)
    private Institution institution;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "normalized_name", nullable = false, length = 120)
    private String normalizedName;

    @Column(length = 1000)
    private String description;

    protected TrainingSkill() {
    }

    public TrainingSkill(Institution institution, String name, String description) {
        this.id = UUID.randomUUID();
        this.institution = institution;
        this.name = name;
        this.normalizedName = name.toLowerCase(Locale.ROOT);
        this.description = description;
    }

    public UUID getId() { return id; }
    public Institution getInstitution() { return institution; }
    public String getName() { return name; }
    public String getNormalizedName() { return normalizedName; }
    public String getDescription() { return description; }
}
