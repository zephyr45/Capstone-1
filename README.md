# SecureAuth — JWT Authentication & Authorization System

A production-style authentication and authorization system built using **Spring Boot, Spring Security, JWT, PostgreSQL and HttpOnly Cookies**.

The system supports secure user registration, login, JWT-based authentication, role-based authorization, refresh-token rotation, account lockout, rate limiting, session management and secure logout.

---

##  Technology Stack

* Java
* Spring Boot
* Spring Security
* JWT — JJWT
* PostgreSQL
* Spring Data JPA / Hibernate
* BCrypt Password Encoding
* SLF4J + Logback
* Resilience4j / Circuit Breaker
* Next.js frontend
* Axios with credentials

---

# 🏗️ Architecture

```text
                    Next.js Frontend
                          │
                          │ HTTP + Cookies
                          ▼
              ┌─────────────────────────┐
              │      Spring Boot        │
              │                         │
              │   Spring Security       │
              │   JWT Authentication    │
              │   Role Authorization    │
              └───────────┬─────────────┘
                          │
          ┌───────────────┼────────────────┐
          ▼               ▼                ▼
     PostgreSQL       TokenStore       Security
     User Data        In-Memory        Services
                      Tokens
                          │
                          ▼
                  USER / ADMIN APIs
```

---

#  Main Features

### Authentication

* User signup
* Username/password login
* BCrypt password hashing
* JWT access token
* JWT refresh token
* HttpOnly cookie-based token storage
* Stateless Spring Security authentication

### Authorization

* USER and ADMIN roles
* Role-based endpoint protection
* Spring Security authorities
* ADMIN-only APIs
* USER/ADMIN shared APIs

### Token Security

* Signed JWTs
* Access token expiration
* Refresh token expiration
* Refresh-token rotation
* Reuse prevention
* Server-side token validation using in-memory TokenStore
* SHA-256 hashing of tokens inside TokenStore

### Account Security

* Login rate limiting
* Failed-login tracking
* Temporary account lockout
* Account active/inactive status
* Secure logout

### Session Security

* HttpOnly cookies
* SameSite cookie protection
* Secure cookie support for HTTPS
* Stateless backend sessions
* Frontend inactivity timeout

### Reliability & Monitoring

* Global exception handling
* Structured application logging
* Database circuit breaker for the login user lookup
* Authentication event logging

---

#  Project Structure

```text
src/main/java/com/hdfc/jwtauth
│
├── config
│   ├── SecurityConfig
│   └── CorsConfig
│
├── controller
│   ├── AuthController
│   ├── UserDashboardController
│   └── AdminDashboardController
│
├── dto
│   ├── LoginRequest
│   ├── RegisterRequest
│   ├── LoginResponse
│   └── ApiResponse
│
├── entity
│   └── User
│
├── repository
│   └── UserRepository
│
├── security
│   ├── JwtService
│   ├── JwtAuthenticationFilter
│   ├── TokenStore
│   ├── CookieService
│   ├── LoginAttemptService
│   ├── LoginRateLimitGuard
│   ├── LoginRateLimitKeyResolver
│   ├── CustomAuthenticationEntryPoint
│   └── CustomAccessDeniedHandler
│
├── service
│   └── ExternalLoginService
│
└── exception
    └── GlobalExceptionHandler
```

---

# ️ Database

PostgreSQL stores **user account information**.

Example:

```text
users
------------------------------------------------
id
username
password
role
active
------------------------------------------------
```

Passwords are never stored as plain text.

They are stored using:

```text
BCrypt
```

Policy and claim dashboard data can remain in-memory as required by the application.

---

# 🔑 Authentication APIs

Base URL:

```text
http://localhost:8080
```

---

## 1. Signup

### Request

```http
POST /api/v1/auth/signup
Content-Type: application/json
```

```json
{
  "username": "sachin",
  "password": "Sachin@123",
  "confirmPassword": "Sachin@123"
}
```

### Working

1. Validate request.
2. Check whether username already exists.
3. Validate password confirmation.
4. Hash password using BCrypt.
5. Create user with `USER` role.
6. Set account as active.
7. Save user in PostgreSQL.

### Response

```json
{
  "message": "Account created successfully"
}
```

Public signup cannot create an ADMIN account.

---

# 2. Login

```http
POST /api/v1/auth/login
Content-Type: application/json
```

```json
{
  "username": "sachin",
  "password": "Sachin@123"
}
```

### Login Flow

```text
Username + Password
        ↓
Find user in PostgreSQL
        ↓
Check account status
        ↓
BCrypt password verification
        ↓
Generate Access JWT
        ↓
Generate Refresh JWT
        ↓
Store token hashes in TokenStore
        ↓
Set HttpOnly cookies
        ↓
Return login response
```

### Response

```json
{
  "message": "Login successful",
  "username": "sachin",
  "roles": [
    "USER"
  ]
}
```

Tokens are **not returned in JSON**.

They are stored in:

```text
HttpOnly Cookies
```

---

# 3. Get Current User

```http
GET /api/v1/auth/me
```

Requires authentication.

Example response:

```json
{
  "username": "sachin",
  "roles": [
    "USER"
  ],
  "status": "ACTIVE",
  "accountType": "USER",
  "authenticated": true
}
```

This API retrieves the authenticated user's current information from the backend.

---

# 4. Authentication Check

```http
GET /api/v1/auth/auth
```

Returns the authenticated principal and authorities.

Example:

```json
{
  "authenticated": true,
  "username": "sachin",
  "roles": [
    "USER"
  ]
}
```

---

# 5. Refresh Token

```http
POST /api/v1/auth/refresh
```

The browser automatically sends the `REFRESH_TOKEN` cookie.

### Working

```text
Refresh Token
      ↓
Check TokenStore
      ↓
Verify JWT signature
      ↓
Check token type
      ↓
Check expiration
      ↓
Find user
      ↓
Remove old refresh token
      ↓
Generate new Access Token
      ↓
Generate new Refresh Token
      ↓
Store new tokens
      ↓
Set new HttpOnly cookies
```

This implements **refresh-token rotation**.

An old refresh token cannot be reused after rotation.

---

# 6. Logout

```http
POST /api/v1/auth/logout
```

### Working

```text
Receive cookies
      ↓
Remove access token from TokenStore
      ↓
Remove refresh token from TokenStore
      ↓
Clear ACCESS_TOKEN cookie
      ↓
Clear REFRESH_TOKEN cookie
```

After logout:

```text
/me              → 401
/refresh         → 401
Protected APIs   → 401
```

---

# 👤 User APIs

## User Dashboard

```http
GET /user/dashboard
```

Accessible by:

```text
USER
ADMIN
```

Example:

```json
{
  "welcomeMessage": "Welcome, Sachin",
  "policies": 2,
  "claims": 2,
  "profile": "ACTIVE"
}
```

---

# 👑 Admin APIs

## Admin Dashboard

```http
GET /admin/dashboard
```

Accessible only by:

```text
ADMIN
```

Example:

```json
{
  "welcomeMessage": "Welcome, Admin",
  "totalUsers": 10,
  "activeUsers": 8,
  "totalPolicies": 12,
  "activePolicies": 10,
  "totalClaims": 15,
  "pendingClaims": 10
}
```

A USER attempting to access this endpoint receives:

```text
403 Forbidden
```

---

# 🔐 JWT Structure

The access token contains minimal claims:

```json
{
  "sub": "sachin",
  "roles": [
    "USER"
  ],
  "type": "access",
  "iat": "...",
  "exp": "..."
}
```

Refresh token:

```json
{
  "sub": "sachin",
  "type": "refresh",
  "iat": "...",
  "exp": "..."
}
```

---

# ✍️ JWT Signing & Verification

JWTs are digitally signed using an HMAC secret key.

During generation:

```text
Header + Payload
       ↓
     HMAC
       ↓
   Signature
```

During authentication:

```text
Incoming JWT
     ↓
Verify Signature
     ↓
Valid?
 ┌───┴────┐
Yes       No
 ↓         ↓
Claims    401
 ↓
Authentication
```

The signature prevents users from modifying claims such as:

```text
USER → ADMIN
```

without invalidating the token.

JWT payloads are encoded, not encrypted, so sensitive information is intentionally not stored inside the JWT.

---

# 🍪 Cookie Security

JWTs are stored in:

```text
ACCESS_TOKEN
REFRESH_TOKEN
```

Cookies are configured with:

```text
HttpOnly
SameSite
Secure (enabled in HTTPS production)
Path=/
```

### Why HttpOnly?

JavaScript cannot directly read the JWT using:

```javascript
document.cookie
```

This reduces exposure to token theft through client-side JavaScript.

---

# 🛡️ TokenStore

The server maintains active token state in memory.

Tokens are stored as SHA-256 hashes rather than raw JWT values.

```text
JWT
 ↓
SHA-256
 ↓
Hash
 ↓
TokenStore
```

The actual JWT remains inside the browser cookie.

TokenStore supports:

```text
save()
isActive()
username()
remove()
```

This allows immediate server-side token revocation.

---

# 🚨 Rate Limiting

The login endpoint is protected by a rate limiter.

Example:

```text
Request 1 → Allowed
Request 2 → Allowed
Request 3 → Allowed
Request 4 → Allowed
Request 5 → Allowed
Request 6 → Allowed
Request 7 → 429 Too Many Requests
```

This reduces brute-force login attempts.

---

# 🔒 Account Lockout

Failed login attempts are tracked.

After the configured number of failures:

```text
Account
   ↓
LOCKED
   ↓
HTTP 423 Locked
```

Example:

```text
Too many failed login attempts.
Account locked for 5 minutes.
```

Successful authentication resets the failed-attempt counter.

---

# 🔄 Refresh Token Rotation

The application does not continuously reuse the same refresh token.

```text
Refresh Token A
       ↓
     /refresh
       ↓
Remove Token A
       ↓
Generate Token B
```

Trying to reuse Token A:

```text
401 Unauthorized
Refresh token is invalid or already used
```

This reduces the impact of refresh-token theft.

---

# ⏱️ Session / Inactivity Handling

The frontend maintains an inactivity timer.

Example:

```text
User active
    ↓
No activity
    ↓
5 minutes
    ↓
Session expired
    ↓
Logout
    ↓
Login page
```

The refresh endpoint should only be used while the user is considered active.

Refresh tokens are not intended to silently keep an inactive user logged in forever.

---

# 🛡️ Spring Security Flow

Every protected request follows:

```text
HTTP Request
     ↓
JwtAuthenticationFilter
     ↓
Read ACCESS_TOKEN cookie
     ↓
Check TokenStore
     ↓
Verify JWT
     ↓
Read username + roles
     ↓
Create Authentication
     ↓
Spring Security Authorization
     ↓
Controller
```

Example:

```text
USER → /admin/dashboard
             ↓
        ADMIN required
             ↓
        USER != ADMIN
             ↓
        403 Forbidden
```

---

# 🌐 CORS

The backend supports the Next.js frontend through CORS.

Configured for:

```text
http://localhost:3000
```

Credentials are enabled because authentication uses cookies.

Frontend Axios requests use:

```javascript
withCredentials: true
```

---

# 🧯 Exception Handling

A global exception handler provides consistent API error responses.

Examples:

```text
400 → Bad Request
401 → Unauthorized
403 → Forbidden
409 → Conflict
423 → Locked
429 → Too Many Requests
500 → Internal Server Error
```

---

# 📝 Logging

SLF4J + Logback are used for authentication and application logging.

Important events include:

```text
SIGNUP_SUCCESS
LOGIN_SUCCESS
LOGIN_FAILED
LOGIN_BLOCKED
ACCOUNT_LOCKED
TOKEN_REFRESH
LOGOUT_SUCCESS
```

Logs help with debugging, monitoring and security auditing.

---

# 🔌 Database Circuit Breaker

The login-specific PostgreSQL lookup is protected by the Resilience4j instance
`databaseCircuitBreakerCore`:

```text
POST /api/v1/auth/login
       ↓
LoginRateLimitGuard
       ↓
Account lock check
       ↓
UserService.findUserForLogin() @CircuitBreaker
       ↓
UserRepository.findByUsername()
       ↓
PostgreSQL
       ↓
ExternalLoginService credential validation
       ↓
JWT and authentication cookies
```

Only the login lookup is protected. Registration, refresh-token processing,
profile lookup, logout, authorization, and the rate limiter remain outside this
breaker. When PostgreSQL failures reach the configured threshold, the breaker
opens and later login requests receive `503 Service Unavailable` without another
database lookup. Database calls that actually fail return `Database unavailable`;
calls rejected by the open breaker return `Database circuit breaker is OPEN`.

The current state is available at:

```bash
curl http://localhost:8080/api/v1/test/circuit-state
```

### Demonstrating PostgreSQL failure and recovery

The committed rate limit permits six requests per minute so the five database
failures and the first open-circuit rejection can be observed through the login
endpoint.

With PostgreSQL running, the state endpoint reports `CLOSED`. In an elevated
PowerShell terminal, discover and stop the installed PostgreSQL service:

```powershell
Get-Service -Name '*postgres*'
Stop-Service -Name '<postgres-service-name>'
```

Send five login requests. Each database failure returns `503 Database
unavailable`, and the fifth failure opens the breaker. Request six returns `503
Database circuit breaker is OPEN` without another PostgreSQL call, and the state
endpoint reports `OPEN`.

Restart PostgreSQL and wait at least 30 seconds:

```powershell
Start-Service -Name '<postgres-service-name>'
```

The state endpoint reports `HALF_OPEN`. Three successful login requests are the
configured recovery probes; after they succeed, the state returns to `CLOSED`.
There is intentionally no endpoint that stops or simulates failure of the database.

---

# 🔐 Security Design Summary

| Security Requirement    | Implementation           |
| ----------------------- | ------------------------ |
| Password security       | BCrypt                   |
| Authentication          | JWT                      |
| Authorization           | Spring Security          |
| Roles                   | USER / ADMIN             |
| Token storage           | HttpOnly cookies         |
| Token integrity         | JWT signature            |
| Token revocation        | In-memory TokenStore     |
| TokenStore protection   | SHA-256 hashing          |
| Access token            | 30 minutes               |
| Refresh token           | 7 days                   |
| Refresh security        | Rotation                 |
| Brute-force protection  | Rate limiting            |
| Failed login protection | Account lockout          |
| Browser token access    | HttpOnly                 |
| Cross-origin security   | CORS                     |
| Session state           | Stateless                |
| Error handling          | Global Exception Handler |
| Monitoring              | SLF4J + Logback          |
| Database failures       | Circuit Breaker          |

---

# 🧪 API Testing Flow

### 1. Signup

```bash
curl -i -X POST http://localhost:8080/api/v1/auth/signup \
-H "Content-Type: application/json" \
-d '{
  "username":"sachin",
  "password":"Sachin@123",
  "confirmPassword":"Sachin@123"
}'
```

### 2. Login

```bash
curl -i -c cookies.txt \
-X POST http://localhost:8080/api/v1/auth/login \
-H "Content-Type: application/json" \
-d '{
  "username":"sachin",
  "password":"Sachin@123"
}'
```

### 3. Check User

```bash
curl -i -b cookies.txt \
http://localhost:8080/api/v1/auth/me
```

### 4. User Dashboard

```bash
curl -i -b cookies.txt \
http://localhost:8080/user/dashboard
```

### 5. Admin Dashboard

```bash
curl -i -b cookies.txt \
http://localhost:8080/admin/dashboard
```

A USER should receive:

```text
403 Forbidden
```

### 6. Refresh

```bash
curl -i -b cookies.txt -c cookies.txt \
-X POST http://localhost:8080/api/v1/auth/refresh
```

### 7. Logout

```bash
curl -i -b cookies.txt -c cookies.txt \
-X POST http://localhost:8080/api/v1/auth/logout
```

---

# 🔄 Complete Application Flow

```text
                    SIGNUP
                      │
                      ▼
               PostgreSQL User
                      │
                      ▼
                    LOGIN
                      │
          ┌───────────┴───────────┐
          ▼                       ▼
   Access Token              Refresh Token
   30 minutes                   7 days
          │                       │
          └───────────┬───────────┘
                      ▼
               HttpOnly Cookies
                      │
                      ▼
             Protected API Request
                      │
                      ▼
             JWT Authentication
                      │
                      ▼
             Role Authorization
                      │
             ┌────────┴────────┐
             ▼                 ▼
           USER              ADMIN
             │                 │
             ▼                 ▼
       User Dashboard    Admin Dashboard
                      │
                      ▼
              Access Token Expires
                      │
                      ▼
                   /refresh
                      │
                      ▼
              Token Rotation
                      │
                      ▼
              Continue Session
                      │
                      ▼
                    LOGOUT
                      │
                      ▼
             Tokens Revoked
                      │
                      ▼
             Cookies Cleared
```

---

# 🎯 Project Objective

The objective of SecureAuth is to demonstrate how a modern authentication system can be designed beyond simple username/password login.

The project combines:

* Authentication
* Authorization
* JWT
* Secure cookies
* Token rotation
* Token revocation
* Rate limiting
* Account lockout
* Session management
* Logging
* Exception handling
* Resilience
* PostgreSQL user management

into one complete Spring Boot security application.

---

# 👨‍💻 Project Highlights

The key differentiators of the project are:

1. **JWT tokens are not exposed through the login response.**
2. **Tokens are stored using HttpOnly cookies.**
3. **Refresh tokens are rotated after use.**
4. **Refresh-token reuse is rejected.**
5. **Active tokens can be revoked server-side.**
6. **JWTs stored in TokenStore are SHA-256 hashed.**
7. **Login brute-force attempts are rate limited.**
8. **Repeated failed logins temporarily lock accounts.**
9. **USER and ADMIN access is enforced by Spring Security.**
10. **Frontend inactivity handling is integrated with backend logout.**
11. **Authentication events are logged.**
12. **Login database failures are handled through a circuit breaker.**

---

# 📌 Important Security Note

JWT payloads are **encoded, not encrypted**.

Therefore, the JWT should contain only non-sensitive information such as:

```text
username
roles
token type
issued time
expiration time
```

Sensitive information such as:

```text
password
phone number
address
financial information
```

should never be placed inside the JWT.

Production deployment should use:

```text
HTTPS
Secure cookies
HttpOnly cookies
Strong JWT signing secret
Environment-based secrets
```

---

# 📄 Final API Summary

| Method | Endpoint               | Access        |
| ------ | ---------------------- | ------------- |
| POST   | `/api/v1/auth/signup`  | Public        |
| POST   | `/api/v1/auth/login`   | Public        |
| POST   | `/api/v1/auth/refresh` | Refresh Token |
| GET    | `/api/v1/auth/me`      | Authenticated |
| GET    | `/api/v1/auth/auth`    | Authenticated |
| POST   | `/api/v1/auth/logout`  | Authenticated |
| GET    | `/user/dashboard`      | USER / ADMIN  |
| GET    | `/admin/dashboard`     | ADMIN         |

---

## 🏁 Conclusion

**SecureAuth** demonstrates a complete authentication lifecycle:

```text
Register
   ↓
Authenticate
   ↓
Issue JWT
   ↓
Secure Cookies
   ↓
Authorize
   ↓
Refresh
   ↓
Rotate Tokens
   ↓
Monitor Session
   ↓
Revoke
   ↓
Logout
```

The project is designed to demonstrate practical **Spring Security and JWT concepts using a production-oriented architecture**, rather than implementing only a basic login API.
