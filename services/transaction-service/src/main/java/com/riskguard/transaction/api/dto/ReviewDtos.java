package com.riskguard.transaction.api.dto;
import com.riskguard.transaction.domain.*; import jakarta.validation.constraints.NotBlank; import jakarta.validation.constraints.NotNull; import jakarta.validation.constraints.Size; import java.math.BigDecimal; import java.time.Instant; import java.util.UUID;
public final class ReviewDtos { private ReviewDtos(){}
 public record Create(@NotNull ReviewDecision decision,@NotBlank @Size(max=1000) String reason){}
 public record Response(UUID id,UUID transactionId,UUID reviewerId,ReviewDecision decision,String reason,FraudDecision automaticDecision,BigDecimal originalRiskScore,String correlationId,Instant reviewedAt){}
}
