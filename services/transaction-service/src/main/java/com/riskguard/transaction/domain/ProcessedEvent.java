package com.riskguard.transaction.domain;
import jakarta.persistence.*; import java.time.Instant; import java.util.UUID;
@Entity @Table(name="processed_events") public class ProcessedEvent { @Id private UUID eventId; @Column(nullable=false,length=120) private String eventType; @Column(nullable=false) private Instant processedAt; protected ProcessedEvent(){} public ProcessedEvent(UUID id,String type,Instant at){eventId=id;eventType=type;processedAt=at;} }
