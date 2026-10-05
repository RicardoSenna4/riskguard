package com.riskguard.transaction.api.dto;

import com.riskguard.transaction.domain.RiskLevel;
import com.riskguard.transaction.domain.TransactionStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class TransactionDtos {
    private TransactionDtos() {}
    public record Create(@NotNull UUID customerId, @NotBlank @Size(max = 100) String externalId, @NotNull @DecimalMin(value = "0.0001") BigDecimal amount, @NotBlank @Pattern(regexp = "[A-Za-z]{3}") String currency, @NotBlank @Size(max = 160) String merchant) {}
    public record StatusUpdate(@NotNull TransactionStatus status, @NotNull Long version) {}
    public record Response(UUID id, UUID customerId, String externalId, BigDecimal amount, String currency, String merchant, TransactionStatus status, RiskLevel risk, BigDecimal riskScore, String riskModelVersion, long version, Instant createdAt, Instant updatedAt) {}
    public record HistoryResponse(UUID id, TransactionStatus status, Instant changedAt, UUID changedBy) {}
}
