package com.hdfc.jwtauth.services;

import com.hdfc.jwtauth.exceptions.InvalidCredentialsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.function.BooleanSupplier;

@Service
public class ExternalLoginService {

    private static final Logger log = LoggerFactory.getLogger(ExternalLoginService.class);

    public boolean validateExternalLogin(
            String username,
            String password,
            BooleanSupplier existingCredentialValidator) {

        log.debug("Performing mock external login validation for username={}", username);

        if (existingCredentialValidator.getAsBoolean()) {
            log.info("External login accepted persisted user username={}", username);
            return true;
        }

        throw new InvalidCredentialsException("Invalid username or password");
    }
}
