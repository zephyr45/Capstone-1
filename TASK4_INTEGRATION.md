# Task4 Integration Documentation

## Objective

Task4 starts from the existing `task3` branch and integrates the Task2 Resilience4j implementation without replacing the task3 authentication architecture.

The resulting flow is:

```text
POST /login
  -> existing account-lockout check
  -> LoginRateLimiter
  -> ExternalLoginService
  -> LoginCircuitBreaker
  -> mock external login validation
  -> existing JwtService
  -> existing TokenStore
  -> existing LoginResponse
```

Resilience4j is used only for the login/external-authentication dependency. JWT validation, protected endpoints, refresh tokens, logout, role authorization, dashboards, and token storage remain task3 responsibilities.

## Task3 architecture preserved

The task3 application uses the `com.hdfc.jwtauth` package hierarchy and contains:

- `AuthController` for login, refresh, authentication, and logout.
- `JwtService` for access-token and refresh-token generation and validation.
- `TokenStore` for active in-memory token tracking and logout revocation.
- `JwtAuthenticationFilter` for JWT extraction, verification, active-token checks, and authority creation.
- `SecurityConfig` for public and protected endpoint rules.
- `LoginAttemptService` for existing failed-login tracking and account lockout.
- Existing dashboard, policy, claim, user, logging, and exception-handling components.

No parallel application package tree was introduced.

## Changes added

### Resilience configuration

Added:

- `src/main/java/com/hdfc/jwtauth/config/RateLimitProperties.java`
- `src/main/java/com/hdfc/jwtauth/config/CircuitBreakerProperties.java`
- `src/main/java/com/hdfc/jwtauth/config/ResilienceConfig.java`

The existing `pom.xml` already contained compatible Spring Cloud CircuitBreaker and Resilience4j dependencies, so it was not replaced or unnecessarily changed.

### Rate limiter

Added `LoginRateLimiter` in:

`src/main/java/com/hdfc/jwtauth/resilience/LoginRateLimiter.java`

It maintains an independent Resilience4j limiter for each `username + client IP` pair. The configured limit is five requests per minute with no waiting. Rejected requests throw `RateLimitExceededException` and are returned as HTTP `429 Too Many Requests`.

The client address uses `HttpServletRequest.getRemoteAddr()`. Untrusted `X-Forwarded-For` headers are not used, preventing callers from bypassing the limiter by supplying different header values.

### Circuit breaker and fallback

Added:

- `LoginCircuitBreaker`
- `LoginFallbackHandler`
- `ExternalLoginService`

The circuit breaker records `ExternalServiceException` failures and ignores `InvalidCredentialsException`. When the circuit is open, the external operation is not called and the request is converted into an external-service failure handled as HTTP `503 Service Unavailable`.

The mock external service supports:

- `user / password`: required mock credentials.
- `serviceDown / anything`: simulated external-service failure.
- Existing task3 users and their existing passwords: retained through a small adapter so task3 login behavior continues to work.
- Other credentials: invalid credentials, returned as HTTP `401 Unauthorized`.

### Login integration

`AuthController` was changed only at the login integration point:

1. Existing account lockout is checked.
2. The request is rate-limited.
3. Credential validation is delegated to `ExternalLoginService`.
4. Existing failed-attempt tracking is retained for invalid credentials.
5. Existing `JwtService` generates the access and refresh tokens.
6. Existing `TokenStore` stores both tokens.
7. Existing `LoginResponse` is returned.

Refresh, authentication, logout, and protected-resource behavior were not redesigned.

### Exception handling

The existing `GlobalExceptionHandler` was extended with the rate-limit mapping. The final mappings are:

| Exception | HTTP status | Meaning |
|---|---:|---|
| `InvalidCredentialsException` | 401 | Credentials are invalid |
| `RateLimitExceededException` | 429 | Username/IP request limit exceeded |
| `ExternalServiceException` | 503 | Mock service failed or circuit is open |

## Configuration

The existing `application.yml` was preserved and extended with:

```yaml
login:
  rate-limit:
    limit-for-period: 5
    limit-refresh-period: 1m
    timeout-duration: 0s

  circuit-breaker:
    failure-rate-threshold: 50
    slow-call-rate-threshold: 100
    wait-duration-in-open-state: 30s
    slow-call-duration-threshold: 2s
    permitted-number-of-calls-in-half-open-state: 3
    minimum-number-of-calls: 5
    sliding-window-size: 10
```

Existing server, application, logging, security, and JWT-related settings were retained.

## Files modified

- `src/main/java/com/hdfc/jwtauth/controllers/AuthController.java`
- `src/main/java/com/hdfc/jwtauth/exceptions/GlobalExceptionHandler.java`
- `src/main/java/com/hdfc/jwtauth/services/InMemoryUserService.java`
- `src/main/resources/application.yml`

`InMemoryUserService` was extended with the required `user/password` mock account. No existing task3 user was removed or changed.

## Files added

- Resilience configuration classes under `config/`.
- Resilience components under `resilience/`.
- `ExternalLoginService` under `services/`.
- `RateLimitExceededException` under `exceptions/`.
- `src/test/java/com/hdfc/jwtauth/AuthFlowIntegrationTest.java`.
- `src/test/java/com/hdfc/jwtauth/resilience/LoginCircuitBreakerTest.java`.
- `src/test/java/com/hdfc/jwtauth/resilience/LoginRateLimiterTest.java`.

No files were moved or renamed.

## Verification performed

`mvn test` passes all five tests. The tests cover:

- Spring context startup and bean wiring.
- Existing login, JWT authentication, refresh, and logout flow.
- Invalid credentials returning `401`.
- External service failure returning `503`.
- Per-username/IP rate limiting and `429` behavior.
- Circuit breaker opening after external failures.
- Open-circuit rejection without calling the protected dependency.
- Invalid credentials being ignored by circuit-breaker failure metrics.

Manual smoke tests also confirmed successful login, token revocation after logout, invalid credentials, external-service failure, circuit-open fallback, and rate-limit rejection.

## Branch and preservation status

- Base branch: `task3`
- Result branch: `task4`
- Repository: `https://github.com/zephyr45/Capstone1task1branch`
- Task3 itself was not modified.
- The final task4 branch contains task3 functionality plus the Task2 resilience integration.

The in-memory token store and mock external service are intentional assignment behavior. In-memory state is cleared when the application restarts.
