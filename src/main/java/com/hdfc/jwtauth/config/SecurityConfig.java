package com.hdfc.jwtauth.config;

import com.hdfc.jwtauth.exceptions.CustomAccessDeniedHandler;
import com.hdfc.jwtauth.security.CustomAuthenticationEntryPoint;
import com.hdfc.jwtauth.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
    private final CustomAccessDeniedHandler customAccessDeniedHandler;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            CustomAuthenticationEntryPoint customAuthenticationEntryPoint,
            CustomAccessDeniedHandler customAccessDeniedHandler
    ) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.customAuthenticationEntryPoint = customAuthenticationEntryPoint;
        this.customAccessDeniedHandler = customAccessDeniedHandler;
    }
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        return http

                // Cookie-based JWT authentication
                // CSRF should eventually be enabled for production.
                .csrf(csrf -> csrf.disable())

                // JWT authentication is stateless
                .sessionManagement(sm ->
                        sm.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                // We use our own /api/v1/auth/logout endpoint
                .logout(logout -> logout.disable())

                // CORS required for Next.js + HttpOnly cookies
                .cors(cors -> {})

                // Authentication / authorization error handling
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(
                                customAuthenticationEntryPoint
                        )
                        .accessDeniedHandler(
                                customAccessDeniedHandler
                        )
                )

                .authorizeHttpRequests(auth -> auth

                        // Public endpoints
                        .requestMatchers(
                                "/api/v1/auth/login",
                                "/api/v1/auth/signup",
                                "/api/v1/auth/refresh",
                                "/api/v1/auth/public",
                                "/api/v1/test/**",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/error"
                        ).permitAll()

                        .requestMatchers("/admin/**")
                        .hasRole("ADMIN")

                        .requestMatchers("/user/**")
                        .hasAnyRole("USER", "ADMIN")

                        .requestMatchers(
                                "/api/v1/auth/auth",
                                "/api/v1/auth/me",
                                "/api/v1/auth/logout"
                        ).authenticated()

                        .anyRequest().authenticated()
                )

                // Read JWT from ACCESS_TOKEN cookie
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        LogoutFilter.class
                )

                .build();
    }


}