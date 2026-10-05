package com.riskguard.transaction.service;

import com.fasterxml.jackson.databind.ObjectMapper; import com.riskguard.transaction.api.event.FraudAnalysisCompletedEvent; import com.riskguard.transaction.domain.*; import com.riskguard.transaction.repository.*; import java.time.Instant; import java.util.UUID; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional;

@Service public class FraudAnalysisService {
 private final FraudAnalysisRepository analyses; private final ProcessedEventRepository processed; private final TransactionRepository transactions; private final TransactionStatusHistoryRepository history; private final ObjectMapper mapper;
 public FraudAnalysisService(FraudAnalysisRepository a,ProcessedEventRepository p,TransactionRepository t,TransactionStatusHistoryRepository h,ObjectMapper m){analyses=a;processed=p;transactions=t;history=h;mapper=m;}
 @Transactional public void process(String raw){
  try{FraudAnalysisCompletedEvent e=mapper.readValue(raw,FraudAnalysisCompletedEvent.class); if(processed.existsById(e.eventId())||analyses.existsByEventId(e.eventId()))return; Transaction t=transactions.findById(e.transactionId()).orElseThrow(()->new DomainExceptions.NotFound("Transaction not found")); Instant now=Instant.now(); analyses.save(new FraudAnalysis(UUID.randomUUID(),t,e.riskScore(),e.decision(),mapper.writeValueAsString(e.reasons()),e.modelVersion(),e.eventId(),e.correlationId(),now)); t.applyFraudResult(e.riskScore(),e.modelVersion(),toStatus(e.decision()),now); transactions.saveAndFlush(t); history.save(new TransactionStatusHistory(UUID.randomUUID(),t,t.getStatus(),now,null)); processed.save(new ProcessedEvent(e.eventId(),e.eventType(),now)); }catch(Exception ex){if(ex instanceof DomainExceptions.ApiException a)throw a;throw new IllegalStateException("Invalid fraud result event",ex);}
 }
 private static TransactionStatus toStatus(FraudDecision d){return switch(d){case APPROVED->TransactionStatus.APPROVED;case REVIEW->TransactionStatus.REVIEW;case BLOCKED->TransactionStatus.BLOCKED;};}
}
