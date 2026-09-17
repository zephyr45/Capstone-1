package com.hdfc.jwtauth.resilience;

import com.hdfc.jwtauth.exceptions.RateLimitExceededException;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LoginRateLimiterTest {

    @Test
    void limitsEachUsernameAndIpPairIndependently() {
        LoginRateLimiter limiter = new LoginRateLimiter(
                RateLimiterConfig.custom()
                        .limitForPeriod(2)
                        .limitRefreshPeriod(Duration.ofHours(1))
                        .timeoutDuration(Duration.ZERO)
                        .build());

        assertDoesNotThrow(() -> limiter.checkRateLimit("alice", "192.0.2.10"));
        assertDoesNotThrow(() -> limiter.checkRateLimit("alice", "192.0.2.10"));
        assertThrows(RateLimitExceededException.class,
                () -> limiter.checkRateLimit("alice", "192.0.2.10"));

        assertDoesNotThrow(() -> limiter.checkRateLimit("alice", "192.0.2.11"));
        assertDoesNotThrow(() -> limiter.checkRateLimit("bob", "192.0.2.10"));
    }
}
