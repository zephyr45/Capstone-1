package com.hdfc.jwtauth.resilience;

import com.hdfc.jwtauth.exceptions.DatabaseUnavailableException;
import com.hdfc.jwtauth.repository.UserRepository;
import com.hdfc.jwtauth.services.UserService;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.TransactionSystemException;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "resilience4j.circuitbreaker.instances.databaseCircuitBreakerCore.failure-rate-threshold=50",
        "resilience4j.circuitbreaker.instances.databaseCircuitBreakerCore.minimum-number-of-calls=5",
        "resilience4j.circuitbreaker.instances.databaseCircuitBreakerCore.sliding-window-size=10",
        "resilience4j.circuitbreaker.instances.databaseCircuitBreakerCore.permitted-number-of-calls-in-half-open-state=3",
        "resilience4j.circuitbreaker.instances.databaseCircuitBreakerCore.wait-duration-in-open-state=100ms",
        "resilience4j.circuitbreaker.instances.databaseCircuitBreakerCore.automatic-transition-from-open-to-half-open-enabled=true",
        "resilience4j.circuitbreaker.instances.databaseCircuitBreakerCore.record-exceptions[0]=org.springframework.dao.DataAccessException",
        "resilience4j.circuitbreaker.instances.databaseCircuitBreakerCore.record-exceptions[1]=org.springframework.transaction.TransactionException",
        "resilience4j.circuitbreaker.instances.databaseCircuitBreakerCore.record-exceptions[2]=jakarta.persistence.PersistenceException",
        "resilience4j.ratelimiter.configs.default.limit-for-period=20"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DatabaseCircuitBreakerTest {

    private static final String CIRCUIT_BREAKER_NAME = "databaseCircuitBreakerCore";

    @Autowired
    private UserService userService;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserRepository userRepository;

    private CircuitBreaker circuitBreaker;

    @BeforeEach
    void setUp() {
        reset(userRepository);
        circuitBreaker = circuitBreakerRegistry.circuitBreaker(CIRCUIT_BREAKER_NAME);
        circuitBreaker.reset();
    }

    @Test
    void returnsServiceUnavailableAndShortCircuitsAfterFiveDatabaseFailures() throws Exception {
        when(userRepository.findByUsername("sachin"))
                .thenThrow(new DataAccessResourceFailureException("database down"));

        for (int attempt = 0; attempt < 5; attempt++) {
            performLogin()
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.message").value("Database unavailable"));
        }

        assertEquals(CircuitBreaker.State.OPEN, circuitBreaker.getState());

        performLogin()
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message")
                        .value("Database circuit breaker is OPEN"));
        verify(userRepository, times(5)).findByUsername("sachin");
    }

    @Test
    void recoversFromHalfOpenToClosedAfterSuccessfulDatabaseCalls() throws InterruptedException {
        when(userRepository.findByUsername("sachin"))
                .thenThrow(new DataAccessResourceFailureException("database down"));

        for (int attempt = 0; attempt < 5; attempt++) {
            assertThrows(
                    DatabaseUnavailableException.class,
                    () -> userService.findUserForLogin("sachin")
            );
        }

        assertEquals(CircuitBreaker.State.OPEN, circuitBreaker.getState());
        awaitState(CircuitBreaker.State.HALF_OPEN, Duration.ofSeconds(3));

        reset(userRepository);
        when(userRepository.findByUsername("sachin")).thenReturn(Optional.empty());

        userService.findUserForLogin("sachin");
        userService.findUserForLogin("sachin");
        userService.findUserForLogin("sachin");

        assertEquals(CircuitBreaker.State.CLOSED, circuitBreaker.getState());
        verify(userRepository, times(3)).findByUsername("sachin");
    }

    @Test
    void translatesEachConfiguredDatabaseExceptionFamily() {
        List<RuntimeException> databaseFailures = List.of(
                new DataAccessResourceFailureException("data access failed"),
                new TransactionSystemException("transaction failed"),
                new PersistenceException("persistence failed")
        );

        for (RuntimeException databaseFailure : databaseFailures) {
            circuitBreaker.reset();
            reset(userRepository);
            when(userRepository.findByUsername("sachin")).thenThrow(databaseFailure);

            DatabaseUnavailableException translated = assertThrows(
                    DatabaseUnavailableException.class,
                    () -> userService.findUserForLogin("sachin")
            );

            assertSame(databaseFailure, translated.getCause());
        }
    }

    @Test
    void propagatesUnrelatedRuntimeExceptionsWithoutRecordingDatabaseFailure() {
        IllegalStateException unrelatedFailure =
                new IllegalStateException("unexpected application failure");
        when(userRepository.findByUsername("sachin")).thenThrow(unrelatedFailure);

        IllegalStateException thrown = assertThrows(
                IllegalStateException.class,
                () -> userService.findUserForLogin("sachin")
        );

        assertSame(unrelatedFailure, thrown);
        assertEquals(CircuitBreaker.State.CLOSED, circuitBreaker.getState());
        assertEquals(0, circuitBreaker.getMetrics().getNumberOfFailedCalls());
    }

    private void awaitState(
            CircuitBreaker.State expectedState,
            Duration timeout
    ) throws InterruptedException {
        long deadline = System.nanoTime() + timeout.toNanos();

        while (System.nanoTime() < deadline) {
            if (circuitBreaker.getState() == expectedState) {
                return;
            }
            Thread.sleep(20);
        }

        assertEquals(expectedState, circuitBreaker.getState());
    }

    private ResultActions performLogin() throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"sachin\",\"password\":\"sachin123\"}"));
    }
}
