package com.hdfc.jwtauth.circuitbreaker;

import com.hdfc.jwtauth.controllers.TestController;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class DatabaseCircuitBreakerEndpointTest {

    @Test
    void returnsDatabaseCircuitBreakerState() throws Exception {
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.ofDefaults();
        registry.circuitBreaker("databaseCircuitBreakerCore");
        MockMvc mockMvc = standaloneSetup(new TestController(registry)).build();

        mockMvc.perform(get("/api/v1/test/circuit-state"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.circuitBreaker").value("CLOSED"));
    }
}
