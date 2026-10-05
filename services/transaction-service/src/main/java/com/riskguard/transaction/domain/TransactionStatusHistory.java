package com.riskguard.transaction.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transaction_status_history")
public class TransactionStatusHistory {
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "transaction_id", nullable = false) private Transaction transaction;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private TransactionStatus status;
    @Column(name = "changed_at", nullable = false) private Instant changedAt;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "changed_by") private UserAccount changedBy;
    protected TransactionStatusHistory() {}
    public TransactionStatusHistory(UUID id, Transaction transaction, TransactionStatus status, Instant changedAt, UserAccount changedBy) {
        this.id = id; this.transaction = transaction; this.status = status; this.changedAt = changedAt; this.changedBy = changedBy;
    }
    public UUID getId() { return id; } public TransactionStatus getStatus() { return status; }
    public Instant getChangedAt() { return changedAt; } public UserAccount getChangedBy() { return changedBy; }
}
