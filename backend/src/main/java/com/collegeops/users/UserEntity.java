package com.collegeops.users;

import com.collegeops.common.AuditedEntity;
import com.collegeops.institution.Institution;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(name = "uq_users_institution_email", columnNames = {"institution_id", "email"})
})
public class UserEntity extends AuditedEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "institution_id", nullable = false)
    private Institution institution;

    @Column(nullable = false, length = 254)
    private String email;

    @Column(name = "display_name", nullable = false, length = 200)
    private String displayName;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 40)
    private Set<UserRole> roles = new HashSet<>();

    protected UserEntity() {
    }

    public UserEntity(Institution institution, String email, String displayName, String passwordHash,
                      Set<UserRole> roles) {
        this.id = UUID.randomUUID();
        this.institution = institution;
        this.email = email.toLowerCase().strip();
        this.displayName = displayName;
        this.passwordHash = passwordHash;
        this.status = UserStatus.ACTIVE;
        this.roles = new HashSet<>(roles);
    }

    public UUID getId() {
        return id;
    }

    public Institution getInstitution() {
        return institution;
    }

    public String getEmail() {
        return email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public UserStatus getStatus() {
        return status;
    }

    public Set<UserRole> getRoles() {
        return Set.copyOf(roles);
    }
}
