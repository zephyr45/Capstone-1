package com.hdfc.jwtauth.resilience;

import com.hdfc.jwtauth.exceptions.RateLimitExceededException;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@ExtendWith(OutputCaptureExtension.class)
class LoginRateLimitGuardTest {

    private static final int LOGIN_LIMIT_PER_MINUTE = 20;

    @Autowired
    private LoginRateLimitGuard guard;

    @Autowired
    private RateLimiterRegistry rateLimiterRegistry;

    @BeforeEach
    void clearRateLimiters() {
        rateLimiterRegistry.getAllRateLimiters().stream()
                .map(RateLimiter::getName)
                .toList()
                .forEach(rateLimiterRegistry::remove);
    }

    @Test
    void guardIsSpringProxied() {
        assertTrue(AopUtils.isAopProxy(guard));
    }

    @Test
    void limitsOneIpAcrossRotatingUsernames() {
        String ip = "192.0.2.10";

        for (int attempt = 0; attempt < LOGIN_LIMIT_PER_MINUTE; attempt++) {
            String username = "user-" + attempt;
            assertDoesNotThrow(() -> guard.checkIp(ip));
            assertDoesNotThrow(() -> guard.checkUsername(username));
        }

        RateLimitExceededException exception = assertThrows(
                RateLimitExceededException.class,
                () -> guard.checkIp(ip));
        assertEquals("Too many login attempts. Please try again later.", exception.getMessage());
    }

    @Test
    void limitsOneNormalizedUsernameAcrossRotatingIps() {
        String[] usernameVariants = {
                " Alice ", "alice", "ALICE", "aLiCe", " alice", "ALIce ", "aLICE"
        };

        for (int attempt = 0; attempt < LOGIN_LIMIT_PER_MINUTE; attempt++) {
            String ip = "198.51.100." + attempt;
            assertDoesNotThrow(() -> guard.checkIp(ip));
            String username = usernameVariants[attempt % usernameVariants.length];
            assertDoesNotThrow(() -> guard.checkUsername(username));
        }

        assertDoesNotThrow(() -> guard.checkIp("198.51.100.99"));
        assertThrows(RateLimitExceededException.class,
                () -> guard.checkUsername("alice"));
    }

    @Test
    void logsSuccessfulAndRejectedRateLimitChecks(CapturedOutput output) {
        String ip = "192.0.2.25";

        for (int attempt = 0; attempt < LOGIN_LIMIT_PER_MINUTE; attempt++) {
            guard.checkIp(ip);
        }
        guard.checkUsername("Alice");
        assertThrows(RateLimitExceededException.class, () -> guard.checkIp(ip));

        assertTrue(output.getAll().contains(
                "Login IP rate limit check passed for ip=192.0.2.25"));
        assertTrue(output.getAll().contains(
                "Login username rate limit check passed for username=alice"));
        assertTrue(output.getAll().contains(
                "Login IP rate limit exceeded for ip=192.0.2.25"));
    }

    @Test
    void usesConfiguredDefaultsForDynamicLimiterNames() {
        guard.checkIp("203.0.113.7");
        guard.checkUsername("Bob");

        assertLimiterConfiguration(findRateLimiter("login-ip-203.0.113.7"));
        assertLimiterConfiguration(findRateLimiter("login-user-bob"));
    }

    @Test
    void mapsMissingIdentityValuesToStableKeys() {
        guard.checkIp(null);
        guard.checkUsername(null);

        findRateLimiter("login-ip-<unknown>");
        findRateLimiter("login-user-<missing>");
    }

    private RateLimiter findRateLimiter(String name) {
        return rateLimiterRegistry.getAllRateLimiters().stream()
                .filter(rateLimiter -> rateLimiter.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing rate limiter " + name));
    }

    private void assertLimiterConfiguration(RateLimiter rateLimiter) {
        assertEquals(LOGIN_LIMIT_PER_MINUTE,
                rateLimiter.getRateLimiterConfig().getLimitForPeriod());
        assertEquals(Duration.ofMinutes(1),
                rateLimiter.getRateLimiterConfig().getLimitRefreshPeriod());
        assertEquals(Duration.ZERO, rateLimiter.getRateLimiterConfig().getTimeoutDuration());
    }
}
