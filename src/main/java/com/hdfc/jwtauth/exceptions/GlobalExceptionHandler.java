package com.hdfc.jwtauth.exceptions;

import com.hdfc.jwtauth.web.ApiResponse;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String AUTH_SERVICE_UNAVAILABLE_CODE = "AUTH_SERVICE_UNAVAILABLE";
    private static final String AUTH_SERVICE_UNAVAILABLE_MESSAGE =
            "Authentication service is temporarily unavailable. Please try again later.";
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage())
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
    }
    /** Creates the same structured error payload for every exception handled here. */
    private ResponseEntity<Map<String, Object>> buildResponse(
            HttpStatus status, String message, HttpServletRequest request) {

        Map<String, Object> response = Map.of(
                "timestamp", Instant.now().toString(),
                "status", status.value(),
                "error", status.getReasonPhrase(),
                "message", message,
                "path", request.getRequestURI()
        );

        return ResponseEntity.status(status).body(response);
    }

    /**
     * Keeps infrastructure details out of the public API while providing clients
     * with a stable code they can safely use for presentation logic.
     */
    private ResponseEntity<Map<String, Object>> buildAuthenticationServiceUnavailableResponse(
            HttpServletRequest request) {

        Map<String, Object> response = Map.of(
                "timestamp", Instant.now().toString(),
                "status", HttpStatus.SERVICE_UNAVAILABLE.value(),
                "error", HttpStatus.SERVICE_UNAVAILABLE.getReasonPhrase(),
                "code", AUTH_SERVICE_UNAVAILABLE_CODE,
                "message", AUTH_SERVICE_UNAVAILABLE_MESSAGE,
                "path", request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidCredentials(
            InvalidCredentialsException ex, HttpServletRequest request) {

        log.warn("Login authentication failed for {} {}", request.getMethod(), request.getRequestURI());
        return buildResponse(HttpStatus.UNAUTHORIZED, "Invalid username or password", request);
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidToken(
            InvalidTokenException ex, HttpServletRequest request) {

        log.warn("Token authentication failed for {} {}", request.getMethod(), request.getRequestURI());
        return buildResponse(HttpStatus.UNAUTHORIZED, "Invalid or expired token", request);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleUserNotFound(
            UserNotFoundException ex, HttpServletRequest request) {

        log.warn("User not found on {} {}", request.getMethod(), request.getRequestURI());
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(DatabaseUnavailableException.class)
    public ResponseEntity<Map<String, Object>> handleDatabaseUnavailable(
            DatabaseUnavailableException ex, HttpServletRequest request) {

        log.error(
                "Authentication dependency failure: database unavailable on {} {}",
                request.getMethod(),
                request.getRequestURI(),
                ex
        );
        return buildAuthenticationServiceUnavailableResponse(request);
    }

    @ExceptionHandler(RequestNotPermitted.class)
    public ResponseEntity<Map<String, Object>> handleRateLimit(
            RequestNotPermitted ex, HttpServletRequest request) {

        log.warn("Request limit exceeded for {} {}", request.getMethod(), request.getRequestURI());
        return buildResponse(HttpStatus.TOO_MANY_REQUESTS, "Too many requests. Try again later.", request);
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<Map<String, Object>> handleLoginRateLimit(
            RateLimitExceededException ex, HttpServletRequest request) {

        log.warn("Login rate limit exceeded for {} {}", request.getMethod(), request.getRequestURI());
        return buildResponse(HttpStatus.TOO_MANY_REQUESTS, "Too many login attempts. Please try again later.", request);
    }

    @ExceptionHandler(CallNotPermittedException.class)
    public ResponseEntity<Map<String, Object>> handleCircuitBreaker(
            CallNotPermittedException ex, HttpServletRequest request) {

        log.warn(
                "Database circuit breaker OPEN; rejected {} {} circuitBreaker={}",
                request.getMethod(),
                request.getRequestURI(),
                ex.getCausingCircuitBreakerName()
        );
        return buildAuthenticationServiceUnavailableResponse(request);
    }
    @ExceptionHandler(DuplicateUsernameException.class)
    public ResponseEntity<ApiResponse> handleDuplicateUsername(
            DuplicateUsernameException ex) {

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ApiResponse(ex.getMessage()));
    }
    @ExceptionHandler(PasswordMismatchException.class)
    public ResponseEntity<ApiResponse> handlePasswordMismatch(
            PasswordMismatchException ex) {

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleException(Exception ex, HttpServletRequest request) {
        log.error("Unhandled error on {} {}", request.getMethod(), request.getRequestURI(), ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong", request);
    }
}
