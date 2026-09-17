package com.hdfclife.smartauth.resilience;

import com.hdfclife.smartauth.exception.ExternalServiceException;
import com.hdfclife.smartauth.exception.InvalidCredentialsException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LoginCircuitBreakerTest {

    @Test
    void opensAfterExternalFailuresAndRejectsTheNextCall() throws Exception {
        LoginCircuitBreaker breaker = new LoginCircuitBreaker(circuitBreaker(2));
        AtomicInteger externalCalls = new AtomicInteger();

        for (int i = 0; i < 2; i++) {
            assertThrows(ExternalServiceException.class, () ->
                    breaker.executeExternalLogin(() -> {
                        externalCalls.incrementAndGet();
                        throw new ExternalServiceException("service down");
                    }));
        }

        assertEquals(CircuitBreaker.State.OPEN, breaker.getState());
        assertThrows(ExternalServiceException.class, () ->
                breaker.executeExternalLogin(() -> {
                    externalCalls.incrementAndGet();
                    return true;
                }));
        assertEquals(2, externalCalls.get(), "an open circuit must not call the dependency");
    }

    @Test
    void ignoresInvalidCredentialsWhenCalculatingFailureRate() throws Exception {
        LoginCircuitBreaker breaker = new LoginCircuitBreaker(circuitBreaker(2));

        for (int i = 0; i < 2; i++) {
            assertThrows(InvalidCredentialsException.class, () ->
                    breaker.executeExternalLogin(() -> {
                        throw new InvalidCredentialsException("bad credentials");
                    }));
        }

        assertEquals(CircuitBreaker.State.CLOSED, breaker.getState());
    }

    private CircuitBreaker circuitBreaker(int minimumNumberOfCalls) {
        return CircuitBreaker.of("test-login-circuit-" + minimumNumberOfCalls + "-" + System.nanoTime(),
                CircuitBreakerConfig.custom()
                        .failureRateThreshold(50)
                        .minimumNumberOfCalls(minimumNumberOfCalls)
                        .slidingWindowSize(minimumNumberOfCalls)
                        .waitDurationInOpenState(Duration.ofHours(1))
                        .recordExceptions(ExternalServiceException.class)
                        .ignoreExceptions(InvalidCredentialsException.class)
                        .build());
    }
}
