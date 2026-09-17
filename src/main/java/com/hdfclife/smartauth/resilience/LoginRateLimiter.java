package com.hdfclife.smartauth.resilience;

import com.hdfclife.smartauth.exception.RateLimitExceededException;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LoginRateLimiter {

    private static final Logger log = LoggerFactory.getLogger(LoginRateLimiter.class);

    private final Map<String, LimiterState> limiters = new ConcurrentHashMap<>();
    private final RateLimiterConfig rateLimiterConfig;

    public LoginRateLimiter(RateLimiterConfig rateLimiterConfig) {
        this.rateLimiterConfig = rateLimiterConfig;
    }

    public void checkRateLimit(String username, String clientIp) {
        String key = username + "|" + clientIp;

        LimiterState state = limiters.computeIfAbsent(
                key,
                ignored -> new LimiterState(RateLimiter.of("login-" + key.hashCode(), rateLimiterConfig))
        );
        state.touch();

        if (!state.limiter().acquirePermission()) {
            log.warn("Login rate limit exceeded for {}", key);
            throw new RateLimitExceededException(
                    "Rate limit exceeded for username: " + username + " from IP: " + clientIp);
        }

        log.debug("Login rate limit check passed for {}", key);
    }

    @Scheduled(fixedDelayString = "${login.rate-limit.cleanup-interval-ms:600000}")
    void removeInactiveLimiters() {
        Instant cutoff = Instant.now().minus(rateLimiterConfig.getLimitRefreshPeriod().multipliedBy(15));
        limiters.entrySet().removeIf(entry -> entry.getValue().lastUsed().isBefore(cutoff));
    }

    private static final class LimiterState {
        private final RateLimiter limiter;
        private volatile Instant lastUsed = Instant.now();

        private LimiterState(RateLimiter limiter) {
            this.limiter = limiter;
        }

        private RateLimiter limiter() {
            return limiter;
        }

        private Instant lastUsed() {
            return lastUsed;
        }

        private void touch() {
            lastUsed = Instant.now();
        }
    }
}
