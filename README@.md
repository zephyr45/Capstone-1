0. Start the application

From the project directory:

cd /Users/SACHIN.M512/Downloads/jwt-auth-springboot-project
mvn spring-boot:run

Application should be running on:

http://localhost:8080
1. Test Public API

This should work without a token.

curl -i http://localhost:8080/public

Expected:

HTTP/1.1 200
2. Login as USER

Use sachin:

curl -i -X POST http://localhost:8080/login \
-H "Content-Type: application/json" \
-d '{"username":"sachin","password":"sachin123"}'

Expected:

{
"message": "Login successful",
"accessToken": "...",
"refreshToken": "...",
"username": "sachin",
"roles": ["USER"]
}
3. Automatically store USER tokens

This is more convenient for all remaining tests.

RESPONSE=$(curl -s -X POST http://localhost:8080/login \
-H "Content-Type: application/json" \
-d '{"username":"sachin","password":"sachin123"}')

ACCESS_TOKEN=$(echo "$RESPONSE" | python3 -c "import sys,json; print(json.load(sys.stdin)['accessToken'])")

REFRESH_TOKEN=$(echo "$RESPONSE" | python3 -c "import sys,json; print(json.load(sys.stdin)['refreshToken'])")

Check:

echo $ACCESS_TOKEN
echo $REFRESH_TOKEN
4. Test /auth

Verify that the JWT is valid and the session is active.

curl -i http://localhost:8080/auth \
-H "Authorization: Bearer $ACCESS_TOKEN"

Expected:

HTTP/1.1 200
{
"authenticated": true,
"username": "sachin",
"roles": ["USER"]
}
5. Test USER Dashboard
   curl -i http://localhost:8080/user/dashboard \
   -H "Authorization: Bearer $ACCESS_TOKEN"

Expected:

{
"policies": 2,
"claims": 2,
"profile": "Active"
}
6. Test USER Profile
   curl -i http://localhost:8080/user/profile \
   -H "Authorization: Bearer $ACCESS_TOKEN"

Expected:

HTTP/1.1 200
7. Test USER accessing ADMIN endpoint

This is an important RBAC test.

curl -i http://localhost:8080/admin/dashboard \
-H "Authorization: Bearer $ACCESS_TOKEN"

Expected:

HTTP/1.1 403
{
"status": 403,
"error": "Forbidden",
"message": "You do not have permission to access this resource"
}

This proves:

USER ❌ → ADMIN API
8. Test ADMIN Login
   ADMIN_RESPONSE=$(curl -s -X POST http://localhost:8080/login \
   -H "Content-Type: application/json" \
   -d '{"username":"admin","password":"admin123"}')

ADMIN_ACCESS_TOKEN=$(echo "$ADMIN_RESPONSE" | python3 -c "import sys,json; print(json.load(sys.stdin)['accessToken'])")

ADMIN_REFRESH_TOKEN=$(echo "$ADMIN_RESPONSE" | python3 -c "import sys,json; print(json.load(sys.stdin)['refreshToken'])")

Check:

echo $ADMIN_ACCESS_TOKEN
echo $ADMIN_REFRESH_TOKEN
9. Test ADMIN /auth
   curl -i http://localhost:8080/auth \
   -H "Authorization: Bearer $ADMIN_ACCESS_TOKEN"

Expected:

{
"authenticated": true,
"username": "admin",
"roles": ["USER", "ADMIN"]
}
10. Test ADMIN Dashboard
    curl -i http://localhost:8080/admin/dashboard \
    -H "Authorization: Bearer $ADMIN_ACCESS_TOKEN"

Expected:

{
"welcomeMessage": "Welcome, Admin",
"totalUsers": 10,
"activeUsers": 8,
"totalPolicies": 12,
"activePolicies": 10,
"totalClaims": 15,
"pendingClaims": 10
}

This verifies:

ADMIN ✅ → ADMIN Dashboard
11. Test Missing Token

Call a protected endpoint without Authorization.

curl -i http://localhost:8080/auth

Expected:

HTTP/1.1 401
{
"status": 401,
"error": "Unauthorized",
"message": "Invalid or expired token"
}
12. Test Invalid Token
    curl -i http://localhost:8080/auth \
    -H "Authorization: Bearer abc.def.xyz"

Expected:

HTTP/1.1 401
13. Test Wrong Username
    curl -i -X POST http://localhost:8080/login \
    -H "Content-Type: application/json" \
    -d '{"username":"wronguser","password":"sachin123"}'

Expected:

HTTP/1.1 401
{
"message": "Invalid username or password"
}
14. Test Wrong Password
    curl -i -X POST http://localhost:8080/login \
    -H "Content-Type: application/json" \
    -d '{"username":"sachin","password":"wrongpassword"}'

Expected:

HTTP/1.1 401
15. Test Access Token as Refresh Token

This verifies that an access token cannot be used to refresh.

curl -i -X POST http://localhost:8080/refresh \
-H "Authorization: Bearer $ACCESS_TOKEN"

Expected:

HTTP/1.1 401
{
"message": "Access token cannot be used as refresh token"
}
16. Test Refresh Token
    curl -i -X POST http://localhost:8080/refresh \
    -H "Authorization: Bearer $REFRESH_TOKEN"

Expected:

HTTP/1.1 200

with:

{
"message": "Token refreshed successfully",
"accessToken": "NEW_ACCESS_TOKEN",
"refreshToken": "NEW_REFRESH_TOKEN",
"username": "sachin",
"roles": ["USER"]
}
17. Store the NEW tokens

After refreshing:

REFRESH_RESPONSE=$(curl -s -X POST http://localhost:8080/refresh \
-H "Authorization: Bearer $REFRESH_TOKEN")

Then:

NEW_ACCESS_TOKEN=$(echo "$REFRESH_RESPONSE" | python3 -c "import sys,json; print(json.load(sys.stdin)['accessToken'])")

NEW_REFRESH_TOKEN=$(echo "$REFRESH_RESPONSE" | python3 -c "import sys,json; print(json.load(sys.stdin)['refreshToken'])")
18. Test NEW Access Token
    curl -i http://localhost:8080/auth \
    -H "Authorization: Bearer $NEW_ACCESS_TOKEN"

Expected:

HTTP/1.1 200
19. Test Refresh Token Rotation

Now try using the OLD refresh token again:

curl -i -X POST http://localhost:8080/refresh \
-H "Authorization: Bearer $REFRESH_TOKEN"

Expected:

HTTP/1.1 401
{
"message": "Refresh token is invalid or already used"
}

This proves:

Refresh Token A
↓
/refresh
↓
Refresh Token A ❌

Refresh Token B ✅
20. Test NEW Refresh Token

The new refresh token should work:

curl -i -X POST http://localhost:8080/refresh \
-H "Authorization: Bearer $NEW_REFRESH_TOKEN"

Expected:

HTTP/1.1 200
21. Test Logout

For logout, use the currently active access token:

curl -i -X POST http://localhost:8080/logout \
-H "Authorization: Bearer $NEW_ACCESS_TOKEN"

Expected:

HTTP/1.1 200
{
"message": "Logout successful"
}
22. Test revoked Access Token

Immediately try the same access token again:

curl -i http://localhost:8080/auth \
-H "Authorization: Bearer $NEW_ACCESS_TOKEN"

Expected:

HTTP/1.1 401
{
"status": 401,
"error": "Unauthorized",
"message": "Invalid or expired token"
}
23. Test malformed Authorization header
    curl -i http://localhost:8080/auth \
    -H "Authorization: abc123"

Expected:

HTTP/1.1 401
24. Test Refresh Without Authorization
    curl -i -X POST http://localhost:8080/refresh

Expected:

HTTP/1.1 400

with:

{
"message": "Bearer refresh token is required"
}
25. Test Logout Without Token
    curl -i -X POST http://localhost:8080/logout

Expected:

HTTP/1.1 400
26. Test logging

While the application is running:

mvn spring-boot:run

you should see logs similar to:

REQUEST POST /login
Login successful for username=sachin
RESPONSE POST /login status=200 duration=...

For failed login:

Login failed for username=sachin

For refresh:

Refresh token rotated for username=sachin

For logout:

Token removed from active session store

And unauthorized/forbidden requests should also be visible in your logs.

Final Testing Matrix

After running everything, your application should satisfy this:

Test	Expected
/public without token	200 ✅
USER login	200 ✅
ADMIN login	200 ✅
/auth with valid USER JWT	200 ✅
/auth with valid ADMIN JWT	200 ✅
USER dashboard	200 ✅
ADMIN dashboard	200 ✅
USER → ADMIN dashboard	403 ✅
No token → protected API	401 ✅
Invalid JWT	401 ✅
Wrong password	401 ✅
Access token as refresh token	401 ✅
Valid refresh token	200 ✅
Old refresh token after rotation	401 ✅
New refresh token	200 ✅
Logout	200 ✅
Revoked access token	401 ✅
Refresh without token	400 ✅
Your complete current flow
┌─────────────┐
│   /login    │
└──────┬──────┘
│
┌─────────┴─────────┐
▼                   ▼
Access Token          Refresh Token
30 min                7 days
│                   │
▼                   ▼
Normal APIs            /refresh
│                   │
│            Rotate tokens
│                   │
│             Old token ❌
│             New token ✅
│
▼
┌─────────────────────┐
│ Spring Security JWT  │
│       + RBAC         │
└──────────┬──────────┘
│
┌───────┴────────┐
▼                ▼
USER             ADMIN
│                │
▼                ▼
/user/dashboard    /admin/dashboard

This covers essentially all the functionality you've built so far, including the newer refresh-token rotation feature.

##Lockout Testing##

Failed login — Attempt 1
curl -i -X POST http://localhost:8080/login \
-H "Content-Type: application/json" \
-d '{"username":"sachin","password":"wrong123"}'

Expected:

HTTP/1.1 401
{
"message": "Invalid username or password"
}
4. Failed login — Attempt 2
   curl -i -X POST http://localhost:8080/login \
   -H "Content-Type: application/json" \
   -d '{"username":"sachin","password":"wrong123"}'

Expected:

HTTP/1.1 401
5. Failed login — Attempt 3
   curl -i -X POST http://localhost:8080/login \
   -H "Content-Type: application/json" \
   -d '{"username":"sachin","password":"wrong123"}'

Expected:

HTTP/1.1 401
6. Failed login — Attempt 4
   curl -i -X POST http://localhost:8080/login \
   -H "Content-Type: application/json" \
   -d '{"username":"sachin","password":"wrong123"}'

Expected:

HTTP/1.1 401
7. Failed login — Attempt 5 🔒
   curl -i -X POST http://localhost:8080/login \
   -H "Content-Type: application/json" \
   -d '{"username":"sachin","password":"wrong123"}'

Expected:

HTTP/1.1 423

Response:

{
"message": "Too many failed login attempts. Account locked for 5 minutes."
}

At this point:

sachin
↓
5 failed attempts
↓
🔒 LOCKED
↓
5 minutes
8. Test correct password while locked

This is the important test.

Even though the password is correct:

curl -i -X POST http://localhost:8080/login \
-H "Content-Type: application/json" \
-d '{"username":"sachin","password":"sachin123"}'

Expected:

HTTP/1.1 423
{
"message": "Account is temporarily locked. Try again later."
}

This proves that the lockout happens before password authentication.

9. Test another user

Make sure locking sachin doesn't lock everyone.

curl -i -X POST http://localhost:8080/login \
-H "Content-Type: application/json" \
-d '{"username":"rahul","password":"rahul123"}'

Expected:

HTTP/1.1 200

So:

sachin → 🔒 locked
rahul  → ✅ works
10. Test that successful login resets failures

If you restart the application, the in-memory lockout data is cleared.

# Stop application
CTRL+C

# Start again
mvn spring-boot:run

Then:

Wrong password
curl -i -X POST http://localhost:8080/login \
-H "Content-Type: application/json" \
-d '{"username":"sachin","password":"wrong123"}'

Expected:

401
Wrong password again
curl -i -X POST http://localhost:8080/login \
-H "Content-Type: application/json" \
-d '{"username":"sachin","password":"wrong123"}'

Expected:

401
Correct password
curl -i -X POST http://localhost:8080/login \
-H "Content-Type: application/json" \
-d '{"username":"sachin","password":"sachin123"}'

Expected:

200

The failed-attempt counter is now reset.

11. Test unknown username

This is also useful:

curl -i -X POST http://localhost:8080/login \
-H "Content-Type: application/json" \
-d '{"username":"hacker","password":"wrong123"}'

Expected:

HTTP/1.1 401
{
"message": "Invalid username or password"
}

Notice that we don't reveal:

"hacker does not exist"

That's intentional—it avoids leaking whether a username exists.

12. Test different usernames

You can also verify that attempts are tracked independently.

curl -i -X POST http://localhost:8080/login \
-H "Content-Type: application/json" \
-d '{"username":"rahul","password":"wrong"}'

Then:

curl -i -X POST http://localhost:8080/login \
-H "Content-Type: application/json" \
-d '{"username":"sahil","password":"wrong"}'

These are separate counters:

sachin → failedAttempts = X
rahul  → failedAttempts = 1
sahil  → failedAttempts = 1
Quick test table
Test	Expected
Correct credentials	200
Wrong password #1	401
Wrong password #2	401
Wrong password #3	401
Wrong password #4	401
Wrong password #5	423 🔒
Correct password while locked	423 🔒
Different user login	200
Unknown username