package com.collegeops.institution;

import com.collegeops.common.AuditedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "institutions")
public class Institution extends AuditedEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, unique = true, length = 80)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InstitutionStatus status;

    protected Institution() {
    }

    public Institution(String name, String code) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.code = code;
        this.status = InstitutionStatus.ACTIVE;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCode() {
        return code;
    }

    public InstitutionStatus getStatus() {
        return status;
    }
}
