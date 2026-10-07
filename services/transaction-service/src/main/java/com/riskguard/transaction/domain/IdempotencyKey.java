package com.riskguard.transaction.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "idempotency_keys", uniqueConstraints = @UniqueConstraint(name = "uq_idempotency_user_key", columnNames = {"user_id", "key_value"}))
public class IdempotencyKey {
    @Id private UUID id;
    @Column(name = "user_id", nullable = false) private UUID userId;
    @Column(name = "key_value", nullable = false, length = 255) private String keyValue;
    @Column(name = "request_hash", nullable = false, length = 64) private String requestHash;
    @Column(name = "response_status", nullable = false) private int responseStatus;
    @Column(name = "response_body", nullable = false, columnDefinition = "text") private String responseBody;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "expires_at", nullable = false) private Instant expiresAt;
    protected IdempotencyKey() {}
    public IdempotencyKey(UUID id, UUID userId, String keyValue, String requestHash, int responseStatus, String responseBody, Instant createdAt, Instant expiresAt) {
        this.id = id; this.userId = userId; this.keyValue = keyValue; this.requestHash = requestHash; this.responseStatus = responseStatus;
        this.responseBody = responseBody; this.createdAt = createdAt; this.expiresAt = expiresAt;
    }
    public String getRequestHash() { return requestHash; } public int getResponseStatus() { return responseStatus; }
    public String getResponseBody() { return responseBody; } public Instant getExpiresAt() { return expiresAt; }
}
