package com.riskguard.transaction.api.event;
import com.riskguard.transaction.domain.FraudDecision; import java.math.BigDecimal; import java.time.Instant; import java.util.List; import java.util.UUID;
public record FraudAnalysisCompletedEvent(UUID eventId,String eventType,int schemaVersion,String correlationId,Instant occurredAt,UUID transactionId,BigDecimal riskScore,FraudDecision decision,List<String> reasons,String modelVersion) {}
