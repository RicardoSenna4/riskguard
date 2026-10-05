package com.riskguard.transaction.api.event;
import java.math.BigDecimal; import java.time.Instant; import java.util.UUID;
public record TransactionCreatedEvent(UUID eventId,String eventType,int schemaVersion,String correlationId,Instant occurredAt,UUID transactionId,UUID customerId,BigDecimal amount,String currency,String merchant) {}
