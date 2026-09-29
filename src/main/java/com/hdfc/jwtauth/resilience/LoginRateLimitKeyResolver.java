package com.hdfc.jwtauth.resilience;

import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class LoginRateLimitKeyResolver {

    public String forUsername(String username) {
        return "login-user-" + normalizeUsername(username);
    }

    public String forIp(String ip) {
        return "login-ip-" + normalizeIp(ip);
    }

    String normalizeUsername(String username) {
        return username == null
                ? "<missing>"
                : username.trim().toLowerCase(Locale.ROOT);
    }

    String normalizeIp(String ip) {
        return ip == null || ip.isBlank()
                ? "<unknown>"
                : ip.trim();
    }
}
