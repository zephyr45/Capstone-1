package com.hdfc.jwtauth.services;

import com.hdfc.jwtauth.exceptions.ExternalServiceException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class MockExternalService {

    private final RestClient restClient;

    public MockExternalService(RestClient.Builder builder) {
        this.restClient = builder
                .baseUrl("http://localhost:8080/api/v1/mock-external")
                .build();
    }

    public String callExternalService(boolean shouldFail) {

        try {

            String endpoint = shouldFail
                    ? "/failure"
                    : "/success";

            return restClient
                    .get()
                    .uri(endpoint)
                    .retrieve()
                    .body(String.class);

        } catch (Exception ex) {

            throw new ExternalServiceException(
                    "External service call failed",
                    ex
            );
        }
    }
}