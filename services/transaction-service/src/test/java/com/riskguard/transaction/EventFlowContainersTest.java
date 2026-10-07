package com.riskguard.transaction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.riskguard.transaction.domain.OutboxEvent;
import com.riskguard.transaction.domain.TransactionStatus;
import com.riskguard.transaction.repository.FraudAnalysisRepository;
import com.riskguard.transaction.repository.OutboxEventRepository;
import com.riskguard.transaction.repository.TransactionRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;

/**
 * Full event-driven flow against real PostgreSQL, Kafka and Redis, with the production
 * configuration (Flyway migrations, schema validation, outbox publisher and Kafka consumer).
 *
 * <p>POST /transactions → PostgreSQL → outbox → transaction.created.v1 →
 * fraud.analysis.completed.v1 → consumer → transaction status updated.
 * The fraud-service side is simulated by publishing its result event; the cross-service
 * flow with the real Python service is covered by scripts/e2e-event-flow.sh in CI.
 */
@Testcontainers
@EnabledIfEnvironmentVariable(named = "RUN_CONTAINERS", matches = "true")
@SpringBootTest
@AutoConfigureMockMvc
class EventFlowContainersTest {
    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    @Container static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");
    @Container static KafkaContainer kafka = new KafkaContainer("apache/kafka:4.0.0");
    @Container static GenericContainer<?> redis = new GenericContainer<>("redis:7.4-alpine").withExposedPorts(6379);

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        registry.add("app.security.jwt.secret", () -> "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=");
        registry.add("app.outbox.poll-ms", () -> "200");
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired TransactionRepository transactions;
    @Autowired OutboxEventRepository outbox;
    @Autowired FraudAnalysisRepository analyses;
    @Autowired KafkaTemplate<String, String> kafkaTemplate;
    @Autowired StringRedisTemplate redisTemplate;

    @Test
    void transactionFlowsThroughOutboxAndKafkaAndFraudResultUpdatesStatus() throws Exception {
        String token = registerAndLogin("event-flow@example.com");
        String customerId = createCustomer(token);

        // When: a transaction is created through the API
        String created = mvc.perform(post("/api/v1/transactions").header("Authorization", "Bearer " + token)
                .header("X-Correlation-Id", "corr-event-flow").contentType(MediaType.APPLICATION_JSON)
                .content("{\"customerId\":\"" + customerId + "\",\"externalId\":\"event-flow-1\",\"amount\":\"2500.00\",\"currency\":\"BRL\",\"merchant\":\"Store\"}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("PENDING"))
            .andReturn().getResponse().getContentAsString();
        UUID transactionId = UUID.fromString(mapper.readTree(created).get("id").asText());

        // Then: it is stored in PostgreSQL and the outbox row is published
        assertThat(transactions.findById(transactionId)).isPresent();
        OutboxEvent published = await("outbox event published", () -> outbox.findAll().stream()
            .filter(e -> e.getAggregateId().equals(transactionId) && "PUBLISHED".equals(e.getStatus())).findFirst());
        assertThat(published.getEventType()).isEqualTo("transaction.created.v1");

        // And: transaction.created.v1 reaches Kafka keyed by the transaction id
        JsonNode event = mapper.readTree(consumeOne("transaction.created.v1", transactionId.toString()).value());
        assertThat(event.get("eventId").asText()).isEqualTo(published.getId().toString());
        assertThat(event.get("transactionId").asText()).isEqualTo(transactionId.toString());
        assertThat(event.get("correlationId").asText()).isEqualTo("corr-event-flow");
        assertThat(event.get("schemaVersion").asInt()).isEqualTo(1);

        // When: the fraud service publishes its result (delivered twice to prove idempotency)
        String result = mapper.writeValueAsString(Map.of(
            "eventId", UUID.randomUUID().toString(), "eventType", "fraud.analysis.completed.v1", "schemaVersion", 1,
            "correlationId", "corr-event-flow", "occurredAt", Instant.now().toString(), "transactionId", transactionId.toString(),
            "riskScore", "0.9100", "decision", "BLOCKED", "reasons", List.of("high_amount"), "modelVersion", "rules-v1"));
        kafkaTemplate.send("fraud.analysis.completed.v1", transactionId.toString(), result).get(10, TimeUnit.SECONDS);
        kafkaTemplate.send("fraud.analysis.completed.v1", transactionId.toString(), result).get(10, TimeUnit.SECONDS);

        // Then: the consumer updates the transaction status exactly once
        await("transaction blocked", () -> transactions.findById(transactionId).filter(t -> t.getStatus() == TransactionStatus.BLOCKED));
        assertThat(analyses.findByTransactionId(transactionId)).isPresent();
        mvc.perform(get("/api/v1/transactions/{id}/status-history", transactionId).header("Authorization", "Bearer " + token))
            .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].status").value("PENDING")).andExpect(jsonPath("$[1].status").value("BLOCKED"));

        // And: rate limiting used the real Redis
        assertThat(redisTemplate.keys("riskguard:rate:*")).isNotEmpty();
    }

    private ConsumerRecord<String, String> consumeOne(String topic, String key) {
        Map<String, Object> props = Map.of(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers(),
            ConsumerConfig.GROUP_ID_CONFIG, "event-flow-test-" + UUID.randomUUID(),
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(List.of(topic));
            return await(topic + " record", () -> {
                for (ConsumerRecord<String, String> record : consumer.poll(Duration.ofMillis(500))) {
                    if (key.equals(record.key())) return Optional.of(record);
                }
                return Optional.empty();
            });
        }
    }

    private static <T> T await(String description, Supplier<Optional<T>> probe) {
        Instant deadline = Instant.now().plus(TIMEOUT);
        while (Instant.now().isBefore(deadline)) {
            Optional<T> value = probe.get();
            if (value.isPresent()) return value.get();
            try { Thread.sleep(200); } catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IllegalStateException(e); }
        }
        throw new AssertionError("Timed out waiting for " + description);
    }

    private String createCustomer(String token) throws Exception {
        String customer = mvc.perform(post("/api/v1/customers").header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"document\":\"DOC-EVENT-FLOW-1\",\"name\":\"Cliente\",\"email\":\"event-flow-customer@example.com\"}"))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return mapper.readTree(customer).get("id").asText();
    }

    private String registerAndLogin(String email) throws Exception {
        String credentials = "{\"email\":\"" + email + "\",\"password\":\"correct-horse-battery\"}";
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(credentials)).andExpect(status().isCreated());
        String response = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(credentials))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return mapper.readTree(response).get("accessToken").asText();
    }
}
