package com.riskguard.transaction.service;

import org.slf4j.Logger; import org.slf4j.LoggerFactory; import org.slf4j.MDC; import org.springframework.beans.factory.annotation.Value; import org.springframework.context.annotation.Profile; import org.springframework.kafka.annotation.KafkaListener; import org.springframework.kafka.core.KafkaTemplate; import org.springframework.kafka.support.KafkaHeaders; import org.springframework.messaging.handler.annotation.Header; import org.springframework.messaging.support.MessageBuilder; import org.springframework.stereotype.Component;

@Component @Profile("!test") public class FraudAnalysisConsumer {
 private static final Logger log=LoggerFactory.getLogger(FraudAnalysisConsumer.class);
 private final FraudAnalysisService service; private final KafkaTemplate<String,String> kafka; private final String dlq;
 public FraudAnalysisConsumer(FraudAnalysisService s,KafkaTemplate<String,String> k,@Value("${app.kafka.fraud-dlq-topic:fraud.analysis.dlq.v1}") String dlq){service=s;kafka=k;this.dlq=dlq;}
 @KafkaListener(topics="${app.kafka.fraud-result-topic:fraud.analysis.completed.v1}",groupId="${app.kafka.transaction-consumer-group:riskguard-transaction-service}")
 public void consume(String payload,@Header(name="X-Correlation-Id",required=false) String correlationId,@Header(name="X-Event-Id",required=false) String eventId){if(correlationId!=null)MDC.put("correlationId",correlationId);if(eventId!=null)MDC.put("eventId",eventId);Exception last=null;try{for(int attempt=1;attempt<=3;attempt++){try{service.process(payload);return;}catch(Exception e){last=e;log.warn("Fraud result processing failed (attempt {}/3): {}",attempt,rootMessage(e));try{Thread.sleep(100L*attempt);}catch(InterruptedException interrupted){Thread.currentThread().interrupt();break;}}}log.error("Sending fraud result to DLQ {} after retries",dlq,last);kafka.send(MessageBuilder.withPayload(payload).setHeader(KafkaHeaders.TOPIC,dlq).setHeader("X-Correlation-Id",correlationId).setHeader("X-Event-Id",eventId).build());}finally{MDC.remove("correlationId");MDC.remove("eventId");}}
 private static String rootMessage(Throwable e){Throwable c=e;while(c.getCause()!=null)c=c.getCause();return c.getClass().getSimpleName()+": "+c.getMessage();}
}
