package com.hdfc.jwtauth.resilience;

import com.hdfc.jwtauth.exceptions.ExternalServiceException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class LoginCircuitBreaker {

    private static final Logger log = LoggerFactory.getLogger(LoginCircuitBreaker.class);

    private final CircuitBreaker circuitBreaker;

    public LoginCircuitBreaker(@Qualifier("externalLoginCircuitBreaker") CircuitBreaker circuitBreaker) {
        this.circuitBreaker = circuitBreaker;
    }

    public <T> T executeExternalLogin(CircuitBreakerCallable<T> externalLoginCall) throws Exception {
        try {
            return circuitBreaker.executeSupplier(() -> {
                try {
                    log.debug("Executing external login call with circuit breaker protection");
                    return externalLoginCall.call();
                } catch (RuntimeException ex) {
                    throw ex;
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            });
        } catch (CallNotPermittedException ex) {
            log.warn("Login circuit breaker is open; external login call rejected");
            throw new ExternalServiceException(
                    "External service is currently unavailable because the circuit breaker is open",
                    ex);
        } catch (RuntimeException ex) {
            if (ex.getCause() instanceof Exception
                    && !(ex.getCause() instanceof RuntimeException)) {
                throw (Exception) ex.getCause();
            }

            throw ex;
        }
    }

    public CircuitBreaker.State getState() {
        return circuitBreaker.getState();
    }

    public void reset() {
        circuitBreaker.reset();
        log.info("Login circuit breaker reset");
    }

    @FunctionalInterface
    public interface CircuitBreakerCallable<T> {
        T call() throws Exception;
    }
}
