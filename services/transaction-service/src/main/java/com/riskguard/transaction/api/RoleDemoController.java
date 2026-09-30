package com.riskguard.transaction.api;

import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RoleDemoController {
    @GetMapping("/api/v1/analyst/ping")
    @PreAuthorize("hasAnyRole('ANALYST', 'ADMIN')")
    public Map<String, String> analyst() { return Map.of("scope", "analyst"); }

    @GetMapping("/api/v1/admin/ping")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, String> admin() { return Map.of("scope", "admin"); }
}
