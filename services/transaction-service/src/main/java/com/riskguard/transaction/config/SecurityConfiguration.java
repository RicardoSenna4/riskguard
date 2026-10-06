package com.riskguard.transaction.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Instant;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfiguration {
    @Bean
    PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(12); }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, ObjectMapper mapper, CorrelationIdFilter correlationIdFilter, RateLimitFilter rateLimitFilter, @org.springframework.beans.factory.annotation.Qualifier("corsConfigurationSource") org.springframework.web.cors.CorsConfigurationSource cors,
            @Value("${management.endpoints.web.base-path:/actuator}") String actuatorPath) throws Exception {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("roles");
        authorities.setAuthorityPrefix("");
        JwtAuthenticationConverter jwtConverter = new JwtAuthenticationConverter();
        jwtConverter.setJwtGrantedAuthoritiesConverter(authorities);

        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(c -> c.configurationSource(cors))
            .headers(h -> h.contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'; frame-ancestors 'none'; object-src 'none'; base-uri 'self'")).frameOptions(frame -> frame.deny()).referrerPolicy(referrer -> referrer.policy(org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER)).httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(31536000)))
            .addFilterBefore(correlationIdFilter, BearerTokenAuthenticationFilter.class)
            .addFilterAfter(rateLimitFilter, CorrelationIdFilter.class)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/register", "/api/v1/auth/login", "/api/v1/auth/refresh", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .requestMatchers(HttpMethod.GET, actuatorPath + "/health", actuatorPath + "/health/**", actuatorPath + "/info").permitAll()
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/v1/analyst/**").hasAnyRole("ANALYST", "ADMIN")
                .anyRequest().authenticated())
            .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtConverter)))
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, cause) -> writeError(mapper, request, response, 401, "Unauthorized", "UNAUTHORIZED", "Authentication is required or invalid"))
                .accessDeniedHandler((request, response, cause) -> writeError(mapper, request, response, 403, "Forbidden", "FORBIDDEN", "You do not have permission to access this resource")));
        return http.build();
    }

    private static void writeError(ObjectMapper mapper, jakarta.servlet.http.HttpServletRequest request, HttpServletResponse response, int status, String error, String code, String message) throws java.io.IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        String correlation = String.valueOf(request.getAttribute(CorrelationIdFilter.ATTRIBUTE));
        response.setHeader("X-Correlation-Id", correlation);
        mapper.writeValue(response.getOutputStream(), Map.of("timestamp", Instant.now().toString(), "status", status, "error", error, "code", code, "message", message, "path", request.getRequestURI(), "correlationId", correlation));
    }
}
