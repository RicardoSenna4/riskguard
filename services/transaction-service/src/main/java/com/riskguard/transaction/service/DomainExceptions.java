package com.riskguard.transaction.service;

import org.springframework.http.HttpStatus;

public final class DomainExceptions {
    private DomainExceptions() {}
    public static class ApiException extends RuntimeException { private final HttpStatus status; public ApiException(HttpStatus status, String message) { super(message); this.status = status; } public HttpStatus status() { return status; } }
    public static class NotFound extends ApiException { public NotFound(String message) { super(HttpStatus.NOT_FOUND, message); } }
    public static class Conflict extends ApiException { public Conflict(String message) { super(HttpStatus.CONFLICT, message); } }
    public static class TooManyRequests extends ApiException { public TooManyRequests(String message) { super(HttpStatus.TOO_MANY_REQUESTS, message); } }
    public static class Unauthorized extends ApiException { public Unauthorized(String message) { super(HttpStatus.UNAUTHORIZED, message); } }
}
