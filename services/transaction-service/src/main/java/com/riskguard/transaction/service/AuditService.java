package com.riskguard.transaction.service;
import com.fasterxml.jackson.core.JsonProcessingException; import com.fasterxml.jackson.databind.ObjectMapper; import com.riskguard.transaction.domain.AuditEvent; import com.riskguard.transaction.repository.AuditEventRepository; import java.time.Instant; import java.util.UUID; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional;
@Service public class AuditService { private final AuditEventRepository events; private final ObjectMapper mapper; public AuditService(AuditEventRepository events,ObjectMapper mapper){this.events=events;this.mapper=mapper;}
 @Transactional public void record(String action,UUID actorId,String correlationId,String resourceType,UUID resourceId,Object before,Object after){events.save(new AuditEvent(UUID.randomUUID(),action,actorId,correlationId,resourceType,resourceId,json(before),json(after),Instant.now()));}
 private String json(Object value){if(value==null)return null;try{return mapper.writeValueAsString(value);}catch(JsonProcessingException e){return "{\"serializationError\":true}";}}
}
