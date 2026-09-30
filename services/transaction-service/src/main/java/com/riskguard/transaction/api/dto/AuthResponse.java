package com.riskguard.transaction.api.dto;

public record AuthResponse(String accessToken, String tokenType, long expiresInSeconds,
                           String refreshToken, long refreshExpiresInSeconds) {}
