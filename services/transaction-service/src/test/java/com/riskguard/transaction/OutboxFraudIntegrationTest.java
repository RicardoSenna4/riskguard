package com.riskguard.transaction;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.riskguard.transaction.domain.FraudDecision;
import com.riskguard.transaction.repository.FraudAnalysisRepository;
import com.riskguard.transaction.repository.OutboxEventRepository;
import com.riskguard.transaction.repository.TransactionRepository;
import com.riskguard.transaction.service.FraudAnalysisService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OutboxFraudIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired OutboxEventRepository outbox;
    @Autowired FraudAnalysisService fraud;
    @Autowired FraudAnalysisRepository analyses;
    @Autowired TransactionRepository transactions;

    @Test
    void transactionAndOutboxAreCreatedTogether() throws Exception {
        String token = registerAndLogin("outbox@example.com");
        String customer = mvc.perform(post("/api/v1/customers").header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"document\":\"DOC-OUTBOX-1\",\"name\":\"Cliente\",\"email\":\"outbox-customer@example.com\"}"))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String customerId = mapper.readTree(customer).get("id").asText();
        long outboxBefore = outbox.countByStatus("PENDING");
        String tx = mvc.perform(post("/api/v1/transactions").header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"customerId\":\"" + customerId + "\",\"externalId\":\"outbox-ext-1\",\"amount\":\"25.00\",\"currency\":\"BRL\",\"merchant\":\"Store\"}"))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        UUID transactionId = UUID.fromString(mapper.readTree(tx).get("id").asText());
        org.junit.jupiter.api.Assertions.assertEquals(outboxBefore + 1, outbox.countByStatus("PENDING"));
        org.junit.jupiter.api.Assertions.assertTrue(transactions.findById(transactionId).isPresent());
    }

    @Test
    void fraudResultIsPersistedAndDuplicateEventDoesNotChangeIt() throws Exception {
        String token = registerAndLogin("fraud-result@example.com");
        String customer = mvc.perform(post("/api/v1/customers").header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"document\":\"DOC-FRAUD-1\",\"name\":\"Cliente\",\"email\":\"fraud-customer@example.com\"}"))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String customerId = mapper.readTree(customer).get("id").asText();
        String tx = mvc.perform(post("/api/v1/transactions").header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"customerId\":\"" + customerId + "\",\"externalId\":\"fraud-ext-1\",\"amount\":\"25.00\",\"currency\":\"BRL\",\"merchant\":\"Store\"}"))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        UUID transactionId = UUID.fromString(mapper.readTree(tx).get("id").asText());
        UUID eventId = UUID.randomUUID();
        String event = mapper.writeValueAsString(new Event(eventId, "fraud.analysis.completed.v1", 1, "corr-fraud", Instant.now(), transactionId, new BigDecimal("0.91"), FraudDecision.BLOCKED, List.of("high_amount"), "rules-v1"));
        fraud.process(event); fraud.process(event);
        org.junit.jupiter.api.Assertions.assertEquals(1, analyses.count());
        org.junit.jupiter.api.Assertions.assertEquals("BLOCKED", transactions.findById(transactionId).orElseThrow().getStatus().name());
    }

    record Event(UUID eventId, String eventType, int schemaVersion, String correlationId, Instant occurredAt, UUID transactionId, BigDecimal riskScore, FraudDecision decision, List<String> reasons, String modelVersion) {}
    private String registerAndLogin(String email) throws Exception { mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"" + email + "\",\"password\":\"correct-horse-battery\"}")); return mapper.readTree(mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"" + email + "\",\"password\":\"correct-horse-battery\"}")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get("accessToken").asText(); }
    private static String bearer(String token) { return "Bearer " + token; }
}
