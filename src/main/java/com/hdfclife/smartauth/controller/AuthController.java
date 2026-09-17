package com.hdfclife.smartauth.controller;

import com.hdfclife.smartauth.repository.InMemoryUserRepository;
import com.hdfclife.smartauth.service.ExternalLoginService;
import com.hdfclife.smartauth.security.JwtTokenProvider;
import com.hdfclife.smartauth.repository.InMemoryTokenRepository;
import com.hdfclife.smartauth.service.LoginAttemptService;
import com.hdfclife.smartauth.exception.InvalidCredentialsException;
import com.hdfclife.smartauth.resilience.LoginRateLimiter;
import com.hdfclife.smartauth.dto.response.ApiResponse;
import com.hdfclife.smartauth.dto.response.ErrorResponse;
import com.hdfclife.smartauth.dto.request.LoginRequest;
import com.hdfclife.smartauth.dto.response.LoginResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.UUID;

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
            @Valid @RequestBody LoginRequest request,
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
                    .body(ErrorResponse.of(HttpStatus.LOCKED,
                            "Account is temporarily locked. Try again later.", servletRequest));
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
                        .body(ErrorResponse.of(HttpStatus.LOCKED,
                                "Too many failed login attempts. Account locked for 5 minutes.", servletRequest));
            }

            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(ErrorResponse.of(HttpStatus.UNAUTHORIZED,
                            "Invalid username or password", servletRequest));
        }

        // 5. Successful login → reset failed attempts
        loginAttemptService.loginSucceeded(username);

        // 6. Generate tokens
        String sessionId = UUID.randomUUID().toString();

        String accessToken =
                jwtService.generateAccessToken(user, sessionId);

        String refreshToken =
                jwtService.generateRefreshToken(user, sessionId);

        // 7. Store tokens
        tokenStore.save(
                accessToken,
                user.username(),
                sessionId
        );

        tokenStore.save(
                refreshToken,
                user.username(),
                sessionId
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
            ) String authorization,
            HttpServletRequest servletRequest) {

        if (authorization == null ||
                !authorization.startsWith("Bearer ")) {

            return ResponseEntity
                    .badRequest()
                    .body(ErrorResponse.of(HttpStatus.BAD_REQUEST,
                            "Bearer refresh token is required", servletRequest));
        }

        String oldRefreshToken = authorization.substring(7);

        try {

            // 1. Check token exists in active InMemoryTokenRepository
            if (!tokenStore.isActive(oldRefreshToken)) {

                return ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED)
                        .body(ErrorResponse.of(HttpStatus.UNAUTHORIZED,
                                "Refresh token is invalid or already used", servletRequest));
            }

            // 2. Make sure it is actually a refresh token
            if (!jwtService.isRefreshToken(oldRefreshToken)) {

                return ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED)
                        .body(ErrorResponse.of(HttpStatus.UNAUTHORIZED,
                                "Access token cannot be used as refresh token", servletRequest));
            }

            // 3. Get username from old refresh token
            String username =
                    jwtService.username(oldRefreshToken);

            var user = users.find(username);

            if (user == null) {

                return ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED)
                        .body(ErrorResponse.of(HttpStatus.UNAUTHORIZED,
                                "User not found", servletRequest));
            }

            // 4. IMPORTANT:
            // Revoke old refresh token
            tokenStore.remove(oldRefreshToken);

            // 5. Generate NEW access token
            String sessionId = jwtService.sessionId(oldRefreshToken);
            if (sessionId == null) {
                sessionId = UUID.randomUUID().toString();
            }

            String newAccessToken =
                    jwtService.generateAccessToken(user, sessionId);

            // 6. Generate NEW refresh token
            String newRefreshToken =
                    jwtService.generateRefreshToken(user, sessionId);

            // 7. Store both new tokens
            tokenStore.save(
                    newAccessToken,
                    user.username(),
                    sessionId
            );

            tokenStore.save(
                    newRefreshToken,
                    user.username(),
                    sessionId
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
                    .body(ErrorResponse.of(HttpStatus.UNAUTHORIZED,
                            "Invalid or expired refresh token", servletRequest));
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
        String sessionId = tokenStore.sessionId(token);
        if (sessionId != null) {
            tokenStore.removeSession(sessionId);
            log.info("All tokens removed from active session store");
        } else {
            tokenStore.remove(token);
            log.info("Token removed from active session store");
        }

        return ResponseEntity.ok(new ApiResponse("Logout successful"));
    }
}
