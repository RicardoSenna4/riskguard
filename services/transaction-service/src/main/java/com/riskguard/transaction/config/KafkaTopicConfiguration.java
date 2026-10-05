package com.riskguard.transaction.config;
import org.apache.kafka.clients.admin.NewTopic; import org.springframework.beans.factory.annotation.Value; import org.springframework.context.annotation.Bean; import org.springframework.context.annotation.Configuration; import org.springframework.context.annotation.Profile; import org.springframework.kafka.config.TopicBuilder;
@Configuration @Profile("!test") public class KafkaTopicConfiguration {
 @Bean NewTopic transactionCreated(@Value("${app.kafka.transaction-created-topic:transaction.created.v1}") String name){return TopicBuilder.name(name).partitions(3).replicas(1).build();}
 @Bean NewTopic fraudCompleted(@Value("${app.kafka.fraud-result-topic:fraud.analysis.completed.v1}") String name){return TopicBuilder.name(name).partitions(3).replicas(1).build();}
 @Bean NewTopic fraudDlq(@Value("${app.kafka.fraud-dlq-topic:fraud.analysis.dlq.v1}") String name){return TopicBuilder.name(name).partitions(3).replicas(1).build();}
}
