package com.riskguard.transaction;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CustomerTransactionIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    @Test
    void managesCustomerAndPreventsDuplicateDocument() throws Exception {
        String token = registerAndLogin("customer-flow@example.com");
        String customer = """
            {"document":"123.456.789-00","name":"Ana Silva","email":"ana@example.com"}
            """;
        String created = mvc.perform(post("/api/v1/customers").header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON).content(customer))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.active").value(true)).andExpect(jsonPath("$.version").value(0)).andReturn().getResponse().getContentAsString();
        String id = mapper.readTree(created).get("id").asText();
        mvc.perform(post("/api/v1/customers").header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON).content("{\"document\":\"123.456.789-00\",\"name\":\"Outra\",\"email\":\"outra@example.com\"}"))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("CONFLICT"));
        mvc.perform(post("/api/v1/customers/{id}/inactivate", id).header("Authorization", bearer(token)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.active").value(false));
        mvc.perform(get("/api/v1/customers").header("Authorization", bearer(token)).param("page", "0").param("size", "10"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(1)).andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void createsTransactionWithIdempotencyAndTracksStatusHistory() throws Exception {
        String token = registerAndLogin("transaction-flow@example.com");
        String customer = mvc.perform(post("/api/v1/customers").header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"document\":\"DOC-TRANSACTION-1\",\"name\":\"Cliente\",\"email\":\"cliente@example.com\"}"))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String customerId = mapper.readTree(customer).get("id").asText();
        String transaction = "{\"customerId\":\"" + customerId + "\",\"externalId\":\"ext-001\",\"amount\":\"120.5000\",\"currency\":\"brl\",\"merchant\":\"Loja Teste\"}";
        String first = mvc.perform(post("/api/v1/transactions").header("Authorization", bearer(token)).header("Idempotency-Key", "tx-key-1").contentType(MediaType.APPLICATION_JSON).content(transaction))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("PENDING")).andExpect(jsonPath("$.risk").value("UNKNOWN")).andReturn().getResponse().getContentAsString();
        String firstId = mapper.readTree(first).get("id").asText();
        mvc.perform(post("/api/v1/transactions").header("Authorization", bearer(token)).header("Idempotency-Key", "tx-key-1").contentType(MediaType.APPLICATION_JSON).content(transaction))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(firstId));
        mvc.perform(post("/api/v1/transactions").header("Authorization", bearer(token)).header("Idempotency-Key", "tx-key-1").contentType(MediaType.APPLICATION_JSON).content(transaction.replace("120.5000", "121.5000")))
            .andExpect(status().isConflict());
        String statusUpdate = "{\"status\":\"APPROVED\",\"version\":0}";
        mvc.perform(patch("/api/v1/transactions/{id}/status", firstId).header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON).content(statusUpdate))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("APPROVED")).andExpect(jsonPath("$.version").value(1));
        mvc.perform(get("/api/v1/transactions/{id}/status-history", firstId).header("Authorization", bearer(token)))
            .andExpect(status().isOk()).andExpect(jsonPath("$[0].status").value("PENDING")).andExpect(jsonPath("$[1].status").value("APPROVED"));
        mvc.perform(patch("/api/v1/transactions/{id}/status", firstId).header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON).content(statusUpdate))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    @Test
    void errorsContainCorrelationIdAndDoNotExposeStackTrace() throws Exception {
        mvc.perform(get("/api/v1/customers").header("X-Correlation-Id", "test-correlation"))
            .andExpect(status().isUnauthorized()).andExpect(header().string("X-Correlation-Id", "test-correlation"))
            .andExpect(jsonPath("$.code").value("UNAUTHORIZED")).andExpect(jsonPath("$.correlationId").value("test-correlation"))
            .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    void concurrentRequestsWithSameIdempotencyKeyReturnTheSameResource() throws Exception {
        String token = registerAndLogin("concurrency@example.com");
        String customer = mvc.perform(post("/api/v1/customers").header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"document\":\"DOC-CONCURRENT-1\",\"name\":\"Cliente\",\"email\":\"conc@example.com\"}"))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String customerId = mapper.readTree(customer).get("id").asText();
        String payload = "{\"customerId\":\"" + customerId + "\",\"externalId\":\"ext-concurrent\",\"amount\":\"10.00\",\"currency\":\"BRL\",\"merchant\":\"Concurrent\"}";
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Callable<String> call = () -> mvc.perform(post("/api/v1/transactions").header("Authorization", bearer(token)).header("Idempotency-Key", "concurrent-key").contentType(MediaType.APPLICATION_JSON).content(payload)).andReturn().getResponse().getContentAsString();
            Future<String> first = executor.submit(call); Future<String> second = executor.submit(call);
            String firstBody = first.get(); String secondBody = second.get();
            JsonNode one = mapper.readTree(firstBody); JsonNode two = mapper.readTree(secondBody);
            org.junit.jupiter.api.Assertions.assertTrue(one.has("id"), firstBody);
            org.junit.jupiter.api.Assertions.assertTrue(two.has("id"), secondBody);
            org.junit.jupiter.api.Assertions.assertEquals(one.get("id").asText(), two.get("id").asText());
        } finally { executor.shutdownNow(); }
    }

    private String registerAndLogin(String email) throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"" + email + "\",\"password\":\"correct-horse-battery\"}"))
            .andExpect(status().isCreated());
        String response = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"" + email + "\",\"password\":\"correct-horse-battery\"}"))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode node = mapper.readTree(response); return node.get("accessToken").asText();
    }
    private static String bearer(String token) { return "Bearer " + token; }
}
