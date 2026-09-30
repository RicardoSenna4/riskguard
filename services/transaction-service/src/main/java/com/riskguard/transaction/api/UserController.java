package com.riskguard.transaction.api;

import com.riskguard.transaction.api.dto.UserResponse;
import com.riskguard.transaction.service.AuthService;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    private final AuthService auth;
    public UserController(AuthService auth) { this.auth = auth; }

    @GetMapping("/{userId}/profile")
    @PreAuthorize("hasRole('ADMIN') or #userId.toString() == authentication.name")
    public UserResponse profile(@PathVariable UUID userId) { return auth.profile(userId); }
}
