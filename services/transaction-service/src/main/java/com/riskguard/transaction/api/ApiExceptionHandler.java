package com.riskguard.transaction.api;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(BadCredentialsException.class)
    ResponseEntity<?> badCredentials(HttpServletRequest req) { return error(HttpStatus.UNAUTHORIZED, "Invalid email or password", req); }
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<?> conflict(HttpServletRequest req) { return error(HttpStatus.CONFLICT, "A record with these unique values already exists", req); }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<?> invalid(HttpServletRequest req) { return error(HttpStatus.BAD_REQUEST, "Request validation failed", req); }
    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<?> invalidToken(HttpServletRequest req) { return error(HttpStatus.UNAUTHORIZED, "Invalid or expired token", req); }

    private ResponseEntity<?> error(HttpStatus status, String message, HttpServletRequest req) {
        return ResponseEntity.status(status).body(Map.of("timestamp", Instant.now().toString(), "status", status.value(),
            "error", status.getReasonPhrase(), "message", message, "path", req.getRequestURI()));
    }
}
