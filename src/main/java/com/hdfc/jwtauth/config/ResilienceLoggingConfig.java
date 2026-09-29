package com.hdfc.jwtauth.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ResilienceLoggingConfig {

    private static final String DATABASE_CIRCUIT_BREAKER = "databaseCircuitBreakerCore";
    private static final Logger log = LoggerFactory.getLogger(ResilienceLoggingConfig.class);

    @Bean
    public CircuitBreaker databaseCircuitBreaker(CircuitBreakerRegistry registry) {
        CircuitBreaker circuitBreaker = registry.circuitBreaker(DATABASE_CIRCUIT_BREAKER);

        circuitBreaker.getEventPublisher()
                .onStateTransition(event -> log.info(
                        "Database circuit breaker state transition: {} -> {}",
                        event.getStateTransition().getFromState(),
                        event.getStateTransition().getToState()))
                .onError(event -> log.warn(
                        "Database circuit breaker recorded failure: {}",
                        event.getThrowable().getMessage()))
                .onSuccess(event -> log.debug("Database circuit breaker success"))
                .onCallNotPermitted(event -> log.warn(
                        "Database circuit breaker is OPEN; database call rejected"))
                .onReset(event -> log.info("Database circuit breaker reset"));

        return circuitBreaker;
    }
}
