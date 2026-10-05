package com.riskguard.transaction.service;

import com.fasterxml.jackson.core.JsonProcessingException; import com.fasterxml.jackson.databind.ObjectMapper; import com.riskguard.transaction.api.event.TransactionCreatedEvent; import com.riskguard.transaction.domain.OutboxEvent; import com.riskguard.transaction.domain.Transaction; import com.riskguard.transaction.repository.OutboxEventRepository; import java.time.Instant; import java.util.UUID; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional;

@Service public class OutboxService {
 private final OutboxEventRepository events; private final ObjectMapper mapper;
 public OutboxService(OutboxEventRepository events,ObjectMapper mapper){this.events=events;this.mapper=mapper;}
 @Transactional public void enqueueTransactionCreated(Transaction t,String correlationId){
  Instant now=Instant.now(); TransactionCreatedEvent event=new TransactionCreatedEvent(UUID.randomUUID(),"transaction.created.v1",1,correlationId,now,t.getId(),t.getCustomer().getId(),t.getAmount(),t.getCurrency(),t.getMerchant());
  try{events.save(new OutboxEvent(event.eventId(),event.eventType(),t.getId(),correlationId,mapper.writeValueAsString(event),now));}catch(JsonProcessingException e){throw new IllegalStateException("Unable to serialize outbox event",e);}
 }
 public long pendingCount(){return events.countByStatus("PENDING");}
}
