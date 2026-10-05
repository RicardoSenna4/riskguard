package com.riskguard.transaction.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.riskguard.transaction.domain.IdempotencyKey;
import com.riskguard.transaction.repository.IdempotencyKeyRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.HexFormat;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class IdempotencyService {
    private final IdempotencyKeyRepository keys; private final ObjectMapper mapper; private final long expirationSeconds;
    private final ConcurrentHashMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();
    public IdempotencyService(IdempotencyKeyRepository keys, ObjectMapper mapper, @Value("${app.idempotency.expiration-seconds:86400}") long expirationSeconds) { this.keys = keys; this.mapper = mapper; this.expirationSeconds = expirationSeconds; }
    @Transactional
    public ResponseEntity<?> execute(UUID userId, String rawKey, Object payload, int status, Supplier<?> action) {
        if (rawKey == null || rawKey.isBlank()) return ResponseEntity.status(status).body(action.get());
        String key = rawKey.trim(); if (key.length() > 255) throw new DomainExceptions.ApiException(org.springframework.http.HttpStatus.BAD_REQUEST, "Idempotency-Key must contain at most 255 characters");
        String lockName = userId + ":" + key; ReentrantLock lock = locks.computeIfAbsent(lockName, ignored -> new ReentrantLock()); lock.lock(); boolean unlockOnExit = true;
        try {
            String hash = hash(json(payload)); Instant now = Instant.now();
            var existing = keys.findByUserIdAndKeyValue(userId, key).orElse(null);
            if (existing != null && existing.getExpiresAt().isAfter(now)) {
                if (!existing.getRequestHash().equals(hash)) throw new DomainExceptions.Conflict("Idempotency-Key was already used with a different payload");
                try { JsonNode response = mapper.readTree(existing.getResponseBody()); return ResponseEntity.status(existing.getResponseStatus()).body(response); }
                catch (JsonProcessingException ex) { throw new IllegalStateException("Stored idempotency response is invalid", ex); }
            }
            Object result = action.get(); String responseBody = json(result);
            keys.save(new IdempotencyKey(UUID.randomUUID(), userId, key, hash, status, responseBody, now, now.plusSeconds(expirationSeconds)));
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCompletion(int completionStatus) { unlock(lockName, lock); }
            });
            unlockOnExit = false;
            return ResponseEntity.status(status).body(result);
        } finally {
            if (unlockOnExit) unlock(lockName, lock);
        }
    }
    private void unlock(String lockName, ReentrantLock lock) { lock.unlock(); locks.remove(lockName, lock); }
    private String json(Object value) { try { return mapper.writeValueAsString(value); } catch (JsonProcessingException ex) { throw new IllegalStateException("Unable to serialize idempotency payload", ex); } }
    private static String hash(String value) { try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); } catch (Exception ex) { throw new IllegalStateException(ex); } }
}
