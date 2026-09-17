# Frontend-ready backend changes

This branch prepares the Spring Boot authentication backend for a Next.js/React frontend.

## What changed

- Added configurable CORS for `FRONTEND_ORIGIN`, defaulting to `http://localhost:3000`. `Authorization`, `Content-Type`, and preflight requests are supported.
- Added `@NotBlank` validation for login username/password. Missing or malformed input now returns HTTP 400 instead of HTTP 500.
- Added one shared `ErrorResponse` shape for controller, validation, authentication, authorization, and resilience errors:

  ```json
  {
    "timestamp": "2026-09-17T17:00:00Z",
    "status": 401,
    "error": "Unauthorized",
    "message": "Invalid or expired token",
    "path": "/auth"
  }
  ```

- Added a `sessionId` claim to access/refresh token pairs. Logout now revokes every token in that session, including the refresh token.
- Restricted normal bearer authentication to access tokens. Refresh tokens are accepted only by `POST /refresh`.
- Moved JWT secret and expiration settings to `app.jwt` configuration with environment-variable overrides:
  - `JWT_SECRET`
  - `JWT_ACCESS_TOKEN_EXPIRATION`
  - `JWT_REFRESH_TOKEN_EXPIRATION`
- Removed duplicate unused Resilience4j instance configuration. The `login.*` settings are now the single source of truth.
- Added scheduled cleanup of inactive per-username/IP rate-limiters to prevent unbounded process-local growth.
- Corrected the Logback package logger from `com.hdfclife.resilience` to `com.hdfclife.smartauth`.
- Added `openapi.yaml` as the frontend-facing API contract.
- Added regression coverage for refresh-token rejection, full logout, malformed input, and CORS preflight.

## Frontend integration

Base URL for local development: `http://localhost:8080`

Store the `accessToken` and `refreshToken` returned by `POST /login`. Attach the access token as:

```text
Authorization: Bearer <accessToken>
```

Use the refresh token only for `POST /refresh`. A successful refresh rotates the refresh token, so replace both stored tokens with the response values. On logout, call `POST /logout` with the access token and then clear both tokens locally.

The backend does not implement the assignment’s five-second inactivity timer. The frontend should track activity, clear local tokens, call logout where possible, and redirect to the login page after five seconds without activity.

Handle these statuses:

| Status | Meaning |
|---:|---|
| 400 | Invalid/malformed request |
| 401 | Missing, invalid, expired, inactive, or wrong token type |
| 403 | Authenticated but insufficient role |
| 423 | Account temporarily locked |
| 429 | Login rate limit exceeded |
| 503 | External authentication service unavailable/circuit open |

## Configuration

```yaml
app:
  cors:
    allowed-origins:
      - ${FRONTEND_ORIGIN:http://localhost:3000}
  jwt:
    secret: ${JWT_SECRET:change-this-development-secret}
    access-token-expiration: ${JWT_ACCESS_TOKEN_EXPIRATION:30m}
    refresh-token-expiration: ${JWT_REFRESH_TOKEN_EXPIRATION:7d}
```

The default secret exists only to make local assignment startup easy. Set `JWT_SECRET` to a strong secret outside development. Token/session state remains intentionally in memory and is cleared on restart; it is not shared between multiple backend instances.

## Verification

`mvn test` passes 7 tests, including the new browser and session-security regression cases.
