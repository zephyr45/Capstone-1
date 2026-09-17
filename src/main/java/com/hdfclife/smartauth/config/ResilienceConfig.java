package com.hdfclife.smartauth.config;

import com.hdfclife.smartauth.exception.ExternalServiceException;
import com.hdfclife.smartauth.exception.InvalidCredentialsException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ResilienceConfig {

    private static final Logger log = LoggerFactory.getLogger(ResilienceConfig.class);

    private final RateLimitProperties rateLimitProperties;
    private final CircuitBreakerProperties circuitBreakerProperties;

    public ResilienceConfig(
            RateLimitProperties rateLimitProperties,
            CircuitBreakerProperties circuitBreakerProperties) {
        this.rateLimitProperties = rateLimitProperties;
        this.circuitBreakerProperties = circuitBreakerProperties;
    }

    @Bean
    public CircuitBreaker externalLoginCircuitBreaker() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(circuitBreakerProperties.getFailureRateThreshold())
                .slowCallRateThreshold(circuitBreakerProperties.getSlowCallRateThreshold())
                .waitDurationInOpenState(circuitBreakerProperties.getWaitDurationInOpenState())
                .slowCallDurationThreshold(circuitBreakerProperties.getSlowCallDurationThreshold())
                .permittedNumberOfCallsInHalfOpenState(
                        circuitBreakerProperties.getPermittedNumberOfCallsInHalfOpenState())
                .minimumNumberOfCalls(circuitBreakerProperties.getMinimumNumberOfCalls())
                .slidingWindowSize(circuitBreakerProperties.getSlidingWindowSize())
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .recordExceptions(ExternalServiceException.class)
                .ignoreExceptions(InvalidCredentialsException.class)
                .build();

        CircuitBreaker circuitBreaker = CircuitBreaker.of("externalLoginCircuitBreaker", config);

        circuitBreaker.getEventPublisher()
                .onStateTransition(event -> log.info(
                        "Login circuit breaker state transition: {} -> {}",
                        event.getStateTransition().getFromState(),
                        event.getStateTransition().getToState()))
                .onError(event -> log.warn(
                        "Login circuit breaker recorded failure: {}",
                        event.getThrowable().getMessage()))
                .onSuccess(event -> log.debug("Login circuit breaker success"));

        return circuitBreaker;
    }

    @Bean
    public RateLimiterConfig loginRateLimiterConfig() {
        return RateLimiterConfig.custom()
                .limitForPeriod(rateLimitProperties.getLimitForPeriod())
                .limitRefreshPeriod(rateLimitProperties.getLimitRefreshPeriod())
                .timeoutDuration(rateLimitProperties.getTimeoutDuration())
                .build();
    }
}
