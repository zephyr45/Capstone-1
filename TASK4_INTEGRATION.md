# Task4 Changes

## What this work does

Task4 starts from the existing task3 branch. It keeps the task3 login, JWT, refresh-token, logout, dashboard, and security behavior. It adds the Task2 Resilience4j login protection.

The login flow is now:

```text
POST /login
  -> existing account-lockout check
  -> LoginRateLimiter
  -> ExternalLoginService
  -> LoginCircuitBreaker
  -> mock external login check
  -> JwtTokenProvider
  -> InMemoryTokenRepository
  -> LoginResponse
```

The account-lockout check remains first because it was already part of task3. The rate limiter still runs before credential validation.

## Folder and package refactor

The Java package root changed from:

```text
com.hdfc.jwtauth
```

to:

```text
com.hdfclife.smartauth
```

Existing files were moved into the requested folders:

| Old location or name | New location or name |
|---|---|
| `controllers` | `controller` |
| `services` | `service` |
| `entity` | `model` |
| `exceptions` | `exception` |
| `web/LoginRequest.java` | `dto/request/LoginRequest.java` |
| `web/*Response.java` | `dto/response/` |
| `JwtAuthApplication.java` | `SmartAuthApplication.java` |
| `AdminDashBoardController.java` | `AdminDashboardController.java` |
| `UserDashBoardController.java` | `DashboardController.java` |
| `UserAccount.java` | `model/User.java` |
| `InMemoryUserService.java` | `repository/InMemoryUserRepository.java` |
| `InMemoryPolicyService.java` | `repository/InMemoryPolicyRepository.java` |
| `InMemoryClaimService.java` | `repository/InMemoryClaimRepository.java` |
| `security/TokenStore.java` | `repository/InMemoryTokenRepository.java` |
| `services/JwtService.java` | `security/JwtTokenProvider.java` |
| `logging/RequestLoggingFilter.java` | `util/RequestLoggingFilter.java` |

Package declarations and imports were updated to match the new locations. The code inside these classes was not redesigned.

## Resilience4j changes

Added configuration classes:

- `config/RateLimitProperties.java`
- `config/CircuitBreakerProperties.java`
- `config/ResilienceConfig.java`

Added resilience classes:

- `resilience/LoginRateLimiter.java`
- `resilience/LoginCircuitBreaker.java`
- `resilience/LoginFallbackHandler.java`

Added:

- `service/ExternalLoginService.java`
- `exception/RateLimitExceededException.java`

### Rate limiter

The limiter uses one bucket for each username and client IP address. It allows five requests per minute. A rejected request returns HTTP `429 Too Many Requests`.

The real peer address is used. An untrusted `X-Forwarded-For` header is not accepted because it could be changed by a caller to bypass the limit.

### Circuit breaker

The circuit breaker protects the mock external login call.

- External service failures count as circuit failures.
- Invalid credentials do not count as circuit failures.
- An open circuit does not call the external service.
- External failures and open-circuit requests return HTTP `503 Service Unavailable`.

The mock behavior is unchanged:

- `user / password` succeeds.
- `serviceDown` simulates an external failure.
- Existing task3 users continue to work with their existing passwords.
- Other credentials return HTTP `401 Unauthorized`.

## Existing functionality kept

The following behavior was kept:

- `POST /login`
- `POST /refresh`
- `GET /auth`
- `POST /logout`
- JWT signature and expiry validation
- In-memory token storage and logout revocation
- Role-based access for user and admin endpoints
- Existing account lockout
- Existing dashboard, policy, claim, and logging behavior
- Existing API response shapes

The resilience classes do not create JWTs and do not store tokens. The existing token code still performs those jobs.

## Exception responses

| Error | Status |
|---|---:|
| Invalid credentials | 401 |
| Rate limit exceeded | 429 |
| External service failed | 503 |
| Circuit breaker is open | 503 |

The existing `GlobalExceptionHandler` was moved to the singular `exception` package and kept as the single exception handler.

## Files changed by the folder refactor

All existing Java files were moved under `src/main/java/com/hdfclife/smartauth/`. The main folders are:

```text
config/
controller/
service/
repository/
model/
dto/request/
dto/response/
security/
exception/
resilience/
util/
```

Tests were moved under:

```text
src/test/java/com/hdfclife/smartauth/
```

The tests now use the new package names and are grouped under `controller/` and `resilience/`.

## Files intentionally not added

The requested example tree contains some classes that did not exist in the original code, including `AuthService`, `JwtConfig`, `OpenApiConfig`, `DashboardService`, `ExternalServiceController`, `CustomUserDetails`, `SecurityUserService`, `Role`, `LogoutRequest`, and `ErrorResponse`.

These files were not added as empty placeholders. Adding them would create unused code or require a business-logic redesign, which would conflict with the instruction to change only the folder structure.

The existing equivalents were moved into the closest requested locations instead.

## Configuration and dependencies

`application.yml` still contains the original server, logging, security, and resilience settings. The logging package name was updated to `com.hdfclife.smartauth`.

`pom.xml` was not replaced. The existing Resilience4j dependencies were already sufficient.

## Verification

The following command passes:

```bash
mvn test
```

All five tests pass. They verify Spring startup, login, JWT authentication, refresh, logout, rate limiting, circuit opening, open-circuit rejection, and the 401/503 responses.

## Git status

- Base branch: `task3`
- Working branch: `task4`
- Repository: `https://github.com/zephyr45/Capstone1task1branch`
- Task3 was not changed.

This refactor changes file locations, package names, and structural class names only. It does not redesign the application behavior.
