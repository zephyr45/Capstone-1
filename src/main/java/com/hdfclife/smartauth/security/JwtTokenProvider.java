package com.hdfclife.smartauth.security;

import com.hdfclife.smartauth.config.JwtProperties;
import com.hdfclife.smartauth.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class JwtTokenProvider {

    private final JwtProperties properties;
    private final SecretKey key;

    public JwtTokenProvider(JwtProperties properties) {
        this.properties = properties;
        this.key = Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    // Generate Access Token
    public String generateAccessToken(User user) {
        return generateAccessToken(user, UUID.randomUUID().toString());
    }

    public String generateAccessToken(User user, String sessionId) {

        Date now = new Date();
        Date expiry = new Date(
                now.getTime() + properties.getAccessTokenExpiration().toMillis()
        );

        return Jwts.builder()
                .subject(user.username())
                .claim("roles", user.roles())
                .claim("type", "access")
                .claim("sessionId", sessionId)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    // Generate Refresh Token
    public String generateRefreshToken(User user) {
        return generateRefreshToken(user, UUID.randomUUID().toString());
    }

    public String generateRefreshToken(User user, String sessionId) {

        Date now = new Date();
        Date expiry = new Date(
                now.getTime() + properties.getRefreshTokenExpiration().toMillis()
        );

        return Jwts.builder()
                .subject(user.username())
                .claim("type", "refresh")
                .claim("sessionId", sessionId)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    // Parse JWT
    public Claims parse(String token) {

        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String username(String token) {
        return parse(token).getSubject();
    }

    public String sessionId(String token) {
        return parse(token).get("sessionId", String.class);
    }

    public Set<String> roles(String token) {

        Object value = parse(token).get("roles");

        if (value instanceof java.util.Collection<?> collection) {

            return collection.stream()
                    .map(Object::toString)
                    .collect(Collectors.toSet());
        }

        return Set.of();
    }

    // Check whether token is Access Token
    public boolean isAccessToken(String token) {

        return "access".equals(parse(token).get("type"));
    }

    // Check whether token is Refresh Token
    public boolean isRefreshToken(String token) {

        return "refresh".equals(parse(token).get("type"));
    }
}
