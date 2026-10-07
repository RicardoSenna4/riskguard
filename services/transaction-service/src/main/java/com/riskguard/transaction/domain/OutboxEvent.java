package com.riskguard.transaction.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name="outbox_events", indexes=@Index(name="ix_outbox_pending", columnList="status,next_attempt_at"))
public class OutboxEvent {
    @Id private UUID id;
    @Column(name="event_type", nullable=false, length=120) private String eventType;
    @Column(name="aggregate_id", nullable=false) private UUID aggregateId;
    @Column(name="correlation_id", nullable=false, length=100) private String correlationId;
    @Column(nullable=false, length=20) private String status;
    @Column(nullable=false, columnDefinition="text") private String payload;
    @Column(nullable=false) private int attempts;
    @Column(name="next_attempt_at", nullable=false) private Instant nextAttemptAt;
    @Column(name="published_at") private Instant publishedAt;
    @Column(name="last_error", length=1000) private String lastError;
    @Column(name="created_at", nullable=false) private Instant createdAt;
    protected OutboxEvent() {}
    public OutboxEvent(UUID id,String type,UUID aggregate,String correlation,String payload,Instant now){this.id=id;eventType=type;aggregateId=aggregate;correlationId=correlation;this.payload=payload;status="PENDING";attempts=0;nextAttemptAt=now;createdAt=now;}
    public UUID getId(){return id;} public String getEventType(){return eventType;} public UUID getAggregateId(){return aggregateId;} public String getCorrelationId(){return correlationId;} public String getStatus(){return status;} public String getPayload(){return payload;} public int getAttempts(){return attempts;} public Instant getNextAttemptAt(){return nextAttemptAt;} public Instant getPublishedAt(){return publishedAt;} public String getLastError(){return lastError;}
    public void markPublished(Instant now){status="PUBLISHED";publishedAt=now;}
    public void markFailed(Instant now,String error){attempts++;lastError=error==null?"unknown":error.substring(0,Math.min(1000,error.length()));nextAttemptAt=now.plusSeconds(Math.min(300L,1L << Math.min(attempts,8)));}
}
