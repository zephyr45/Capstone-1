package com.hdfc.jwtauth.services;

import com.hdfc.jwtauth.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    private static final long ACCESS_TOKEN_EXPIRATION_MS =
            1 * 60 * 1000L;

    private static final long REFRESH_TOKEN_EXPIRATION_MS =
            24 * 60 * 60 * 1000L;

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(
                secret.getBytes(StandardCharsets.UTF_8)
        );
    }

    public String generateAccessToken(User user) {

        Date now = new Date();

        Date expiry = new Date(
                now.getTime() + ACCESS_TOKEN_EXPIRATION_MS
        );

        return Jwts.builder()
                .subject(user.getUsername())
                .claim("roles", Set.of(user.getRole()))
                .claim("type", "access")
                .issuedAt(now)
                .expiration(expiry)
                .signWith(getKey())
                .compact();
    }

    public String generateRefreshToken(User user) {

        Date now = new Date();

        Date expiry = new Date(
                now.getTime() + REFRESH_TOKEN_EXPIRATION_MS
        );

        return Jwts.builder()
                .subject(user.getUsername())
                .claim("type", "refresh")
                .issuedAt(now)
                .expiration(expiry)
                .signWith(getKey())
                .compact();
    }

    public Claims parse(String token) {

        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String username(String token) {

        return parse(token)
                .getSubject();
    }

    public Set<String> roles(String token) {

        Object value =
                parse(token).get("roles");

        if (value instanceof java.util.Collection<?> collection) {

            return collection.stream()
                    .map(Object::toString)
                    .collect(Collectors.toSet());
        }

        return Set.of();
    }

    public boolean isAccessToken(String token) {

        return "access".equals(
                parse(token).get("type", String.class)
        );
    }

    public boolean isRefreshToken(String token) {

        return "refresh".equals(
                parse(token).get("type", String.class)
        );
    }
}