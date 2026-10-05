package com.riskguard.transaction.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transactions")
public class Transaction {
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "customer_id", nullable = false) private Customer customer;
    @Column(name = "external_id", nullable = false, unique = true, length = 100) private String externalId;
    @Column(nullable = false, precision = 19, scale = 4) private BigDecimal amount;
    @Column(nullable = false, length = 3) private String currency;
    @Column(nullable = false, length = 160) private String merchant;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private TransactionStatus status;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private RiskLevel risk;
    @Version @Column(nullable = false) private long version;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    protected Transaction() {}
    public Transaction(UUID id, Customer customer, String externalId, BigDecimal amount, String currency, String merchant, Instant now) {
        this.id = id; this.customer = customer; this.externalId = externalId; this.amount = amount; this.currency = currency;
        this.merchant = merchant; this.status = TransactionStatus.PENDING; this.risk = RiskLevel.UNKNOWN; this.createdAt = now; this.updatedAt = now;
    }
    public UUID getId() { return id; } public Customer getCustomer() { return customer; }
    public String getExternalId() { return externalId; } public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; } public String getMerchant() { return merchant; }
    public TransactionStatus getStatus() { return status; } public RiskLevel getRisk() { return risk; }
    public long getVersion() { return version; } public Instant getCreatedAt() { return createdAt; } public Instant getUpdatedAt() { return updatedAt; }
    public void changeStatus(TransactionStatus status, Instant now) { this.status = status; this.updatedAt = now; }
    public void changeRisk(RiskLevel risk, Instant now) { this.risk = risk; this.updatedAt = now; }
}
