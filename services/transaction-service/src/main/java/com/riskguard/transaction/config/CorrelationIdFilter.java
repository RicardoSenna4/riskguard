package com.riskguard.transaction.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class CorrelationIdFilter extends OncePerRequestFilter {
    public static final String ATTRIBUTE = "correlationId";
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        String correlationId = request.getHeader("X-Correlation-Id");
        if (correlationId == null || correlationId.isBlank() || correlationId.length() > 100) correlationId = UUID.randomUUID().toString();
        request.setAttribute(ATTRIBUTE, correlationId); response.setHeader("X-Correlation-Id", correlationId); MDC.put(ATTRIBUTE, correlationId);
        try { chain.doFilter(request, response); } finally { MDC.remove(ATTRIBUTE); }
    }
}
