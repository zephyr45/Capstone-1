# JWT Authentication API — Spring Boot + Spring Security

No database. Users and active JWT sessions are stored in memory.

## Requirements
- Java 17+
- Maven 3.9+

## Run
```bash
mvn clean spring-boot:run
```

Server:
`http://localhost:8080`

## Hardcoded users

| Username | Password | Roles |
|---|---|---|
| user | password | USER |
| sachin | sachin123 | USER |
| admin | admin123 | USER, ADMIN |

## APIs

### 1. Login
```bash
curl -X POST http://localhost:8080/login   -H "Content-Type: application/json"   -d '{"username":"sachin","password":"sachin123"}'
```

Copy the `accessToken` from the response.

### 2. Auth
```bash
curl http://localhost:8080/auth   -H "Authorization: Bearer YOUR_TOKEN"
```

### 3. User endpoint
```bash
curl http://localhost:8080/user/profile   -H "Authorization: Bearer YOUR_TOKEN"
```

### 4. Admin endpoint
```bash
curl http://localhost:8080/admin/dashboard   -H "Authorization: Bearer YOUR_TOKEN"
```

A `sachin` token receives 403 here. An `admin` token can access it.

### 5. Logout
```bash
curl -X POST http://localhost:8080/logout   -H "Authorization: Bearer YOUR_TOKEN"
```

After logout, the same JWT is rejected because it has been removed from the in-memory InMemoryTokenRepository.

## Architecture

The Java code is under `com.hdfclife.smartauth` and is organized into:

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

Login uses:

`AuthController -> LoginRateLimiter -> ExternalLoginService -> LoginCircuitBreaker -> JwtTokenProvider -> InMemoryTokenRepository`

Protected requests use:

`Spring Security Filter Chain -> JwtAuthenticationFilter -> JwtTokenProvider + InMemoryTokenRepository`

- JWT is signed with an HMAC secret.
- JWT contains username, roles, issued-at and expiration.
- `InMemoryTokenRepository` is the session/revocation layer.
- Spring Security is stateless.
- Role checks use `hasRole("USER")` / `hasRole("ADMIN")`.
- Global exception handling uses `@RestControllerAdvice`.
- SLF4J + Spring Boot's Logback setup logs requests, responses and errors.

For a simple explanation of every refactor and the files that moved, see
[`TASK4_INTEGRATION.md`](TASK4_INTEGRATION.md).

## Important

This is intentionally database-free for the assignment. The in-memory token store is also cleared whenever the application restarts, so previously issued tokens become invalid even if their JWT expiry has not elapsed.

## Frontend integration

The frontend handoff is documented in [FRONTEND_READY.md](FRONTEND_READY.md), and the machine-readable API contract is in [openapi.yaml](openapi.yaml).

For local Next.js development, the backend allows `http://localhost:3000`. Set `FRONTEND_ORIGIN` when the frontend runs on another origin. Set `JWT_SECRET` in any non-development environment.
