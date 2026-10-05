package com.riskguard.transaction.service;

import com.riskguard.transaction.domain.OutboxEvent; import com.riskguard.transaction.repository.OutboxEventRepository; import io.micrometer.core.instrument.MeterRegistry; import java.time.Instant; import org.springframework.beans.factory.annotation.Value; import org.springframework.context.annotation.Profile; import org.springframework.data.domain.PageRequest; import org.springframework.kafka.core.KafkaTemplate; import org.springframework.scheduling.annotation.Scheduled; import org.springframework.stereotype.Component; import org.springframework.transaction.annotation.Transactional;

@Component @Profile("!test") public class OutboxPublisher {
 private final OutboxEventRepository events; private final KafkaTemplate<String,String> kafka; private final MeterRegistry metrics; private final String topic;
 public OutboxPublisher(OutboxEventRepository events,KafkaTemplate<String,String> kafka,MeterRegistry metrics,@Value("${app.kafka.transaction-created-topic:transaction.created.v1}") String topic){this.events=events;this.kafka=kafka;this.metrics=metrics;this.topic=topic;metrics.gauge("riskguard.outbox.backlog",events,repo -> repo.countByStatus("PENDING"));}
 @Scheduled(fixedDelayString="${app.outbox.poll-ms:1000}") @Transactional public void publish(){
  for(OutboxEvent event:events.findPending(Instant.now(),PageRequest.of(0,50))){try{kafka.send(topic,event.getAggregateId().toString(),event.getPayload()).get(5,java.util.concurrent.TimeUnit.SECONDS);event.markPublished(Instant.now());metrics.counter("riskguard.outbox.published").increment();}catch(Exception ex){event.markFailed(Instant.now(),ex.getMessage());metrics.counter("riskguard.outbox.failures").increment();}}
 }
}
