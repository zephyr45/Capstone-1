package com.hdfc.jwtauth.controllers;

import com.hdfc.jwtauth.resilience.LoginCircuitBreaker;
import com.hdfc.jwtauth.services.ExternalLoginService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/test")
public class TestController {
    private final LoginCircuitBreaker loginCircuitBreaker;

    private final ExternalLoginService externalLoginService;

    public TestController(
            LoginCircuitBreaker loginCircuitBreaker, ExternalLoginService externalLoginService) {
        this.loginCircuitBreaker = loginCircuitBreaker;
        this.externalLoginService = externalLoginService;
    }

    @GetMapping("/circuit-state")
    public ResponseEntity<?> circuitState() {
        return ResponseEntity.ok(
                Map.of(
                        "circuitBreaker", loginCircuitBreaker.getState().name()
                )
        );
    }

    @GetMapping("/external")
    public ResponseEntity<?> testExternalService() {

        String result = externalLoginService.callExternalService();

        return ResponseEntity.ok(result);
    }
    @PostMapping("/external-service/fail")
    public ResponseEntity<?> simulateExternalServiceFailure() throws Exception {

        externalLoginService.simulateExternalServiceFailure();

        return ResponseEntity.ok(
                Map.of(
                        "message", "External service failure simulated"
                )
        );
    }
}