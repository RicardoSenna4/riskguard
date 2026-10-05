package com.riskguard.transaction.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "customers")
public class Customer {
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id", nullable = false) private UserAccount user;
    @Column(nullable = false, length = 40) private String document;
    @Column(nullable = false, length = 160) private String name;
    @Column(nullable = false, length = 320) private String email;
    @Column(nullable = false) private boolean active;
    @Version @Column(nullable = false) private long version;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    protected Customer() {}
    public Customer(UUID id, UserAccount user, String document, String name, String email, Instant now) {
        this.id = id; this.user = user; this.document = document; this.name = name; this.email = email;
        this.active = true; this.createdAt = now; this.updatedAt = now;
    }
    public UUID getId() { return id; } public UserAccount getUser() { return user; }
    public String getDocument() { return document; } public String getName() { return name; }
    public String getEmail() { return email; } public boolean isActive() { return active; }
    public long getVersion() { return version; } public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void update(String name, String email, Instant now) { this.name = name; this.email = email; this.updatedAt = now; }
    public void activate(Instant now) { this.active = true; this.updatedAt = now; }
    public void deactivate(Instant now) { this.active = false; this.updatedAt = now; }
}
