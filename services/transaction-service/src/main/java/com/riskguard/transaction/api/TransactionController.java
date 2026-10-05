package com.riskguard.transaction.api;

import com.riskguard.transaction.api.dto.PageResponse;
import com.riskguard.transaction.api.dto.TransactionDtos;
import com.riskguard.transaction.domain.RiskLevel;
import com.riskguard.transaction.domain.TransactionStatus;
import com.riskguard.transaction.service.IdempotencyService;
import com.riskguard.transaction.service.TransactionService;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {
    private final TransactionService service; private final IdempotencyService idempotency;
    public TransactionController(TransactionService service, IdempotencyService idempotency) { this.service = service; this.idempotency = idempotency; }
    @PostMapping public ResponseEntity<?> create(@AuthenticationPrincipal Jwt jwt, @RequestHeader(value = "Idempotency-Key", required = false) String key, @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId, @Valid @RequestBody TransactionDtos.Create body) { UUID userId = userId(jwt); return idempotency.execute(userId, key, body, HttpStatus.CREATED.value(), () -> service.create(userId, body, elevated(jwt), correlationId == null ? UUID.randomUUID().toString() : correlationId)); }
    @GetMapping("/{id}") public TransactionDtos.Response get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) { return service.get(userId(jwt), id, elevated(jwt)); }
    @GetMapping public PageResponse<TransactionDtos.Response> list(@AuthenticationPrincipal Jwt jwt, @RequestParam(required = false) UUID customerId, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to, @RequestParam(required = false) TransactionStatus status, @RequestParam(required = false) RiskLevel risk, @RequestParam(required = false) String merchant, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) { return service.list(userId(jwt), customerId, from, to, status, risk, merchant, elevated(jwt), pageable(page, size)); }
    @PatchMapping("/{id}/status") public TransactionDtos.Response status(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id, @Valid @RequestBody TransactionDtos.StatusUpdate body) { return service.changeStatus(userId(jwt), id, body, elevated(jwt)); }
    @GetMapping("/{id}/status-history") public java.util.List<TransactionDtos.HistoryResponse> history(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) { return service.history(userId(jwt), id, elevated(jwt)); }
    private static UUID userId(Jwt jwt) { return UUID.fromString(jwt.getSubject()); }
    private static boolean elevated(Jwt jwt) { var roles = jwt.getClaimAsStringList("roles"); return roles != null && (roles.contains("ROLE_ANALYST") || roles.contains("ROLE_ADMIN")); }
    private static Pageable pageable(int page, int size) { if (page < 0 || size < 1 || size > 100) throw new IllegalArgumentException("page must be >= 0 and size must be between 1 and 100"); return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")); }
}
