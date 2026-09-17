package com.hdfclife.smartauth.controller;

import com.hdfclife.smartauth.repository.InMemoryUserRepository;
import com.hdfclife.smartauth.service.ExternalLoginService;
import com.hdfclife.smartauth.security.JwtTokenProvider;
import com.hdfclife.smartauth.repository.InMemoryTokenRepository;
import com.hdfclife.smartauth.service.LoginAttemptService;
import com.hdfclife.smartauth.exception.InvalidCredentialsException;
import com.hdfclife.smartauth.resilience.LoginRateLimiter;
import com.hdfclife.smartauth.dto.response.ApiResponse;
import com.hdfclife.smartauth.dto.request.LoginRequest;
import com.hdfclife.smartauth.dto.response.LoginResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
public class AuthController {
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final InMemoryUserRepository users;
    private final LoginAttemptService loginAttemptService;
    private final LoginRateLimiter loginRateLimiter;
    private final ExternalLoginService externalLoginService;
    private final JwtTokenProvider jwtService;
    private final InMemoryTokenRepository tokenStore;

    public AuthController(
            InMemoryUserRepository users,
            LoginAttemptService loginAttemptService,
            LoginRateLimiter loginRateLimiter,
            ExternalLoginService externalLoginService,
            JwtTokenProvider jwtService,
            InMemoryTokenRepository tokenStore) {
        this.users = users;
        this.loginAttemptService = loginAttemptService;
        this.loginRateLimiter = loginRateLimiter;
        this.externalLoginService = externalLoginService;
        this.jwtService = jwtService;
        this.tokenStore = tokenStore;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestBody LoginRequest request,
            HttpServletRequest servletRequest) {

        String username = request.username();

        // 1. Check whether account is locked
        if (loginAttemptService.isLocked(username)) {

            log.warn(
                    "Login blocked because account is locked: username={}",
                    username
            );

            return ResponseEntity
                    .status(HttpStatus.LOCKED)
                    .body(new ApiResponse(
                            "Account is temporarily locked. Try again later."
                ));
        }

        // 2. Apply request rate limiting before credential validation
        loginRateLimiter.checkRateLimit(username, clientIp(servletRequest));

        // 3. Find user
        var user = users.find(username);

        // 4. Validate credentials through the resilient mock external login service
        try {
            externalLoginService.validateExternalLogin(
                    username,
                    request.password(),
                    () -> user != null && user.password().equals(request.password())
            );
        } catch (InvalidCredentialsException ex) {

            loginAttemptService.loginFailed(username);

            int failedAttempts =
                    loginAttemptService.getFailedAttempts(username);

            log.warn(
                    "Login failed: username={}, failedAttempts={}",
                    username,
                    failedAttempts
            );

            // Account became locked after this attempt
            if (loginAttemptService.isLocked(username)) {

                log.warn(
                        "Account locked after too many failed attempts: username={}",
                        username
                );

                return ResponseEntity
                        .status(HttpStatus.LOCKED)
                        .body(new ApiResponse(
                                "Too many failed login attempts. Account locked for 5 minutes."
                        ));
            }

            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse(
                            "Invalid username or password"
                    ));
        }

        // 5. Successful login → reset failed attempts
        loginAttemptService.loginSucceeded(username);

        // 6. Generate tokens
        String accessToken =
                jwtService.generateAccessToken(user);

        String refreshToken =
                jwtService.generateRefreshToken(user);

        // 7. Store tokens
        tokenStore.save(
                accessToken,
                user.username()
        );

        tokenStore.save(
                refreshToken,
                user.username()
        );

        log.info(
                "Login successful for username={}",
                username
        );

        // 8. Return response
        return ResponseEntity.ok(
                new LoginResponse(
                        "Login successful",
                        accessToken,
                        refreshToken,
                        user.username(),
                        user.roles()
                )
        );
    }

    private String clientIp(HttpServletRequest request) {
        // Use the peer address unless a trusted proxy is explicitly configured.
        // Accepting X-Forwarded-For here would let a caller bypass the limiter by
        // supplying a different header value on every request.
        return request.getRemoteAddr();
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(
            @RequestHeader(
                    value = "Authorization",
                    required = false
            ) String authorization) {

        if (authorization == null ||
                !authorization.startsWith("Bearer ")) {

            return ResponseEntity
                    .badRequest()
                    .body(new ApiResponse(
                            "Bearer refresh token is required"
                    ));
        }

        String oldRefreshToken = authorization.substring(7);

        try {

            // 1. Check token exists in active InMemoryTokenRepository
            if (!tokenStore.isActive(oldRefreshToken)) {

                return ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED)
                        .body(new ApiResponse(
                                "Refresh token is invalid or already used"
                        ));
            }

            // 2. Make sure it is actually a refresh token
            if (!jwtService.isRefreshToken(oldRefreshToken)) {

                return ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED)
                        .body(new ApiResponse(
                                "Access token cannot be used as refresh token"
                        ));
            }

            // 3. Get username from old refresh token
            String username =
                    jwtService.username(oldRefreshToken);

            var user = users.find(username);

            if (user == null) {

                return ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED)
                        .body(new ApiResponse(
                                "User not found"
                        ));
            }

            // 4. IMPORTANT:
            // Revoke old refresh token
            tokenStore.remove(oldRefreshToken);

            // 5. Generate NEW access token
            String newAccessToken =
                    jwtService.generateAccessToken(user);

            // 6. Generate NEW refresh token
            String newRefreshToken =
                    jwtService.generateRefreshToken(user);

            // 7. Store both new tokens
            tokenStore.save(
                    newAccessToken,
                    user.username()
            );

            tokenStore.save(
                    newRefreshToken,
                    user.username()
            );

            log.info(
                    "Refresh token rotated for username={}",
                    username
            );

            // 8. Return new token pair
            return ResponseEntity.ok(
                    new LoginResponse(
                            "Token refreshed successfully",
                            newAccessToken,
                            newRefreshToken,
                            user.username(),
                            user.roles()
                    )
            );

        } catch (Exception ex) {

            log.warn(
                    "Refresh token validation failed"
            );

            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse(
                            "Invalid or expired refresh token"
                    ));
        }
    }
    @GetMapping("/auth")
    public ResponseEntity<?> auth(Authentication authentication) {
        return ResponseEntity.ok(java.util.Map.of(
            "authenticated", true,
            "username", authentication.getName(),
            "roles", authentication.getAuthorities().stream()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .toList()
        ));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return ResponseEntity.badRequest()
                .body(new ApiResponse("Bearer token is required"));
        }

        String token = authorization.substring(7);
        tokenStore.remove(token);
        log.info("Token removed from active session store");

        return ResponseEntity.ok(new ApiResponse("Logout successful"));
    }
}
