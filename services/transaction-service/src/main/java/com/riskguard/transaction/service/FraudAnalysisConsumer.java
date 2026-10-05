package com.riskguard.transaction.service;

import org.springframework.beans.factory.annotation.Value; import org.springframework.context.annotation.Profile; import org.springframework.kafka.annotation.KafkaListener; import org.springframework.kafka.core.KafkaTemplate; import org.springframework.stereotype.Component;

@Component @Profile("!test") public class FraudAnalysisConsumer {
 private final FraudAnalysisService service; private final KafkaTemplate<String,String> kafka; private final String dlq;
 public FraudAnalysisConsumer(FraudAnalysisService s,KafkaTemplate<String,String> k,@Value("${app.kafka.fraud-dlq-topic:fraud.analysis.dlq.v1}") String dlq){service=s;kafka=k;this.dlq=dlq;}
 @KafkaListener(topics="${app.kafka.fraud-result-topic:fraud.analysis.completed.v1}",groupId="${app.kafka.transaction-consumer-group:riskguard-transaction-service}")
 public void consume(String payload){Exception failure=null;for(int attempt=1;attempt<=3;attempt++){try{service.process(payload);return;}catch(Exception e){failure=e;try{Thread.sleep(100L*attempt);}catch(InterruptedException interrupted){Thread.currentThread().interrupt();break;}}}kafka.send(dlq,payload);}
}
