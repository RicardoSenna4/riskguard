package com.riskguard.transaction.api;

import com.riskguard.transaction.api.dto.CustomerDtos;
import com.riskguard.transaction.api.dto.PageResponse;
import com.riskguard.transaction.service.CustomerService;
import com.riskguard.transaction.service.IdempotencyService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/customers")
public class CustomerController {
    private final CustomerService service; private final IdempotencyService idempotency;
    public CustomerController(CustomerService service, IdempotencyService idempotency) { this.service = service; this.idempotency = idempotency; }
    @PostMapping
    public ResponseEntity<?> create(@AuthenticationPrincipal Jwt jwt, @RequestHeader(value = "Idempotency-Key", required = false) String key, @Valid @RequestBody CustomerDtos.Create body) { UUID userId = userId(jwt); return idempotency.execute(userId, key, body, HttpStatus.CREATED.value(), () -> service.create(userId, body)); }
    @GetMapping("/{id}") public CustomerDtos.Response get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) { return service.get(userId(jwt), id, elevated(jwt)); }
    @GetMapping public PageResponse<CustomerDtos.Response> list(@AuthenticationPrincipal Jwt jwt, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) { return service.list(userId(jwt), elevated(jwt), pageable(page, size)); }
    @PutMapping("/{id}") public CustomerDtos.Response update(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id, @Valid @RequestBody CustomerDtos.Update body) { return service.update(userId(jwt), id, body, elevated(jwt)); }
    @PostMapping("/{id}/activate") public CustomerDtos.Response activate(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) { return service.setActive(userId(jwt), id, true, elevated(jwt)); }
    @PostMapping("/{id}/inactivate") public CustomerDtos.Response inactivate(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) { return service.setActive(userId(jwt), id, false, elevated(jwt)); }
    private static UUID userId(Jwt jwt) { return UUID.fromString(jwt.getSubject()); }
    private static boolean elevated(Jwt jwt) { var roles = jwt.getClaimAsStringList("roles"); return roles != null && (roles.contains("ROLE_ANALYST") || roles.contains("ROLE_ADMIN")); }
    private static Pageable pageable(int page, int size) { if (page < 0 || size < 1 || size > 100) throw new IllegalArgumentException("page must be >= 0 and size must be between 1 and 100"); return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")); }
}
