package com.hdfc.jwtauth.controllers;
import com.hdfc.jwtauth.services.UserService;
import io.swagger.v3.oas.annotations.*;
import com.hdfc.jwtauth.entity.User;
import com.hdfc.jwtauth.exceptions.InvalidCredentialsException;
import com.hdfc.jwtauth.repository.UserRepository;
import com.hdfc.jwtauth.resilience.LoginRateLimiter;
import com.hdfc.jwtauth.security.CookieService;
import com.hdfc.jwtauth.security.TokenStore;
import com.hdfc.jwtauth.web.ApiResponse;
import com.hdfc.jwtauth.web.LoginRequest;
import com.hdfc.jwtauth.web.LoginResponse;
import com.hdfc.jwtauth.web.RegisterRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;


import com.hdfc.jwtauth.services.JwtService;
import com.hdfc.jwtauth.services.ExternalLoginService;
import com.hdfc.jwtauth.services.LoginAttemptService;

import java.util.Set;
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Endpoints for user signup, authentication, token refresh, and session management")
public class AuthController {

    private static final Logger log =
            LoggerFactory.getLogger(AuthController.class);

    private final LoginRateLimiter loginRateLimiter;
    private final CookieService cookieService;
    private final LoginAttemptService loginAttemptService;
    private final JwtService jwtService;
    private final TokenStore tokenStore;
    private final ExternalLoginService externalLoginService;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserService userService;


    public AuthController(
            LoginRateLimiter loginRateLimiter,
            CookieService cookieService,
            LoginAttemptService loginAttemptService,
            JwtService jwtService,
            TokenStore tokenStore,
            ExternalLoginService externalLoginService,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            UserService userService
    ) {

        this.loginRateLimiter = loginRateLimiter;
        this.cookieService = cookieService;
        this.loginAttemptService = loginAttemptService;
        this.jwtService = jwtService;
        this.tokenStore = tokenStore;
        this.externalLoginService = externalLoginService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.userService=userService;
    }


    // ============================================================
    // EXTRACT COOKIE
    // ============================================================

    private String extractCookie(
            HttpServletRequest request,
            String cookieName
    ) {

        Cookie[] cookies = request.getCookies();

        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {

            if (cookieName.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }

        return null;
    }


    // ============================================================
    // SIGNUP
    // ============================================================

    @PostMapping("/signup")
    public ResponseEntity<?> signup(
            @Valid @RequestBody RegisterRequest request
    ) {

        userService.register(request);

        log.info(
                "SIGNUP_SUCCESS username={}",
                request.username()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new ApiResponse(
                        "Account created successfully"
                ));
    }
    @PostMapping("/login")
    @Operation(
            summary = "Authenticate user",
            description = "Authenticates user credentials, sets access/refresh HTTP-only cookies, and tracks failed attempts / rate limits."
    )
    public ResponseEntity<?> login(
            @RequestBody LoginRequest request,
            HttpServletRequest httpRequest
    ) {

        String username = request.username();
        String ip = httpRequest.getRemoteAddr();


        // --------------------------------
        // RESILIENCE4J RATE LIMIT CHECK
        // --------------------------------

        loginRateLimiter.checkRateLimit(username, ip);


        // --------------------------------
        // ACCOUNT LOCK CHECK
        // --------------------------------

        if (loginAttemptService.isLocked(username)) {

            log.warn(
                    "LOGIN_BLOCKED username={} ip={} reason=ACCOUNT_LOCKED",
                    username,
                    ip
            );

            return ResponseEntity
                    .status(HttpStatus.LOCKED)
                    .body(new ApiResponse(
                            "Account is temporarily locked. Try again later."
                    ));
        }

        // --------------------------------
        // FIND USER FROM DATABASE
        // --------------------------------

        User user = userRepository
                .findByUsername(username)
                .orElse(null);


        // --------------------------------
        // INVALID LOGIN
        // --------------------------------

        try {
            externalLoginService.validateExternalLogin(
                    username,
                    request.password(),
                    () -> user != null && passwordEncoder.matches(
                            request.password(),
                            user.getPassword()
                    )
            );
        } catch (InvalidCredentialsException ex) {

            loginAttemptService.loginFailed(username);

            int failedAttempts =
                    loginAttemptService
                            .getFailedAttempts(username);

            log.warn(
                    "LOGIN_FAILED username={} ip={} failedAttempts={}",
                    username,
                    ip,
                    failedAttempts
            );


            // Account became locked
            if (loginAttemptService.isLocked(username)) {

                log.warn(
                        "ACCOUNT_LOCKED username={} ip={}",
                        username,
                        ip
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


        // --------------------------------
        // ACCOUNT STATUS
        // --------------------------------

        if (!user.isActive()) {

            log.warn(
                    "LOGIN_BLOCKED username={} ip={} reason=ACCOUNT_INACTIVE",
                    username,
                    ip
            );

            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse(
                            "Account is inactive"
                    ));
        }


        // --------------------------------
        // LOGIN SUCCESS
        // --------------------------------

        loginAttemptService.loginSucceeded(username);


        // --------------------------------
        // GENERATE JWTs
        // --------------------------------

        String accessToken =
                jwtService.generateAccessToken(user);

        String refreshToken =
                jwtService.generateRefreshToken(user);


        // --------------------------------
        // STORE TOKENS IN MEMORY
        // --------------------------------

        tokenStore.save(
                accessToken,
                user.getUsername()
        );

        tokenStore.save(
                refreshToken,
                user.getUsername()
        );


        // --------------------------------
        // CREATE COOKIES
        // --------------------------------

        ResponseCookie accessCookie =
                cookieService.createAccessTokenCookie(
                        accessToken,
                        900
                );

        ResponseCookie refreshCookie =
                cookieService.createRefreshTokenCookie(
                        refreshToken,
                        86400
                );


        log.info(
                "LOGIN_SUCCESS username={} ip={}",
                username,
                ip
        );


        // --------------------------------
        // RETURN RESPONSE
        // --------------------------------

        return ResponseEntity
                .ok()
                .header(
                        HttpHeaders.SET_COOKIE,
                        accessCookie.toString()
                )
                .header(
                        HttpHeaders.SET_COOKIE,
                        refreshCookie.toString()
                )
                .body(new LoginResponse(
                        "Login successful",
                        user.getUsername(),
                        Set.of(user.getRole())
                ));
    }


    // ============================================================
    // REFRESH TOKEN
    // ============================================================

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(
            HttpServletRequest request
    ) {

        String refreshToken =
                extractCookie(
                        request,
                        "REFRESH_TOKEN"
                );


        // --------------------------------
        // CHECK COOKIE
        // --------------------------------

        if (refreshToken == null) {

            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse(
                            "Refresh token is required"
                    ));
        }


        try {

            // --------------------------------
            // CHECK TOKEN STORE
            // --------------------------------

            if (!tokenStore.isActive(refreshToken)) {

                return ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED)
                        .body(new ApiResponse(
                                "Refresh token is invalid or already used"
                        ));
            }


            // --------------------------------
            // CHECK TOKEN TYPE
            // --------------------------------

            if (!jwtService.isRefreshToken(
                    refreshToken
            )) {

                return ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED)
                        .body(new ApiResponse(
                                "Access token cannot be used as refresh token"
                        ));
            }


            // --------------------------------
            // GET USERNAME
            // --------------------------------

            String username =
                    jwtService.username(refreshToken);


            // --------------------------------
            // FIND USER FROM DATABASE
            // --------------------------------

            User user = userRepository
                    .findByUsername(username)
                    .orElse(null);


            if (user == null) {

                return ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED)
                        .body(new ApiResponse(
                                "User not found"
                        ));
            }


            // --------------------------------
            // CHECK ACCOUNT STATUS
            // --------------------------------

            if (!user.isActive()) {

                return ResponseEntity
                        .status(HttpStatus.FORBIDDEN)
                        .body(new ApiResponse(
                                "Account is inactive"
                        ));
            }


            // --------------------------------
            // ROTATE OLD REFRESH TOKEN
            // --------------------------------

            tokenStore.remove(refreshToken);


            // --------------------------------
            // CREATE NEW TOKENS
            // --------------------------------

            String newAccessToken =
                    jwtService.generateAccessToken(user);

            String newRefreshToken =
                    jwtService.generateRefreshToken(user);


            // --------------------------------
            // STORE NEW TOKENS
            // --------------------------------

            tokenStore.save(
                    newAccessToken,
                    user.getUsername()
            );

            tokenStore.save(
                    newRefreshToken,
                    user.getUsername()
            );


            // --------------------------------
            // CREATE NEW COOKIES
            // --------------------------------

            ResponseCookie accessCookie =
                    cookieService.createAccessTokenCookie(
                            newAccessToken,
                            900
                    );

            ResponseCookie refreshCookie =
                    cookieService.createRefreshTokenCookie(
                            newRefreshToken,
                            86400
                    );


            log.info(
                    "TOKEN_REFRESH username={}",
                    username
            );


            return ResponseEntity
                    .ok()
                    .header(
                            HttpHeaders.SET_COOKIE,
                            accessCookie.toString()
                    )
                    .header(
                            HttpHeaders.SET_COOKIE,
                            refreshCookie.toString()
                    )
                    .body(new ApiResponse(
                            "Token refreshed successfully"
                    ));

        } catch (Exception ex) {

            log.warn(
                    "Refresh token validation failed: {}",
                    ex.getMessage()
            );

            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse(
                            "Invalid or expired refresh token"
                    ));
        }
    }


    // ============================================================
    // CURRENT USER PROFILE
    // ============================================================

    @GetMapping("/me")
    public ResponseEntity<?> getProfile(
            Authentication authentication
    ) {

        String username =
                authentication.getName();


        User user = userRepository
                .findByUsername(username)
                .orElse(null);


        if (user == null) {

            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse(
                            "User not found"
                    ));
        }


        String accountType =
                "ADMIN".equals(user.getRole())
                        ? "ADMIN"
                        : "USER";


        String status =
                user.isActive()
                        ? "ACTIVE"
                        : "INACTIVE";


        return ResponseEntity.ok(
                Map.of(
                        "username", user.getUsername(),
                        "roles", Set.of(user.getRole()),
                        "status", status,
                        "accountType", accountType,
                        "authenticated", true
                )
        );
    }


    // ============================================================
    // AUTH CHECK
    // ============================================================

    @GetMapping("/auth")
    public ResponseEntity<?> auth(
            Authentication authentication
    ) {

        return ResponseEntity.ok(
                Map.of(
                        "authenticated", true,
                        "username", authentication.getName(),
                        "roles",
                        authentication
                                .getAuthorities()
                                .stream()
                                .map(a ->
                                        a.getAuthority()
                                                .replace(
                                                        "ROLE_",
                                                        ""
                                                )
                                )
                                .toList()
                )
        );
    }


    // ============================================================
    // LOGOUT
    // ============================================================

    @PostMapping("/logout")
    public ResponseEntity<?> logout(
            HttpServletRequest request
    ) {

        // --------------------------------
        // ACCESS TOKEN
        // --------------------------------

        String accessToken =
                extractCookie(
                        request,
                        "ACCESS_TOKEN"
                );


        // --------------------------------
        // REFRESH TOKEN
        // --------------------------------

        String refreshToken =
                extractCookie(
                        request,
                        "REFRESH_TOKEN"
                );


        // --------------------------------
        // REVOKE ACCESS TOKEN
        // --------------------------------

        if (accessToken != null) {

            tokenStore.remove(accessToken);
        }


        // --------------------------------
        // REVOKE REFRESH TOKEN
        // --------------------------------

        if (refreshToken != null) {

            tokenStore.remove(refreshToken);
        }


        // --------------------------------
        // CLEAR COOKIES
        // --------------------------------

        ResponseCookie clearAccessCookie =
                cookieService.clearAccessTokenCookie();

        ResponseCookie clearRefreshCookie =
                cookieService.clearRefreshTokenCookie();


        log.info("LOGOUT_SUCCESS");


        return ResponseEntity
                .ok()
                .header(
                        HttpHeaders.SET_COOKIE,
                        clearAccessCookie.toString()
                )
                .header(
                        HttpHeaders.SET_COOKIE,
                        clearRefreshCookie.toString()
                )
                .body(new ApiResponse(
                        "Logout successful"
                ));
    }
}

