package com.hdfc.jwtauth.security;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TokenStore {

    private final Map<String, String> activeTokens =
            new ConcurrentHashMap<>();

    public void save(String token, String username) {
        activeTokens.put(token, username);
    }

    public boolean isActive(String token) {
        return activeTokens.containsKey(token);
    }

    public String username(String token) {
        return activeTokens.get(token);
    }

    public void remove(String token) {
        activeTokens.remove(token);
    }

    private String hash(String token) {
        try {
            MessageDigest digest= MessageDigest.getInstance("SHA-256");
            byte[] hash=digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);         }
        catch (NoSuchAlgorithmException e ) {
            throw new IllegalStateException(
                    "SHA-256 algorithm unavailable", e            );
        }

    }}