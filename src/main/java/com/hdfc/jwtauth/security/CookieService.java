package com.hdfc.jwtauth.security;

import jakarta.servlet.http.Cookie;

import jakarta.servlet.http.HttpServletResponse;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class CookieService {

    @Value("${app.security.cookie.secure:false}")
    private boolean secure;

    @Value("${app.security.cookie.same-site:Lax}")
    private String sameSite;

    @Value("${app.security.cookie.domain:}")
    private String domain;

    @Value("${app.security.cookie.access-token-name:ACCESS_TOKEN}")
    private String accessTokenName;

    @Value("${app.security.cookie.refresh-token-name:REFRESH_TOKEN}")
    private String refreshTokenName;

    public ResponseCookie createAccessTokenCookie(String token, long expirationSeconds) {

        return createCookie(
                accessTokenName,
                token,
                expirationSeconds
        );
    }

    public ResponseCookie createRefreshTokenCookie(String token, long expirationSeconds) {

        return createCookie(
                refreshTokenName,
                token,
                expirationSeconds
        );
    }

    private ResponseCookie createCookie(
            String name,
            String value,
            long expirationSeconds) {

        ResponseCookie.ResponseCookieBuilder builder =
                ResponseCookie.from(name, value)
                        .httpOnly(true)
                        .secure(secure)
                        .path("/")
                        .sameSite(sameSite)
                        .maxAge(Duration.ofSeconds(expirationSeconds));

        if (domain != null && !domain.isBlank()) {
            builder.domain(domain);
        }

        return builder.build();
    }

    public ResponseCookie clearAccessTokenCookie() {
        return clearCookie(accessTokenName);
    }

    public ResponseCookie clearRefreshTokenCookie() {
        return clearCookie(refreshTokenName);
    }

    private ResponseCookie clearCookie(String name) {

        return ResponseCookie.from(name, "")
                .httpOnly(true)
                .secure(secure)
                .path("/")
                .sameSite(sameSite)
                .maxAge(Duration.ZERO)
                .build();
    }
}