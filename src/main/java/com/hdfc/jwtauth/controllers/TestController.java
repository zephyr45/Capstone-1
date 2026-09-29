package com.hdfc.jwtauth.controllers;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/test")
public class TestController {
    private static final String DATABASE_CIRCUIT_BREAKER = "databaseCircuitBreakerCore";

    private final CircuitBreakerRegistry circuitBreakerRegistry;

    public TestController(CircuitBreakerRegistry circuitBreakerRegistry) {
        this.circuitBreakerRegistry = circuitBreakerRegistry;
    }

    @GetMapping("/circuit-state")
    public ResponseEntity<?> circuitState() {
        return ResponseEntity.ok(
                Map.of(
                        "circuitBreaker",
                        circuitBreakerRegistry
                                .circuitBreaker(DATABASE_CIRCUIT_BREAKER)
                                .getState()
                                .name()
                )
        );
    }
}
