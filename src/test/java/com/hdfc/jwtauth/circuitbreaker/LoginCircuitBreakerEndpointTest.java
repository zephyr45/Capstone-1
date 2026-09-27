package com.hdfc.jwtauth.circuitbreaker;

import com.hdfc.jwtauth.controllers.TestController;
import com.hdfc.jwtauth.resilience.LoginCircuitBreaker;
import com.hdfc.jwtauth.resilience.LoginFallbackHandler;
import com.hdfc.jwtauth.services.ExternalLoginService;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class LoginCircuitBreakerEndpointTest {

    @Test
    void shouldAllowSuccessfulCircuitBreakerStateRequest() throws Exception {
        MockMvc mockMvc = circuitBreakerMockMvc();

        mockMvc.perform(get("/api/v1/test/circuit-state"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.circuitBreaker").value("CLOSED"));
    }

    @Test
    void shouldReturnExistingCircuitBreakerFallbackOnFailure() throws Exception {
        MockMvc mockMvc = circuitBreakerMockMvc();

        mockMvc.perform(get("/api/v1/test/external"))
                .andExpect(status().isOk())
                .andExpect(content().string(
                        "FALLBACK RESPONSE: External authentication service is temporarily unavailable"));
    }

    private MockMvc circuitBreakerMockMvc() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .minimumNumberOfCalls(3)
                .slidingWindowSize(3)
                .waitDurationInOpenState(Duration.ofMinutes(1))
                .build();
        CircuitBreaker circuitBreaker = CircuitBreaker.of(
                "test-http-circuit-" + System.nanoTime(), config);

        LoginCircuitBreaker loginCircuitBreaker = new LoginCircuitBreaker(circuitBreaker);
        ExternalLoginService externalLoginService =
                new ExternalLoginService(loginCircuitBreaker, new LoginFallbackHandler());

        return standaloneSetup(new TestController(loginCircuitBreaker, externalLoginService)).build();
    }
}
