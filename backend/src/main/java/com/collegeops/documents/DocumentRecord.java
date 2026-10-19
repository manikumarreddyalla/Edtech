package com.collegeops.documents;

import com.collegeops.common.AuditedEntity;
import com.collegeops.institution.Department;
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
@Table(name = "documents", uniqueConstraints = {
        @UniqueConstraint(name = "uq_documents_id_institution", columnNames = {"id", "institution_id"})
})
public class DocumentRecord extends AuditedEntity {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "institution_id", nullable = false)
    private Institution institution;

    @Column(name = "owner_user_id", nullable = false)
    private UUID ownerUserId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 50)
    private String category;

    @Column(nullable = false, length = 20)
    private String status;

    protected DocumentRecord() {
    }

    public DocumentRecord(Institution institution, UUID ownerUserId, Department department,
                          String title, String category) {
        this.id = UUID.randomUUID();
        this.institution = institution;
        this.ownerUserId = ownerUserId;
        this.department = department;
        this.title = title;
        this.category = category;
        this.status = "ACTIVE";
    }

    public UUID getId() {
        return id;
    }

    public Institution getInstitution() {
        return institution;
    }

    public UUID getOwnerUserId() {
        return ownerUserId;
    }

    public Department getDepartment() {
        return department;
    }

    public String getTitle() {
        return title;
    }

    public String getCategory() {
        return category;
    }

    public String getStatus() {
        return status;
    }
}
