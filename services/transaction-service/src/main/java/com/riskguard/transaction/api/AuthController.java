package com.riskguard.transaction.api;

import com.riskguard.transaction.api.dto.AuthResponse;
import com.riskguard.transaction.api.dto.LoginRequest;
import com.riskguard.transaction.api.dto.LogoutRequest;
import com.riskguard.transaction.api.dto.RefreshRequest;
import com.riskguard.transaction.api.dto.RegisterRequest;
import com.riskguard.transaction.api.dto.UserResponse;
import com.riskguard.transaction.service.AuthService;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService auth;
    public AuthController(AuthService auth) { this.auth = auth; }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody RegisterRequest body) { return auth.register(body); }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest body) { return auth.login(body); }

    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest body) { return auth.refresh(body.refreshToken()); }

    @PostMapping("/logout")
    public Map<String, String> logout(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody LogoutRequest body) {
        auth.logout(UUID.fromString(jwt.getSubject()), body.refreshToken());
        return Map.of("message", "Session revoked");
    }
}
