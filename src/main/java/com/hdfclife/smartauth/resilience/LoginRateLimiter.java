package com.hdfclife.smartauth.resilience;

import com.hdfclife.smartauth.exception.RateLimitExceededException;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LoginRateLimiter {

    private static final Logger logger =
            LoggerFactory.getLogger(LoginRateLimiter.class);

    private final Map<String, RateLimiter> limiters;

    private final RateLimiterConfig rateLimiterConfig;

    public LoginRateLimiter(RateLimiterConfig rateLimiterConfig) {
        this.limiters = new ConcurrentHashMap<>();
        this.rateLimiterConfig = rateLimiterConfig;
    }

    public void checkRateLimit(String username, String clientIp) {

        String key = createKey(username, clientIp);

        RateLimiter limiter = limiters.computeIfAbsent(
                key,
                keyFromMap -> createRateLimiter()
        );

        boolean allowed = limiter.acquirePermission();

        if (!allowed) {
            logger.warn("Rate limit exceeded for: {}", key);

            throw new RateLimitExceededException(
                    "Rate limit exceeded for username: "
                            + username
                            + " from IP: "
                            + clientIp
            );
        }

        logger.debug("Rate limit check passed for: {}", key);
    }

    private RateLimiter createRateLimiter() {

        logger.debug("Creating new rate limiter");

        return RateLimiter.of(
                "login-" + System.nanoTime(),
                rateLimiterConfig
        );
    }

    private String createKey(String username, String clientIp) {
        return username + "|" + clientIp;
    }
}
