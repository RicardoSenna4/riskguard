package com.riskguard.transaction.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name="fraud_analyses", uniqueConstraints=@UniqueConstraint(name="uq_fraud_analysis_transaction", columnNames="transaction_id"))
public class FraudAnalysis {
    @Id private UUID id;
    @OneToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="transaction_id", nullable=false) private Transaction transaction;
    @Column(name="risk_score", nullable=false, precision=5, scale=4) private BigDecimal riskScore;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=20) private FraudDecision decision;
    @Lob @Column(nullable=false) private String reasons;
    @Column(name="model_version", nullable=false, length=80) private String modelVersion;
    @Column(name="event_id", nullable=false, unique=true) private UUID eventId;
    @Column(name="correlation_id", nullable=false, length=100) private String correlationId;
    @Column(name="created_at", nullable=false) private Instant createdAt;
    protected FraudAnalysis() {}
    public FraudAnalysis(UUID id,Transaction transaction,BigDecimal score,FraudDecision decision,String reasons,String version,UUID eventId,String correlation,Instant now){this.id=id;this.transaction=transaction;riskScore=score;this.decision=decision;this.reasons=reasons;modelVersion=version;this.eventId=eventId;correlationId=correlation;createdAt=now;}
    public UUID getId(){return id;} public Transaction getTransaction(){return transaction;} public BigDecimal getRiskScore(){return riskScore;} public FraudDecision getDecision(){return decision;} public String getReasons(){return reasons;} public String getModelVersion(){return modelVersion;} public UUID getEventId(){return eventId;} public String getCorrelationId(){return correlationId;} public Instant getCreatedAt(){return createdAt;}
}
