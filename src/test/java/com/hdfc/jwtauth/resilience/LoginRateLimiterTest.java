package com.hdfc.jwtauth.resilience.ratelimiter;

import com.hdfc.jwtauth.controllers.AuthController;
import com.hdfc.jwtauth.entity.User;
import com.hdfc.jwtauth.exceptions.GlobalExceptionHandler;
import com.hdfc.jwtauth.repository.UserRepository;
import com.hdfc.jwtauth.resilience.LoginCircuitBreaker;
import com.hdfc.jwtauth.resilience.LoginFallbackHandler;
import com.hdfc.jwtauth.resilience.LoginRateLimiter;
import com.hdfc.jwtauth.security.CookieService;
import com.hdfc.jwtauth.services.JwtService;
import com.hdfc.jwtauth.security.TokenStore;
import com.hdfc.jwtauth.services.ExternalLoginService;
import com.hdfc.jwtauth.services.LoginAttemptService;
import com.hdfc.jwtauth.services.UserService;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class LoginRateLimiterTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {

        // ============================================================
        // USER
        // ============================================================

        User user = new User(
                "user",
                "encoded-password",
                "USER",
                true
        );


        // ============================================================
        // USER REPOSITORY
        // ============================================================

        UserRepository userRepository =
                mock(UserRepository.class);

        when(userRepository.findByUsername("user"))
                .thenReturn(Optional.of(user));


        // ============================================================
        // PASSWORD ENCODER
        // ============================================================

        PasswordEncoder passwordEncoder =
                mock(PasswordEncoder.class);

        when(passwordEncoder.matches(
                "password",
                "encoded-password"
        )).thenReturn(true);


        // ============================================================
        // USER SERVICE
        //
        // AuthController now uses UserService for signup.
        // Login still uses UserRepository and PasswordEncoder
        // directly, so all three dependencies are provided.
        // ============================================================

        UserService userService =
                new UserService(
                        userRepository,
                        passwordEncoder
                );


        // ============================================================
        // JWT SERVICE
        //
        // Standalone MockMvc does not load Spring @Value properties.
        // Therefore, set the JWT secret manually.
        // ============================================================

        JwtService jwtService =
                new JwtService();

        ReflectionTestUtils.setField(
                jwtService,
                "secret",
                "test-secret-key-must-be-at-least-32-bytes-long-123456"
        );


        // ============================================================
        // COOKIE SERVICE
        // ============================================================

        CookieService cookieService =
                new CookieService();

        ReflectionTestUtils.setField(
                cookieService,
                "sameSite",
                "Lax"
        );

        ReflectionTestUtils.setField(
                cookieService,
                "accessTokenName",
                "ACCESS_TOKEN"
        );

        ReflectionTestUtils.setField(
                cookieService,
                "refreshTokenName",
                "REFRESH_TOKEN"
        );


        // ============================================================
        // LOGIN ATTEMPT SERVICE
        // ============================================================

        LoginAttemptService loginAttemptService =
                new LoginAttemptService();


        // ============================================================
        // EXTERNAL LOGIN SERVICE
        // ============================================================

        ExternalLoginService externalLoginService =
                new ExternalLoginService(
                        new LoginCircuitBreaker(
                                CircuitBreaker.ofDefaults(
                                        "rate-limiter-test-external-login"
                                )
                        ),
                        new LoginFallbackHandler()
                );


        // ============================================================
        // RATE LIMITER
        //
        // Allow 5 requests per minute.
        // Sixth request should return 429.
        // ============================================================

        LoginRateLimiter rateLimiter =
                new LoginRateLimiter(
                        RateLimiterConfig.custom()
                                .limitForPeriod(5)
                                .limitRefreshPeriod(
                                        Duration.ofMinutes(1)
                                )
                                .timeoutDuration(Duration.ZERO)
                                .build()
                );


        // ============================================================
        // AUTH CONTROLLER
        //
        // IMPORTANT:
        // This order exactly matches your current AuthController
        // constructor.
        // ============================================================

        AuthController controller =
                new AuthController(
                        rateLimiter,
                        cookieService,
                        loginAttemptService,
                        jwtService,
                        new TokenStore(),
                        externalLoginService,
                        userRepository,
                        passwordEncoder,
                        userService
                );


        // ============================================================
        // MOCK MVC
        // ============================================================

        this.mockMvc =
                standaloneSetup(controller)
                        .setControllerAdvice(
                                new GlobalExceptionHandler()
                        )
                        .build();
    }


    // ============================================================
    // TEST 1
    // Request within rate limit should succeed
    // ============================================================

    @Test
    void shouldAllowRequestWithinRateLimit()
            throws Exception {

        mockMvc.perform(
                        post("/api/v1/auth/login")
                                .contentType("application/json")
                                .content(
                                        """
                                        {
                                            "username": "user",
                                            "password": "password"
                                        }
                                        """
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.username")
                                .value("user")
                );
    }


    // ============================================================
    // TEST 2
    // Sixth request should be rejected
    // ============================================================

    @Test
    void shouldReturnRateLimitResponseWhenLimitIsExceeded()
            throws Exception {

        String requestBody =
                """
                {
                    "username": "user",
                    "password": "password"
                }
                """;


        // --------------------------------------------------------
        // Requests 1 to 5 should succeed.
        // --------------------------------------------------------

        for (int attempt = 0; attempt < 5; attempt++) {

            mockMvc.perform(
                            post("/api/v1/auth/login")
                                    .contentType("application/json")
                                    .content(requestBody)
                    )
                    .andExpect(status().isOk());
        }


        // --------------------------------------------------------
        // Request 6 should exceed the rate limit.
        // --------------------------------------------------------

        mockMvc.perform(
                        post("/api/v1/auth/login")
                                .contentType("application/json")
                                .content(requestBody)
                )
                .andExpect(status().isTooManyRequests())
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Too many login attempts. Please try again later."
                                )
                );
    }


    // ============================================================
    // TEST 3
    // Invalid password should remain 401
    // ============================================================

    @Test
    void shouldKeepInvalidPasswordAsUnauthorized()
            throws Exception {

        mockMvc.perform(
                        post("/api/v1/auth/login")
                                .contentType("application/json")
                                .content(
                                        """
                                        {
                                            "username": "user",
                                            "password": "wrong"
                                        }
                                        """
                                )
                )
                .andExpect(status().isUnauthorized())
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Invalid username or password"
                                )
                );
    }
}