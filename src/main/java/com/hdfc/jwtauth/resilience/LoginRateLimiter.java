package com.hdfc.jwtauth.resilience;

import com.hdfc.jwtauth.exceptions.RateLimitExceededException;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LoginRateLimiter {

    private static final Logger log = LoggerFactory.getLogger(LoginRateLimiter.class);

    private final Map<String, RateLimiter> limiters = new ConcurrentHashMap<>();
    private final RateLimiterConfig rateLimiterConfig;

    public LoginRateLimiter(RateLimiterConfig rateLimiterConfig) {
        this.rateLimiterConfig = rateLimiterConfig;
    }

    public void checkRateLimit(String username, String clientIp) {
        String key = username + "|" + clientIp;

        RateLimiter limiter = limiters.computeIfAbsent(
                key,
                ignored -> RateLimiter.of("login-" + key.hashCode(), rateLimiterConfig)
        );

        if (!limiter.acquirePermission()) {
            log.warn("Login rate limit exceeded for {}", key);
            throw new RateLimitExceededException(
                    "Rate limit exceeded for username: " + username + " from IP: " + clientIp);
        }

        log.debug("Login rate limit check passed for {}", key);
    }
}
