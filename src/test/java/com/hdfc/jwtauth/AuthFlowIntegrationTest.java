package com.hdfc.jwtauth;

import com.hdfc.jwtauth.entity.User;
import com.hdfc.jwtauth.repository.UserRepository;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.servlet.http.Cookie;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @Autowired
    private RateLimiterRegistry rateLimiterRegistry;

    @BeforeEach
    void seedRequiredUser() {
        circuitBreakerRegistry
                .circuitBreaker("databaseCircuitBreakerCore")
                .reset();
        rateLimiterRegistry.getAllRateLimiters().stream()
                .map(RateLimiter::getName)
                .toList()
                .forEach(rateLimiterRegistry::remove);

        User user = userRepository.findByUsername("sachin")
                .orElseGet(() -> new User("sachin", "", "USER", true));
        user.setPassword(passwordEncoder.encode("sachin123"));
        user.setRole("USER");
        user.setActive(true);
        userRepository.save(user);
    }

    @Test
    void returnsTooManyRequestsOnSixthLoginForSameUsernameAndIp() throws Exception {
        User user = userRepository.findByUsername("rate-user")
                .orElseGet(() -> new User("rate-user", "", "USER", true));
        user.setPassword(passwordEncoder.encode("rate-password"));
        user.setRole("USER");
        user.setActive(true);
        userRepository.save(user);

        for (int attempt = 0; attempt < 5; attempt++) {
            mockMvc.perform(post("/api/v1/auth/login")
                            .with(request -> {
                                request.setRemoteAddr("198.51.100.10");
                                return request;
                            })
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"rate-user\",\"password\":\"rate-password\"}"))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(post("/api/v1/auth/login")
                        .with(request -> {
                            request.setRemoteAddr("198.51.100.10");
                            return request;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"rate-user\",\"password\":\"rate-password\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.message")
                        .value("Too many login attempts. Please try again later."));
    }

    @Test
    void returnsTooManyRequestsForOneIpAcrossRotatingUsernames() throws Exception {
        for (int attempt = 0; attempt < 5; attempt++) {
            mockMvc.perform(post("/api/v1/auth/login")
                            .with(request -> {
                                request.setRemoteAddr("198.51.100.50");
                                return request;
                            })
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"unknown-" + attempt + "\",\"password\":\"wrong\"}"))
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(post("/api/v1/auth/login")
                        .with(request -> {
                            request.setRemoteAddr("198.51.100.50");
                            return request;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"unknown-6\",\"password\":\"wrong\"}"))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void returnsTooManyRequestsForOneUsernameAcrossRotatingIps() throws Exception {
        User user = userRepository.findByUsername("rotating-ip-user")
                .orElseGet(() -> new User("rotating-ip-user", "", "USER", true));
        user.setPassword(passwordEncoder.encode("rate-password"));
        user.setRole("USER");
        user.setActive(true);
        userRepository.save(user);

        for (int attempt = 0; attempt < 5; attempt++) {
            String clientIp = "203.0.113." + attempt;
            mockMvc.perform(post("/api/v1/auth/login")
                            .with(request -> {
                                request.setRemoteAddr(clientIp);
                                return request;
                            })
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"rotating-ip-user\",\"password\":\"rate-password\"}"))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(post("/api/v1/auth/login")
                        .with(request -> {
                            request.setRemoteAddr("203.0.113.99");
                            return request;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"rotating-ip-user\",\"password\":\"rate-password\"}"))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void preservesLoginRefreshAuthAndLogoutFlow() throws Exception {
        MvcResult login = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"sachin\",\"password\":\"sachin123\"}"))
                .andExpect(status().isOk())
                .andReturn();

        Cookie accessToken = login.getResponse().getCookie("ACCESS_TOKEN");
        Cookie refreshToken = login.getResponse().getCookie("REFRESH_TOKEN");

        mockMvc.perform(get("/api/v1/auth/auth")
                        .cookie(accessToken))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"username\":\"sachin\"")));

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(refreshToken))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Token refreshed successfully")));

        mockMvc.perform(post("/api/v1/auth/logout")
                        .cookie(accessToken, refreshToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/auth/auth")
                        .cookie(accessToken))
                .andExpect(status().isUnauthorized());

        assertEquals(
                CircuitBreaker.State.CLOSED,
                circuitBreakerRegistry
                        .circuitBreaker("databaseCircuitBreakerCore")
                        .getState()
        );
    }

    @Test
    void mapsInvalidCredentialsToUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"unknown-user\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized());
    }
}
