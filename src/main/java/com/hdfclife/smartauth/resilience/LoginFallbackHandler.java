package com.hdfclife.smartauth.resilience;

import com.hdfclife.smartauth.exception.ExternalServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LoginFallbackHandler {

    private static final Logger log = LoggerFactory.getLogger(LoginFallbackHandler.class);

    public void handleExternalServiceFailure(String username, Throwable throwable) {
        log.warn(
                "External login service unavailable for username={}: {}",
                username,
                throwable.getMessage());

        if (throwable instanceof ExternalServiceException externalServiceException) {
            throw externalServiceException;
        }

        throw new ExternalServiceException(
                "External authentication service failed: " + throwable.getMessage(),
                throwable);
    }
}
