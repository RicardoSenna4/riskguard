package com.riskguard.transaction.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_events", indexes = @Index(name = "ix_audit_created_at", columnList = "created_at"))
public class AuditEvent {
    @Id private UUID id;
    @Column(nullable=false, length=80) private String action;
    @Column(name="actor_id") private UUID actorId;
    @Column(name="correlation_id", nullable=false, length=100) private String correlationId;
    @Column(name="resource_type", length=80) private String resourceType;
    @Column(name="resource_id") private UUID resourceId;
    @Lob @Column(name="before_json") private String beforeJson;
    @Lob @Column(name="after_json") private String afterJson;
    @Column(name="created_at", nullable=false) private Instant createdAt;
    protected AuditEvent() {}
    public AuditEvent(UUID id,String action,UUID actorId,String correlationId,String resourceType,UUID resourceId,String beforeJson,String afterJson,Instant createdAt){this.id=id;this.action=action;this.actorId=actorId;this.correlationId=correlationId;this.resourceType=resourceType;this.resourceId=resourceId;this.beforeJson=beforeJson;this.afterJson=afterJson;this.createdAt=createdAt;}
    public UUID getId(){return id;} public String getAction(){return action;} public UUID getActorId(){return actorId;} public String getCorrelationId(){return correlationId;} public String getResourceType(){return resourceType;} public UUID getResourceId(){return resourceId;} public String getBeforeJson(){return beforeJson;} public String getAfterJson(){return afterJson;} public Instant getCreatedAt(){return createdAt;}
}
