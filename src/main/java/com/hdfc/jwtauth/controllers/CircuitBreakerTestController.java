package com.hdfc.jwtauth.controllers;

import com.hdfc.jwtauth.services.CircuitBreakerTestService;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/circuit-breaker")
public class CircuitBreakerTestController {

    private final CircuitBreakerTestService circuitBreakerTestService;
    private final CircuitBreaker circuitBreaker;
    public CircuitBreakerTestController(
            CircuitBreakerTestService circuitBreakerTestService, CircuitBreaker circuitBreaker
    ) {
        this.circuitBreakerTestService = circuitBreakerTestService;
        this.circuitBreaker = circuitBreaker;
    }

    @GetMapping("/test")
    public ResponseEntity<String> test(
            @RequestParam(defaultValue = "false") boolean fail
    ) {

        return ResponseEntity.ok(
                circuitBreakerTestService.callExternalService(fail)
        );
    }
    @GetMapping("/state")
    public ResponseEntity<String> state() {
        return ResponseEntity.ok(
                circuitBreaker.getState().name()
        );
    }
}