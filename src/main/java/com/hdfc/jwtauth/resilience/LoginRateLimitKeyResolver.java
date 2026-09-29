package com.hdfc.jwtauth.resilience;

import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class LoginRateLimitKeyResolver {

    public String forUsername(String username) {
        String normalizedUsername = username == null
                ? "<missing>"
                : username.trim().toLowerCase(Locale.ROOT);

        return "login-user-" + normalizedUsername;
    }

    public String forIp(String ip) {
        String normalizedIp = ip == null || ip.isBlank()
                ? "<unknown>"
                : ip.trim();

        return "login-ip-" + normalizedIp;
    }
}
