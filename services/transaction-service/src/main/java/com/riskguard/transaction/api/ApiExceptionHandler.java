package com.riskguard.transaction.api;

import com.riskguard.transaction.config.CorrelationIdFilter;
import com.riskguard.transaction.service.DomainExceptions;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.converter.HttpMessageNotReadableException;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(BadCredentialsException.class) ResponseEntity<?> badCredentials(HttpServletRequest req) { return error(HttpStatus.UNAUTHORIZED, "AUTH_INVALID_CREDENTIALS", "Invalid email or password", req); }
    @ExceptionHandler(AuthorizationDeniedException.class) ResponseEntity<?> denied(HttpServletRequest req) { return error(HttpStatus.FORBIDDEN, "FORBIDDEN", "You do not have permission to access this resource", req); }
    @ExceptionHandler(DomainExceptions.ApiException.class) ResponseEntity<?> domain(DomainExceptions.ApiException ex, HttpServletRequest req) { return error(ex.status(), code(ex.status()), ex.getMessage(), req); }
    @ExceptionHandler(DataIntegrityViolationException.class) ResponseEntity<?> conflict(HttpServletRequest req) { return error(HttpStatus.CONFLICT, "RESOURCE_CONFLICT", "A record with these unique values already exists", req); }
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class) ResponseEntity<?> locking(HttpServletRequest req) { return error(HttpStatus.CONFLICT, "OPTIMISTIC_LOCK_CONFLICT", "The resource was modified by another request", req); }
    @ExceptionHandler(MethodArgumentNotValidException.class) ResponseEntity<?> invalid(HttpServletRequest req) { return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Request validation failed", req); }
    @ExceptionHandler({ConstraintViolationException.class, MethodArgumentTypeMismatchException.class, HttpMessageNotReadableException.class, HttpMediaTypeNotSupportedException.class, IllegalArgumentException.class}) ResponseEntity<?> malformed(HttpServletRequest req) { return error(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "The request is invalid", req); }
    @ExceptionHandler(ResponseStatusException.class) ResponseEntity<?> status(ResponseStatusException ex, HttpServletRequest req) { return error(HttpStatus.valueOf(ex.getStatusCode().value()), "HTTP_" + ex.getStatusCode().value(), ex.getReason() == null ? "Request failed" : ex.getReason(), req); }
    @ExceptionHandler(Exception.class) ResponseEntity<?> unexpected(HttpServletRequest req) { return error(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "An unexpected error occurred", req); }
    private ResponseEntity<ErrorResponse> error(HttpStatus status, String code, String message, HttpServletRequest req) { String correlation = String.valueOf(req.getAttribute(CorrelationIdFilter.ATTRIBUTE)); return ResponseEntity.status(status).body(new ErrorResponse(Instant.now(), status.value(), status.getReasonPhrase(), code, message, req.getRequestURI(), correlation)); }
    private static String code(HttpStatus status) { return switch (status) { case BAD_REQUEST -> "VALIDATION_ERROR"; case UNAUTHORIZED -> "UNAUTHORIZED"; case FORBIDDEN -> "FORBIDDEN"; case NOT_FOUND -> "NOT_FOUND"; case CONFLICT -> "CONFLICT"; case TOO_MANY_REQUESTS -> "RATE_LIMITED"; default -> "HTTP_" + status.value(); }; }
    public record ErrorResponse(Instant timestamp, int status, String error, String code, String message, String path, String correlationId) {}
}
