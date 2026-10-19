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
@Table(name = "accreditation_frameworks", uniqueConstraints = {
        @UniqueConstraint(name = "uq_frameworks_tenant_name_version",
                columnNames = {"institution_id", "name", "version_label"}),
        @UniqueConstraint(name = "uq_frameworks_id_tenant", columnNames = {"id", "institution_id"})
})
public class AccreditationFramework extends AuditedEntity {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "institution_id", nullable = false)
    private Institution institution;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "version_label", nullable = false, length = 50)
    private String versionLabel;

    @Column(nullable = false, length = 20)
    private String status;

    protected AccreditationFramework() {
    }

    public AccreditationFramework(Institution institution, String name, String versionLabel) {
        this.id = UUID.randomUUID();
        this.institution = institution;
        this.name = name;
        this.versionLabel = versionLabel;
        this.status = "ACTIVE";
    }

    public UUID getId() {
        return id;
    }

    public Institution getInstitution() {
        return institution;
    }

    public String getName() {
        return name;
    }

    public String getVersionLabel() {
        return versionLabel;
    }

    public String getStatus() {
        return status;
    }
}
