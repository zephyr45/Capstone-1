package com.hdfc.jwtauth.logging;

import com.hdfc.jwtauth.config.ResilienceLoggingConfig;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(OutputCaptureExtension.class)
class CircuitBreakerLoggingTest {

    @Test
    void logsFailuresStateChangesRejectedCallsAndResets(CapturedOutput output) {
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .minimumNumberOfCalls(1)
                .slidingWindowSize(1)
                .build();
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(circuitBreakerConfig);
        CircuitBreaker circuitBreaker =
                new ResilienceLoggingConfig().databaseCircuitBreaker(registry);

        assertThrows(
                IllegalStateException.class,
                () -> circuitBreaker.executeRunnable(() -> {
                    throw new IllegalStateException("database down");
                })
        );
        assertThrows(
                CallNotPermittedException.class,
                () -> circuitBreaker.executeRunnable(() -> {
                })
        );
        circuitBreaker.reset();

        assertThat(output.getAll())
                .contains("Database circuit breaker recorded failure: database down")
                .contains("Database circuit breaker state transition: CLOSED -> OPEN")
                .contains("Database circuit breaker is OPEN; database call rejected")
                .contains("Database circuit breaker reset");
    }
}
