package com.collegeops.common;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_events")
public class AuditEvent {
    @Id
    private UUID id;

    @Column(name = "institution_id", nullable = false)
    private UUID institutionId;

    @Column(name = "actor_user_id")
    private UUID actorUserId;

    @Column(nullable = false, length = 100)
    private String action;

    @Column(name = "target_type", nullable = false, length = 100)
    private String targetType;

    @Column(name = "target_id", nullable = false)
    private UUID targetId;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(length = 1000)
    private String summary;

    protected AuditEvent() {
    }

    public AuditEvent(UUID institutionId, UUID actorUserId, String action, String targetType,
                      UUID targetId, String summary) {
        this.id = UUID.randomUUID();
        this.institutionId = institutionId;
        this.actorUserId = actorUserId;
        this.action = action;
        this.targetType = targetType;
        this.targetId = targetId;
        this.occurredAt = Instant.now();
        this.summary = summary;
    }
}
