package com.hdfc.jwtauth.exceptionhandling;

import com.hdfc.jwtauth.exceptions.DatabaseUnavailableException;
import com.hdfc.jwtauth.exceptions.GlobalExceptionHandler;
import com.hdfc.jwtauth.exceptions.InvalidCredentialsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = standaloneSetup(new ExceptionThrowingController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void returnsUnauthorizedResponseForInvalidCredentials() throws Exception {
        mockMvc.perform(get("/test/invalid-credentials"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Invalid username or password"))
                .andExpect(jsonPath("$.path").value("/test/invalid-credentials"));
    }

    @Test
    void returnsServiceUnavailableResponseForDatabaseFailure() throws Exception {
        mockMvc.perform(get("/test/database-failure"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.message")
                        .value("Database unavailable"))
                .andExpect(jsonPath("$.path").value("/test/database-failure"));
    }

    @RestController
    static class ExceptionThrowingController {

        @GetMapping("/test/invalid-credentials")
        void invalidCredentials() {
            throw new InvalidCredentialsException("credentials rejected");
        }

        @GetMapping("/test/database-failure")
        void databaseFailure() {
            throw new DatabaseUnavailableException(
                    "database unavailable",
                    new IllegalStateException("connection refused")
            );
        }
    }
}
