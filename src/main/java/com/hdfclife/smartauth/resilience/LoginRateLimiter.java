package com.hdfclife.smartauth.resilience;

import com.hdfclife.smartauth.exception.RateLimitExceededException;
import io.github.resilience4j.ratelimiter.RateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LoginRateLimiter {

    private static final Logger logger =
            LoggerFactory.getLogger(LoginRateLimiter.class);

    /*
     * Each username + IP combination gets its own RateLimiter.
     *
     * Example:
     *
     * "alice|10.0.0.1" -> RateLimiter A
     * "alice|10.0.0.2" -> RateLimiter B
     * "bob|10.0.0.1"   -> RateLimiter C
     *
     * ConcurrentHashMap is used because multiple login requests
     * can access this map at the same time.
     */
    private final Map<String, RateLimiter> limiters;

    /*
     * Contains the common RateLimiter configuration:
     * 5 attempts / minute, timeout = 0.
     *
     * Spring injects this configured Resilience4j RateLimiter.
     *
     * We use it as a template to obtain the common configuration
     * when creating separate RateLimiters for each username + IP.
     *
     * Each newly created RateLimiter has the same configuration,
     * but maintains its own independent permission state.
     */
    private final RateLimiter rateLimiterTemplate;

    public LoginRateLimiter(RateLimiter rateLimiterTemplate) {
        this.limiters = new ConcurrentHashMap<>();
        this.rateLimiterTemplate = rateLimiterTemplate;
    }

    /*
     * Checks whether this username + IP combination
     * is allowed to make another login attempt.
     */
    public void checkRateLimit(String username, String clientIp) {

        // Create the unique key for this user and IP.
        String key = createKey(username, clientIp);

        /*
         * Find the RateLimiter for this key.
         *
         * If the key exists:
         *     return the existing RateLimiter.
         *     The lambda is NOT executed.
         *
         * If the key does not exist:
         *     execute the lambda, create a new RateLimiter,
         *     store it in the map, and return it.
         *
         * computeIfAbsent() requires a function that accepts
         * the map's key. We don't need the key to create the
         * RateLimiter, so keyFromMap is not used.
         */
        RateLimiter limiter = limiters.computeIfAbsent(
                key,
                keyFromMap -> createRateLimiter()
        );

        /*
         * Ask the RateLimiter for permission.
         *
         * true  -> permission granted
         * false -> no permission available
         */
        boolean allowed = limiter.acquirePermission();

        if (!allowed) {
            logger.warn("Rate limit exceeded for: {}", key);

            // Our application converts this exception to HTTP 429.
            throw new RateLimitExceededException(
                    "Rate limit exceeded for username: "
                            + username
                            + " from IP: "
                            + clientIp
            );
        }

        logger.debug("Rate limit check passed for: {}", key);
    }

    /*
     * Creates a new Resilience4j RateLimiter using the common
     * configuration from the injected template.
     */
    private RateLimiter createRateLimiter() {
        logger.debug("Creating new rate limiter");

        return RateLimiter.of(
                "login-" + System.nanoTime(),
                rateLimiterTemplate.getRateLimiterConfig()
        );
    }

    /*
     * The username + IP pair is the identity of a rate-limit bucket.
     */
    private String createKey(String username, String clientIp) {
        return username + "|" + clientIp;
    }
}
