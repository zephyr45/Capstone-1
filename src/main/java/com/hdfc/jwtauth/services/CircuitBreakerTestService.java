package com.hdfc.jwtauth.services;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.stereotype.Service;

@Service
public class CircuitBreakerTestService {

    private final MockExternalService mockExternalService;

    public CircuitBreakerTestService(
            MockExternalService mockExternalService
    ) {
        this.mockExternalService = mockExternalService;
    }

    @CircuitBreaker(
            name = "externalLoginCircuitBreaker",
            fallbackMethod = "fallback"
    )
    public String callExternalService(boolean shouldFail) {

        return mockExternalService.callExternalService(
                shouldFail
        );
    }

    public String fallback(
            boolean shouldFail,
            Throwable throwable
    ) {

        return "FALLBACK: External service is currently unavailable";
    }
}