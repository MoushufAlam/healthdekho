# Step 10 – Error Handling and HTTP Status Codes

> **In one line:** every error, from any layer, comes back as the **same clean JSON** with the **right HTTP status code**, never a stack trace.

**Covers:** README 2.1 ("appropriate exception handling and HTTP status codes"), FR-3B/FR-4C ("handle validation errors, database failures and business-rule violations")

---

## Part 1 – The theory

### 1.1 Why one error format?

Without handling, Spring returns different shapes for different errors, sometimes HTML, sometimes a stack trace (which leaks internals). The React app would need special code for each case.

We return **one shape** for everything:

```json
{
  "timestamp": "2026-11-26T03:46:19.89Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/auth/login",
  "fieldErrors": {                         ← only for field validation errors
    "email": "Email must be a valid email address",
    "password": "Password is required"
  }
}
```

The frontend can always show `message`, and highlight form fields using `fieldErrors`.

### 1.2 `@RestControllerAdvice`: one place for all errors

Instead of `try/catch` in every controller, services just **throw**, and one class catches everything:

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }
    ...
}
```

> 💡 **Analogy:** a hospital's front desk. Every department sends problems to one desk, and that desk always replies to patients in the same polite format.

### 1.3 Our own exception types

The **name** of an exception says what went wrong; the handler maps it to a status code:

| Exception | Status | Example |
|---|---|---|
| `BusinessRuleException` | **400** Bad Request | "Your age … is outside the entry age range", over-payment, invalid search combination |
| `AuthenticationFailedException` | **401** Unauthorized | "Invalid email or password" |
| `ResourceNotFoundException` | **404** Not Found | "Policy HDP-2026-0001 not found" (also used for other customers' data) |
| `ConflictException` | **409** Conflict | duplicate policy, installment already paid, policy already fully paid |

### 1.4 Errors that happen before our code runs

| Problem | Thrown by | Our answer |
|---|---|---|
| `@Valid` failed on a request body | Spring MVC: `MethodArgumentNotValidException` | 400 + `fieldErrors` |
| `@Min/@Max` failed on a URL parameter | `HandlerMethodValidationException` | 400 + `fieldErrors` |
| Broken JSON, unknown enum (`"CAR"`), wrong date format | Jackson: `HttpMessageNotReadableException` | 400 "Invalid value 'CAR' for field 'preferredPolicyType'" |
| `?dateOfBirth=15-04-1990` | `MethodArgumentTypeMismatchException` | 400 "Parameter 'dateOfBirth' has an invalid value…" |
| Unknown URL | `NoResourceFoundException` | 404 "No endpoint at /api/..." |
| Wrong HTTP method | `HttpRequestMethodNotSupportedException` | 405 |
| A DB constraint fires (e.g. two registrations at the same moment) | `DataIntegrityViolationException` | 409 "The request conflicts with existing data…" |
| Anything unexpected | `Exception` | 500 "Something went wrong" (details only in the server log) |

**Pattern matching** picks out the bad field from a Jackson error:

```java
if (ex.getCause() instanceof InvalidFormatException invalid) {
    message = "Invalid value '%s' for field '%s'".formatted(invalid.getValue(), invalid.getPath().getLast().getFieldName());
}
```

### 1.5 Security errors are special

Missing or bad tokens are rejected by the **security filter chain**, **before** the request reaches any controller. So `@RestControllerAdvice` never sees them. `JsonSecurityErrorHandler` (Step 4) writes the **same JSON shape** for 401/403.

```
request → [security filters] ──401──► JsonSecurityErrorHandler  → ApiError JSON
              │
              ▼
          controller → service ──throw──► GlobalExceptionHandler → ApiError JSON
```

### 1.6 400 vs 404 vs 409: how to choose

| Ask yourself | Code |
|---|---|
| Is the input wrong or breaking a rule? | 400 |
| Who are you? (no or bad login) | 401 |
| Does the thing not exist **for this customer**? | 404 |
| Does it clash with what's already there? | 409 |
| Did **we** break? | 500 |

---

## Part 2 – What we built

```
exception/ApiError.java                       ← the one JSON shape (record, empty fieldErrors hidden)
exception/GlobalExceptionHandler.java         ← @RestControllerAdvice, 15 handlers
exception/BusinessRuleException.java          ← 400
exception/AuthenticationFailedException.java  ← 401
exception/ResourceNotFoundException.java      ← 404
exception/ConflictException.java              ← 409
security/JsonSecurityErrorHandler.java        ← 401/403 from the security filters
```

### Request flow of an error

```
POST /api/policies {productId: 201}   (Aarav already has 201)
  → PolicyController → PolicyRegistrationService.register()
       throw new ConflictException("You already have a policy for HealthDekho Secure Term (HD-TERM-001)")
  → @Transactional rolls back (nothing saved)
  → GlobalExceptionHandler.handleConflict()
  → 409 { status: 409, error: "Conflict", message: "You already have a policy…", path: "/api/policies" }
```

**Logging:** expected problems (400/404/409) aren't logged as errors. Unexpected ones (500) are logged with the full stack trace on the server, while the client only gets a generic message.

---

## Part 3 – Run and check

Postman folder **7. Error handling**, plus the negative requests in the other folders:

| Request | Expected |
|---|---|
| ERR-01 Invalid enum `CAR` | 400 "Invalid value 'CAR' for field 'preferredPolicyType'" |
| ERR-02 Malformed JSON | 400 "Request body is missing or is not valid JSON" |
| ERR-03 Unknown URL | 404 "No endpoint at /api/does-not-exist" |
| ERR-04 DELETE /api/dashboard | 405 |
| AUTH-04 Invalid login body | 400 + `fieldErrors` |
| REG-04 Duplicate policy | 409 |
| PAY-09 Other customer's policy | 404 |

Almost every integration test also checks the error body (`$.message`, `$.fieldErrors…`).

---

## Part 4 – Screenshots

See `docs/screenshot-checklist.md` → rows **ERR-01, ERR-02** and the validation rows (slide 6).

---

## Quick recap

- **One JSON error shape** (`ApiError`) for every error, including security errors.
- **Services throw meaningful exceptions**; **`@RestControllerAdvice`** turns them into status codes.
- **400** rule/input · **401** login/token · **404** not found (or not yours) · **409** conflict · **500** our bug.
- **No stack traces to the client**; details stay in the server log.

**Next, Step 11:** automated tests and coverage.
