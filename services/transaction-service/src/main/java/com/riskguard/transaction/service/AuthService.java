package com.riskguard.transaction.service;

import com.riskguard.transaction.api.dto.AuthResponse;
import com.riskguard.transaction.api.dto.LoginRequest;
import com.riskguard.transaction.api.dto.RegisterRequest;
import com.riskguard.transaction.api.dto.UserResponse;
import com.riskguard.transaction.domain.RefreshToken;
import com.riskguard.transaction.domain.Role;
import com.riskguard.transaction.domain.UserAccount;
import com.riskguard.transaction.repository.RefreshTokenRepository;
import com.riskguard.transaction.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {
    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokens;
    private final AuditService audit;
    private final long accessSeconds;
    private final long refreshSeconds;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(UserRepository users, RefreshTokenRepository refreshTokens, PasswordEncoder passwordEncoder,
            JwtTokenService jwtTokens, AuditService audit, @Value("${app.security.jwt.access-expiration-seconds:900}") long accessSeconds,
            @Value("${app.security.jwt.refresh-expiration-seconds:604800}") long refreshSeconds) {
        this.users = users; this.refreshTokens = refreshTokens; this.passwordEncoder = passwordEncoder;
        this.jwtTokens = jwtTokens; this.audit = audit; this.accessSeconds = accessSeconds; this.refreshSeconds = refreshSeconds;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (users.existsByEmail(email)) throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already registered");
        UserAccount user = users.save(new UserAccount(UUID.randomUUID(), email,
            passwordEncoder.encode(request.password()), Role.USER, Instant.now()));
        audit.record("USER_CREATED", user.getId(), UUID.randomUUID().toString(), "USER", user.getId(), null, toResponse(user));
        return toResponse(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        UserAccount user = users.findByEmail(normalizeEmail(request.email()))
            .filter(candidate -> passwordEncoder.matches(request.password(), candidate.getPasswordHash()))
            .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
        audit.record("LOGIN", user.getId(), UUID.randomUUID().toString(), "USER", user.getId(), null, null);
        return issuePair(user);
    }

    @Transactional
    public AuthResponse refresh(String rawToken) {
        String hash = hash(rawToken);
        RefreshToken old = refreshTokens.findByTokenHash(hash)
            .filter(token -> token.getRevokedAt() == null && token.getExpiresAt().isAfter(Instant.now()))
            .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));
        old.revoke(Instant.now());
        audit.record("TOKEN_REFRESHED", old.getUser().getId(), UUID.randomUUID().toString(), "USER", old.getUser().getId(), null, null);
        return issuePair(old.getUser());
    }

    @Transactional
    public void logout(UUID userId, String rawToken) {
        RefreshToken token = refreshTokens.findByTokenHash(hash(rawToken))
            .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));
        if (!token.getUser().getId().equals(userId)) throw new BadCredentialsException("Invalid refresh token");
        if (token.getRevokedAt() == null) { token.revoke(Instant.now()); audit.record("LOGOUT", userId, UUID.randomUUID().toString(), "USER", userId, null, null); }
    }

    @Transactional(readOnly = true)
    public UserResponse profile(UUID userId) {
        return users.findById(userId).map(AuthService::toResponse)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private AuthResponse issuePair(UserAccount user) {
        Instant now = Instant.now();
        String access = jwtTokens.issue(user, now, now.plusSeconds(accessSeconds));
        byte[] bytes = new byte[32]; secureRandom.nextBytes(bytes);
        String refresh = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        refreshTokens.save(new RefreshToken(UUID.randomUUID(), user, hash(refresh), now.plusSeconds(refreshSeconds), now));
        return new AuthResponse(access, "Bearer", accessSeconds, refresh, refreshSeconds);
    }

    private static String hash(String raw) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (java.security.NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }
    private static String normalizeEmail(String email) { return email.trim().toLowerCase(java.util.Locale.ROOT); }
    private static UserResponse toResponse(UserAccount user) { return new UserResponse(user.getId(), user.getEmail(), user.getRole(), user.getCreatedAt()); }
}
