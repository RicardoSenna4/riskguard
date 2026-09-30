package com.riskguard.transaction.api.dto;

import com.riskguard.transaction.domain.Role;
import java.time.Instant;
import java.util.UUID;

public record UserResponse(UUID id, String email, Role role, Instant createdAt) {}
