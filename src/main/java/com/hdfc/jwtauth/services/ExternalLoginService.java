package com.hdfc.jwtauth.services;

import com.hdfc.jwtauth.exceptions.ExternalServiceException;
import com.hdfc.jwtauth.exceptions.InvalidCredentialsException;
import com.hdfc.jwtauth.resilience.LoginCircuitBreaker;
import com.hdfc.jwtauth.resilience.LoginFallbackHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.function.BooleanSupplier;

@Service
public class ExternalLoginService {

    private static final Logger log = LoggerFactory.getLogger(ExternalLoginService.class);

    private static final String VALID_USERNAME = "user";
    private static final String VALID_PASSWORD = "password";
    private static final String SERVICE_DOWN_USERNAME = "serviceDown";

    private final LoginCircuitBreaker loginCircuitBreaker;
    private final LoginFallbackHandler loginFallbackHandler;

    public ExternalLoginService(
            LoginCircuitBreaker loginCircuitBreaker,
            LoginFallbackHandler loginFallbackHandler) {
        this.loginCircuitBreaker = loginCircuitBreaker;
        this.loginFallbackHandler = loginFallbackHandler;
    }

    public boolean validateExternalLogin(
            String username,
            String password,
            BooleanSupplier existingCredentialValidator) {

        try {
            return loginCircuitBreaker.executeExternalLogin(() ->
                    performExternalLoginValidation(username, password, existingCredentialValidator));
        } catch (InvalidCredentialsException ex) {
            throw ex;
        } catch (Exception ex) {
            loginFallbackHandler.handleExternalServiceFailure(username, ex);
            return false;
        }
    }

    private boolean performExternalLoginValidation(
            String username,
            String password,
            BooleanSupplier existingCredentialValidator) {

        log.debug("Performing mock external login validation for username={}", username);

        if (SERVICE_DOWN_USERNAME.equals(username)) {
            throw new ExternalServiceException(
                    "External authentication service is currently unavailable");
        }

        if (VALID_USERNAME.equals(username) && VALID_PASSWORD.equals(password)) {
            log.info("Mock external login successful for username={}", username);
            return true;
        }

        if (existingCredentialValidator.getAsBoolean()) {
            log.info("Mock external login accepted existing task3 user username={}", username);
            return true;
        }

        throw new InvalidCredentialsException("Invalid username or password");
    }
}
