package com.riskguard.transaction.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public final class CustomerDtos {
    private CustomerDtos() {}
    public record Create(@NotBlank @Size(max = 40) String document, @NotBlank @Size(max = 160) String name, @NotBlank @Email @Size(max = 320) String email) {}
    public record Update(@NotBlank @Size(max = 160) String name, @NotBlank @Email @Size(max = 320) String email) {}
    public record Response(UUID id, UUID userId, String document, String name, String email, boolean active, long version, Instant createdAt, Instant updatedAt) {}
}
