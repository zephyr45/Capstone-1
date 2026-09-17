package com.hdfclife.smartauth.config;

import com.hdfclife.smartauth.exception.ExternalServiceException;
import com.hdfclife.smartauth.exception.InvalidCredentialsException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@EnableConfigurationProperties(RateLimitProperties.class)
public class ResilienceConfig {

    private static final Logger logger =
            LoggerFactory.getLogger(ResilienceConfig.class);

    @Bean
    public CircuitBreaker externalLoginCircuitBreaker() {

        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(50.0f)
                .slowCallRateThreshold(100.0f)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .slowCallDurationThreshold(Duration.ofSeconds(2))
                .permittedNumberOfCallsInHalfOpenState(3)
                .minimumNumberOfCalls(5)
                .slidingWindowSize(10)
                .slidingWindowType(
                        CircuitBreakerConfig.SlidingWindowType.COUNT_BASED
                )
                .recordExceptions(ExternalServiceException.class)
                .ignoreExceptions(InvalidCredentialsException.class)
                .build();

        CircuitBreaker circuitBreaker =
                CircuitBreaker.of("login-circuit-breaker", config);

        circuitBreaker.getEventPublisher()
                .onStateTransition(event ->
                        logger.info(
                                "Circuit breaker state transition: {} -> {}",
                                event.getStateTransition().getFromState(),
                                event.getStateTransition().getToState()
                        ))
                .onError(event ->
                        logger.warn(
                                "Circuit breaker recorded failure: {}",
                                event.getThrowable().getMessage()
                        ))
                .onSuccess(event ->
                        logger.debug("Circuit breaker success"));

        return circuitBreaker;
    }

    @Bean
    public RateLimiterConfig loginRateLimiterConfig(
            RateLimitProperties properties) {

        return RateLimiterConfig.custom()
                .limitForPeriod(properties.getLimitForPeriod())
                .limitRefreshPeriod(properties.getLimitRefreshPeriod())
                .timeoutDuration(properties.getTimeoutDuration())
                .build();
    }
}
