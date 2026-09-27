package com.hdfc.jwtauth.security;
import com.hdfc.jwtauth.services.JwtService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.stream.Collectors;
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log =
            LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtService jwtService;
    private final TokenStore tokenStore;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            TokenStore tokenStore) {

        this.jwtService = jwtService;
        this.tokenStore = tokenStore;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        // Get ACCESS_TOKEN from cookie
        String token = extractAccessToken(request);

        if (token != null) {

            try {

                Claims claims = jwtService.parse(token);

                if (tokenStore.isActive(token)) {

                    var authorities =
                            jwtService.roles(token)
                                    .stream()
                                    .map(role ->
                                            new SimpleGrantedAuthority(
                                                    "ROLE_" + role
                                            ))
                                    .collect(Collectors.toSet());

                    var authentication =
                            new UsernamePasswordAuthenticationToken(
                                    claims.getSubject(),
                                    null,
                                    authorities
                            );

                    SecurityContextHolder
                            .getContext()
                            .setAuthentication(authentication);

                    log.debug(
                            "JWT authentication successful username={}",
                            claims.getSubject()
                    );

                } else {

                    log.debug(
                            "JWT rejected because token is not active"
                    );
                }

            } catch (Exception ex) {

                log.debug(
                        "Invalid JWT: {}",
                        ex.getMessage()
                );
            }
        }

        filterChain.doFilter(request, response);
    }


    private String extractAccessToken(
            HttpServletRequest request) {

        Cookie[] cookies = request.getCookies();

        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {

            if ("ACCESS_TOKEN".equals(cookie.getName())) {
                return cookie.getValue();
            }
        }

        return null;
    }
}