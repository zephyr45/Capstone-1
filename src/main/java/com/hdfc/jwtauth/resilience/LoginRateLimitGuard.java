package com.hdfc.jwtauth.resilience;

import com.hdfc.jwtauth.exceptions.RateLimitExceededException;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LoginRateLimitGuard {

    private static final Logger log = LoggerFactory.getLogger(LoginRateLimitGuard.class);
    private static final String RATE_LIMIT_MESSAGE =
            "Too many login attempts. Please try again later.";
    private final LoginRateLimitKeyResolver keyResolver;

    public LoginRateLimitGuard(LoginRateLimitKeyResolver keyResolver) {
        this.keyResolver = keyResolver;
    }

    @RateLimiter(
            name = "@loginRateLimitKeyResolver.forIp(#p0)",
            fallbackMethod = "ipRateLimitFallback"
    )
    public void checkIp(String ip) {
        log.debug("Login IP rate limit check passed for ip={}", keyResolver.normalizeIp(ip));
    }

    @RateLimiter(
            name = "@loginRateLimitKeyResolver.forUsername(#p0)",
            fallbackMethod = "usernameRateLimitFallback"
    )
    public void checkUsername(String username) {
        log.debug(
                "Login username rate limit check passed for username={}",
                keyResolver.normalizeUsername(username)
        );
    }

    private void ipRateLimitFallback(String ip, RequestNotPermitted exception) {
        log.warn("Login IP rate limit exceeded for ip={}", keyResolver.normalizeIp(ip));
        throw new RateLimitExceededException(RATE_LIMIT_MESSAGE, exception);
    }

    private void usernameRateLimitFallback(String username, RequestNotPermitted exception) {
        log.warn(
                "Login username rate limit exceeded for username={}",
                keyResolver.normalizeUsername(username)
        );
        throw new RateLimitExceededException(RATE_LIMIT_MESSAGE, exception);
    }
}
