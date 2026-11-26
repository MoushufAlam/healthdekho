# Step 4 – Login with Spring Security and JWT

> **In one line:** a customer logs in with email + password, gets a **token**, and must send that token with every other request. Without a valid token, the API answers **401**.

**Covers:** FR-1A (customer authentication, restrict data to the logged-in customer)

---

## Part 1 – The theory

### 1.1 Two questions every secure app asks

| Question | Name | In our app |
|---|---|---|
| **Who are you?** | Authentication | Email + password checked against the `customer` table |
| **What may you do?** | Authorization | A customer may only see **their own** policies |

**Spring Security** is the Spring library that answers both questions for us.

### 1.2 How Spring Security works: a row of security guards

Every HTTP request passes through a chain of **filters** *before* it reaches our controller. Think of airport security: several checkpoints, and if one says no, you never reach the gate.

```
HTTP request
   │
   ▼
┌──────────────────────── Security Filter Chain ────────────────────────┐
│  CORS filter  →  Bearer-token filter  →  ...  →  Authorization check  │
└───────────────────────────────────────────────────────────────────────┘
   │  (only if every check passes)
   ▼
Controller → Service → Repository → DB
```

We **configure** the chain in `SecurityConfig.java`; we don't write the filters ourselves.

### 1.3 Sessions vs tokens: why JWT?

**Old way (session):** after login, the server remembers you in its memory and gives the browser a cookie with a session ID.
Problem: the server must store every logged-in user, and a second server doesn't know them.

**Our way (JWT token):** after login, the server gives you a **signed token**. You send it back with every request. The server **remembers nothing**; it just checks the signature. This is called **stateless**.

> 💡 **Analogy:** a JWT is like a **cinema wristband**. At the entrance (login) they check your ticket once and put a wristband on you. Inside, staff don't check your ticket again; they just look at the wristband. A fake wristband is spotted because only the cinema has the special stamp (the **secret key**).

### 1.4 What's inside a JWT?

A JWT is three Base64 parts joined by dots: `header.payload.signature`

```
eyJhbGciOiJIUzI1NiJ9 . eyJzdWIiOiIxMDAxIiwiZW1haWwiOi... . 3kF9x...
      header                       payload (claims)            signature
```

Decoded payload of our token:

```json
{
  "iss": "healthdekho",               ← issuer: who made the token
  "sub": "1001",                      ← subject: the CUSTOMER ID
  "email": "aarav.sharma@example.com",
  "iat": 1795000000,                  ← issued at
  "exp": 1795003600                   ← expires (1 hour later)
}
```

- **Anyone can read** the payload (it's only encoded, not encrypted), so we never put passwords in it.
- **Nobody can change** it: the **signature** is made with our secret key (HMAC-SHA256). Change one letter and the signature no longer matches, so the request is rejected.

### 1.5 The rule that protects customer data

> 🔒 **The customer ID always comes from the token (`sub`), never from the URL or request body.**

If the API accepted `GET /api/dashboard?customerId=1002`, anyone could change the number and read someone else's data. Because the ID is inside the signed token, a customer can only ever be **themselves**.

### 1.6 Passwords: BCrypt

We stored passwords as `{bcrypt}$2a$10$...` in Step 2. At login:

```
typed password "Password@123"  ──BCrypt──►  compare with stored hash  ──►  match? ✅ / ❌
```

`PasswordEncoderFactories.createDelegatingPasswordEncoder()` reads the `{bcrypt}` prefix and picks the right algorithm.

### 1.7 Status codes you will see

| Code | Meaning | When |
|---|---|---|
| **200** | OK | Login worked |
| **400** | Bad request | Email missing or not an email |
| **401** | Unauthorized = "I don't know who you are" | Wrong password, no token, fake/expired token |
| **403** | Forbidden = "I know you, but you may not" | Not used (customer-only app, no roles) |

---

## Part 2 – What we built

```
config/
  SecurityConfig.java      ← the filter chain: which URLs are public, stateless, JWT, CORS
  JwtConfig.java           ← secret key + JwtEncoder (makes tokens) + JwtDecoder (checks tokens)
  AppProperties.java       ← typed access to app.jwt.*, app.cors.*, app.ai.* from application.yml
security/
  CustomerUserDetails.java         ← our customer, in the shape Spring Security understands
  CustomerUserDetailsService.java  ← "load customer by email" for Spring Security
  JwtTokenService.java             ← builds and signs the token after login
  CurrentCustomer.java             ← reads the customer id from the token
  JsonSecurityErrorHandler.java    ← 401/403 as clean JSON (same shape as all other errors)
service/AuthService.java           ← login logic
controller/AuthController.java     ← POST /api/auth/login
controller/CustomerController.java ← GET /api/customers/me (needs token)
```

**Library choice:** `spring-boot-starter-oauth2-resource-server` gives us a ready-made, well-tested **Bearer-token filter** and JWT tools (Nimbus). We don't write our own token-parsing filter, which is a common source of security bugs.

### Public vs protected URLs (`SecurityConfig`)

| URL | Access |
|---|---|
| `POST /api/auth/login` | Public |
| `GET /actuator/health` | Public |
| **everything else** | Needs `Authorization: Bearer <token>` |

### The login flow

```
POST /api/auth/login  {email, password}
  │
  ▼
AuthController.login()            @Valid checks email format / not blank  → 400 if bad
  │
  ▼
AuthService.login()
  ├─ AuthenticationManager.authenticate(email, password)
  │     └─ CustomerUserDetailsService.loadUserByUsername(email) → CustomerRepository.findByEmail → DB
  │     └─ BCrypt compares the password with password_hash        → 401 "Invalid email or password"
  │     └─ status must be ACTIVE                                   → 401 "account is not active"
  ├─ JwtTokenService.issue()  → signed token, valid 1 hour
  └─ returns { accessToken, tokenType: "Bearer", expiresAt, customer profile }
```

### Every request after login

```
GET /api/dashboard   Header: Authorization: Bearer eyJ...
  │
  ▼
Bearer-token filter → JwtDecoder checks signature, expiry, issuer → 401 JSON if invalid
  │
  ▼
Controller gets the token:  @AuthenticationPrincipal Jwt jwt
  CurrentCustomer.id(jwt)  →  1001   (from "sub")
  │
  ▼
Service uses 1001 for every query
```

### The JWT secret

| Setting | Value |
|---|---|
| `JWT_SECRET` env var **set** (≥ 32 characters) | Used to sign tokens; tokens survive restarts |
| **not set** (dev) | A random key is generated at startup; tokens stop working after a restart, so just log in again |
| prod profile | `JWT_SECRET` is **required**; the app refuses to start without it |

Optional, to keep tokens across restarts (run once in PowerShell, then reopen VS Code):

```powershell
[Environment]::SetEnvironmentVariable("JWT_SECRET", [Convert]::ToBase64String((1..48 | % { Get-Random -Max 256 })), "User")
```

### Other details worth knowing

- **Same error for unknown email and wrong password.** Otherwise an attacker could find out which emails are registered.
- **CSRF is disabled.** CSRF attacks abuse cookies; we don't use cookies, so there is nothing to protect.
- **CORS** allows the React app (`localhost:5173` / `3000`) to call the API from the browser.
- **Pattern matching:** `if (!(authentication.getPrincipal() instanceof CustomerUserDetails customer))` checks the type and creates the variable in one step (Java 16+).
- **A record implementing an interface:** `CustomerUserDetails` is a Java `record` that implements Spring's `UserDetails`.

---

## Part 3 – Run and check

```powershell
cd backend
.\mvnw spring-boot:run
```

In Postman (collection `docs/postman/HealthDekho.postman_collection.json`, folder **1. Auth**):

| Request | Expected |
|---|---|
| AUTH-01 Login Aarav | **200**, `accessToken` (saved automatically for later requests) |
| AUTH-02 Wrong password | **401** `"Invalid email or password"` |
| AUTH-04 Invalid body | **400** with `fieldErrors.email` and `fieldErrors.password` |
| AUTH-06 Dashboard without token | **401** `"Authentication required..."` |

Paste the token into **https://jwt.io** to see the payload (`sub` = 1001).

**Automated tests:** `AuthIntegrationTest` (AUTH-01 … AUTH-08) covers success, wrong password, unknown email, invalid body, locked account, missing token, forged token and the public health check.

---

## Part 4 – Screenshots

See `docs/screenshot-checklist.md` → rows **AUTH-01, AUTH-02, AUTH-06** (slides 5 and 6).

---

## Quick recap

- **Spring Security** = a chain of filters in front of every controller; we configure it in `SecurityConfig`.
- **JWT** = signed, stateless "wristband". The server checks the signature and stores nothing.
- **The customer ID comes only from the token** (`sub`), never from the request.
- **BCrypt** checks passwords; the stored value is a one-way hash.
- **Errors:** wrong login → 401, bad input → 400, missing or bad token → 401 (all as JSON).

**Next, Step 5:** the dashboard and the list of available products.
