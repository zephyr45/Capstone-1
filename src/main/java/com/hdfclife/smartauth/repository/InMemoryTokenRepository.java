package com.hdfclife.smartauth.repository;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryTokenRepository {

    private final Map<String, TokenSession> activeTokens =
            new ConcurrentHashMap<>();

    public void save(String token, String username) {
        save(token, username, null);
    }

    public void save(String token, String username, String sessionId) {
        activeTokens.put(token, new TokenSession(username, sessionId));
    }

    public boolean isActive(String token) {
        return activeTokens.containsKey(token);
    }

    public String username(String token) {
        TokenSession session = activeTokens.get(token);
        return session == null ? null : session.username();
    }

    public String sessionId(String token) {
        TokenSession session = activeTokens.get(token);
        return session == null ? null : session.sessionId();
    }

    public void remove(String token) {
        activeTokens.remove(token);
    }

    public void removeSession(String sessionId) {
        if (sessionId != null) {
            activeTokens.entrySet().removeIf(entry -> sessionId.equals(entry.getValue().sessionId()));
        }
    }

    private record TokenSession(String username, String sessionId) {}
}
