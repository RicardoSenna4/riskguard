package com.riskguard.transaction.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "fraud_reviews")
public class FraudReview {
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "transaction_id", nullable = false) private Transaction transaction;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "reviewer_id", nullable = false) private UserAccount reviewer;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private ReviewDecision decision;
    @Column(nullable = false, length = 1000) private String reason;
    @Enumerated(EnumType.STRING) @Column(name = "automatic_decision", nullable = false, length = 20) private FraudDecision automaticDecision;
    @Column(name = "original_risk_score", nullable = false, precision = 5, scale = 4) private java.math.BigDecimal originalRiskScore;
    @Column(name = "correlation_id", nullable = false, length = 100) private String correlationId;
    @Column(name = "reviewed_at", nullable = false) private Instant reviewedAt;
    protected FraudReview() {}
    public FraudReview(UUID id, Transaction transaction, UserAccount reviewer, ReviewDecision decision, String reason, FraudDecision automaticDecision, java.math.BigDecimal originalRiskScore, String correlationId, Instant reviewedAt) { this.id=id; this.transaction=transaction; this.reviewer=reviewer; this.decision=decision; this.reason=reason; this.automaticDecision=automaticDecision; this.originalRiskScore=originalRiskScore; this.correlationId=correlationId; this.reviewedAt=reviewedAt; }
    public UUID getId(){return id;} public Transaction getTransaction(){return transaction;} public UserAccount getReviewer(){return reviewer;} public ReviewDecision getDecision(){return decision;} public String getReason(){return reason;} public FraudDecision getAutomaticDecision(){return automaticDecision;} public java.math.BigDecimal getOriginalRiskScore(){return originalRiskScore;} public String getCorrelationId(){return correlationId;} public Instant getReviewedAt(){return reviewedAt;}
}
