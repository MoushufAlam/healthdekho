# The Backend, Explained End to End

> **In one line:** this guide follows a request from the moment it reaches Spring Boot until the JSON goes back, first in general, then **endpoint by endpoint**, naming the exact class and method that does each job.
>
> New to a word? Every concept (bean, JWT, `@Transactional`, record…) is explained from zero in [`concepts-explained.md`](concepts-explained.md). Tables and columns are in [`entity-and-sql-explained.md`](entity-and-sql-explained.md).

---

## Part 1 – The big map

```
                         ┌──────────────────────── Spring Boot app (port 8080) ────────────────────────┐
 React / Postman         │                                                                             │
  ──HTTP + JSON──►  [Security filter chain]  ──►  controller  ──►  service  ──►  repository  ──►  PostgreSQL
                         │   CORS, JWT check        HTTP only       rules         SQL via JPA     7 tables
                         │                              │              │
                         │                              │              └──► ai (Spring AI → Gemini, or fallback)
                         │                              │
  ◄──JSON (DTO)──────────┤  ◄── Jackson turns the record into JSON
  ◄──JSON (ApiError)─────┘  ◄── GlobalExceptionHandler / JsonSecurityErrorHandler when anything throws
```

### Every package and what lives there

| Package | Job | Classes |
|---|---|---|
| `config` | Builds the shared objects (beans) the app needs | `SecurityConfig` (who may call what), `JwtConfig` (sign/verify tokens), `AppConfig` (Clock, AI thread pool, TransactionTemplate), `AppProperties` (typed `app.*` settings) |
| `security` | Login and "who is calling" | `CustomerUserDetails`, `CustomerUserDetailsService`, `JwtTokenService`, `CurrentCustomer`, `JsonSecurityErrorHandler` |
| `controller` | HTTP in, JSON out. **No business rules** | `AuthController`, `CustomerController`, `PolicyController`, `RecommendationController` |
| `service` | **All business rules and calculations** | `AuthService`, `CustomerService`, `ProductService`, `PolicyService`, `PolicyRegistrationService`, `RecommendationService`, `RuleBasedRecommender`, `PremiumScheduleCalculator`, `PolicyNumberGenerator`, `AgeCalculator` · *optional:* `PaymentService`, `PremiumRules`, `PolicySearchService`, `SearchCriteria` |
| `ai` | Talking to the AI model, nothing else | `RecommendationAiClient` (interface), `GeminiRecommendationClient`, `RecommendationPrompt`, `AiRecommendationInput`, `AiSuggestion`, `AiUnavailableException` |
| `repository` | Database access (Spring Data JPA) | one interface per table + `PolicyPaymentTotals` |
| `entity` (+ `enums`) | Java mirror of the 7 tables | `Customer`, `InsuranceProduct`, `CustomerPolicy`, `PremiumInstallment`, `PremiumPayment`, `AiRecommendationRequest`, `AiRecommendationItem` |
| `dto` | What the API accepts and returns (records) | `auth`, `customer`, `product`, `policy`, `recommendation` · *optional:* `payment`, `search`, `common` |
| `exception` | Error types and the one JSON error shape | `ApiError`, `BusinessRuleException` (400), `AuthenticationFailedException` (401), `ResourceNotFoundException` (404), `ConflictException` (409), `GlobalExceptionHandler` |
| `resources` | Settings and database scripts | `application.yml` (+ `-dev`, `-prod`), `db/migration/V1__create_schema.sql`, `V2__seed_data.sql` |

**The golden rule:** each layer only talks to the one below it. Controllers never touch repositories; entities never leave the service layer (controllers return DTO records).

---

## Part 2 – What happens when the app starts

`.\mvnw spring-boot:run` → `HealthDekhoApplication.main()` → `SpringApplication.run(...)`:

| # | What happens | Where it's configured |
|---|---|---|
| 1 | Reads `application.yml`, then `application-dev.yml` (profile `dev` unless `SPRING_PROFILES_ACTIVE` says otherwise). `${DB_PASSWORD}` etc. are filled from **environment variables** | `spring.profiles.active: ${SPRING_PROFILES_ACTIVE:dev}` |
| 2 | **Component scan:** finds every class annotated `@Configuration`, `@Service`, `@Component`, `@RestController`, and every repository interface | package `com.healthdekho` and below |
| 3 | Creates the **beans** and wires them together through their constructors (dependency injection) | `@RequiredArgsConstructor` on each class |
| 4 | Opens the connection pool (HikariCP) to PostgreSQL | `spring.datasource.*` |
| 5 | **Flyway** checks `flyway_schema_history`: runs V1 and V2 on an empty DB, does nothing if they already ran, **fails** if an applied file was edited (checksum) | `db/migration/` |
| 6 | **Hibernate validates** every entity against the real tables (`ddl-auto: validate`). A wrong column name stops the app here | `entity/*.java` |
| 7 | Builds the **security filter chain**, JWT encoder/decoder and password encoder | `SecurityConfig`, `JwtConfig` |
| 8 | Builds the Spring AI `ChatClient` for Gemini (with key `not-configured` if `GEMINI_API_KEY` is missing) | `spring.ai.google.genai.*` |
| 9 | Starts embedded **Tomcat** on port 8080; requests are served on **virtual threads** | `server.port`, `spring.threads.virtual.enabled` |

Log line to look for: `Started HealthDekhoApplication in … seconds`.

---

## Part 3 – The life of one request (general)

Example: `GET /api/dashboard` with header `Authorization: Bearer eyJ…`

```
1  Tomcat receives the HTTP request (on a virtual thread)
2  Security filter chain (SecurityConfig.securityFilterChain)
     a. CorsFilter            → browser origin allowed? (http://localhost:5173)
     b. BearerTokenAuthenticationFilter
          - reads "Authorization: Bearer <token>"
          - JwtDecoder (JwtConfig): signature OK (HS256 + our secret)? not expired? issuer = healthdekho?
          - OK  → SecurityContext now holds a Jwt (sub = "1001")
          - bad → JsonSecurityErrorHandler.commence → 401 JSON, request stops here
     c. AuthorizationFilter   → this URL needs authenticated()? yes, we are → continue
3  DispatcherServlet finds the method: CustomerController.dashboard(@AuthenticationPrincipal Jwt jwt)
4  Controller: CurrentCustomer.id(jwt) → 1001  (the ONLY source of "who is asking")
5  Service: PolicyService.getDashboard(1001)    ← @Transactional(readOnly = true) opens a transaction
6  Repository: findAllByCustomerIdWithProduct(1001) → Hibernate → SQL → PostgreSQL → entities
7  Service maps entities → DTO records (PolicySummaryResponse::from), transaction closes
8  Controller returns DashboardResponse → Jackson → JSON → 200 OK
```

If **anything** in steps 3–7 throws, the exception travels up to `GlobalExceptionHandler`, which picks the status code and writes an `ApiError` JSON. If a `@Transactional` method throws, every database change it made is **rolled back**.

### Request validation (before the service runs)

| Annotation | Where | Failure → |
|---|---|---|
| `@Valid @RequestBody LoginRequest` + `@NotBlank`, `@Email` in the record | body | 400 + `fieldErrors` (`MethodArgumentNotValidException`) |
| `@RequestParam @Positive Long productId` | URL params | 400 + `fieldErrors` (`HandlerMethodValidationException`) |
| Wrong type, e.g. `?startDate=12-01-2026` | URL params | 400 "Parameter 'startDate' has an invalid value…" |
| Broken JSON / unknown enum `"CAR"` | body | 400 "Invalid value 'CAR' for field 'preferredPolicyType'" |

Annotations check **shape** (required, format, size). **Business rules** (age, duplicates, dates in the past) are checked in the **service**, in plain Java.

---

## Part 4 – Every endpoint as a story

Notation: `Class.method()` = the code that runs; **DB** = what is read/written; ⚠️ = possible errors.

### 4.1 `POST /api/auth/login` (FR-1A), public

```json
{ "email": "aarav.sharma@example.com", "password": "Password@123" }
```

1. Security: the URL is `permitAll()`, so no token is needed.
2. `AuthController.login(@Valid LoginRequest)`: email format and non-blank password are checked by annotations.
3. `AuthService.login()`
   - normalises the email (`trim().toLowerCase()`)
   - `authenticationManager.authenticate(...)` → `DaoAuthenticationProvider`
     - `CustomerUserDetailsService.loadUserByUsername(email)` → **DB:** `CustomerRepository.findByEmail`
     - `PasswordEncoder.matches(typed, "{bcrypt}$2a$10$…")` → BCrypt compares the hashes
     - `CustomerUserDetails.isEnabled()` → status must be `ACTIVE`
   - pattern matching: `authentication.getPrincipal() instanceof CustomerUserDetails customer`
   - `JwtTokenService.issue(customer)` → signed JWT with `sub = 1001`, `email`, `iss = healthdekho`, `exp = now + 1h`
   - `CustomerService.getProfile(1001)` → profile with age calculated from DOB
4. **200** `{ accessToken, tokenType: "Bearer", expiresAt, customer }`

⚠️ Wrong email **or** wrong password → **401** "Invalid email or password" (the same message for both, so nobody can find out which emails exist). Inactive/locked account → 401 "This account is not active…". Bad body → 400.

### 4.2 `GET /api/customers/me`

`CustomerController.me` → `CustomerService.getProfile(id)` → **DB:** `findById` → `CustomerProfileResponse` (age via `AgeCalculator.ageOn`, using `Period.between`).

### 4.3 `GET /api/dashboard` (FR-1B)

1. `CustomerController.dashboard` → `PolicyService.getDashboard(customerId)`
2. **DB:** customer name; `CustomerPolicyRepository.findAllByCustomerIdWithProduct(id)`, a `join fetch` query that loads each policy **with its product in one SQL**, so there is no extra query per row (the N+1 problem).
3. Stream: `policies.stream().map(PolicySummaryResponse::from).toList()`
4. No policies → `message: "You have not registered any insurance policy yet…"`. With policies, `message` is left out of the JSON (`@JsonInclude(NON_NULL)`).

**Data isolation:** the query has `where p.customer.id = :customerId`, and the id comes from the token. There is no way to ask for another customer's dashboard.

### 4.4 `GET /api/products/available` (FR-1C)

`ProductService.availableProductsFor(customerId)`:

```java
Set<Long> owned = policyRepository.findAllByCustomerIdWithProduct(customerId).stream()
        .map(policy -> policy.getProduct().getId())
        .collect(Collectors.toSet());                         // products I already hold
return productRepository.findByActiveTrueOrderByProductCodeAsc().stream()
        .filter(product -> !owned.contains(product.getId()))  // duplicate rule: one policy per product
        .toList();
```

Then mapped to `ProductResponse` records. The same method is reused by the AI (4.7), so both screens agree on what is "available".

### 4.5 `GET /api/policies/preview?productId=203&startDate=2026-12-01` (FR-3A)

Read-only: nothing is saved. `PolicyRegistrationService.preview()`:

1. `today = LocalDate.now(clock)`: the injected `Clock` is in **Asia/Kolkata** (tests pin it to 26 Nov 2026).
2. Load customer + product (**404** if the product id doesn't exist).
3. `startDate` missing → today.
4. Problem check, first match wins (`Optional<String>`):
   - already holds this product → "You already have a policy for …"
   - `eligibilityProblem(...)`: product inactive · invalid premium/sum/term · start date in the past · DOB in the future · **age on the start date** outside `minEntryAge–maxEntryAge`
5. `PremiumScheduleCalculator.plan(product, startDate)`:
   - installments = `termMonths / frequency.months` (e.g. 120 / 3 = 40)
   - maturity = `start + term − 1 day` (cover ends the day before the anniversary)
   - total = `premium × installments`, `BigDecimal`, 2 decimals
   - due date *n* = `start + (n−1) × months`, always counted from the start date so a 31st doesn't drift; grace end = due + 1 month
6. **200** `RegistrationPreviewResponse`: customer (pre-fill), product, dates, totals, `eligible` + `eligibilityMessage`.

The React form calls this on every start-date change, so the screen always shows the server's own calculation.

### 4.6 `POST /api/policies` (FR-3B)

```json
{ "productId": 203, "startDate": "2026-12-01", "recommendationItemId": 7004 }
```

`PolicyRegistrationService.register()` is **`@Transactional`**: the policy and all its installments are saved together, or nothing is.

1. Same checks as the preview, but they **throw**:
   - holds it already → `ConflictException` → **409**
   - any eligibility problem → `BusinessRuleException` → **400**
2. `plan(...)`; extra safety check: maturity must be after start.
3. `PolicyNumberGenerator.next(today)` → **DB:** `findMaxPolicyNumberStartingWith("HDP-2026-")` → `HDP-2026-0003`.
4. Build `CustomerPolicy`: frequency, premium and sum insured are **copied** from the product, so a later price change doesn't alter a policy already sold. Status `ACTIVE`. **DB:** `save`.
5. Build all `PremiumInstallment`s from the plan (stream → `toList()`). **DB:** `saveAll` (40 rows for Smart Savings).
6. If `recommendationItemId` is given: `findOwnedItem(itemId, customerId)` (404 if it isn't this customer's), check it's for the same product (400), set `selected_for_registration = true`.
7. Commit → **201** with header `Location: /api/policies/HDP-2026-0003` and `{ policyNumber, message, policy, numberOfInstallments, totalPremiumPayable, firstDueDate }`.

⚠️ Two identical registrations at the very same moment: both pass the "already holds it?" check, but the database's `UNIQUE(customer_id, product_id)` rejects the second → `DataIntegrityViolationException` → **409**. The database is the last line of defence.

### 4.7 `POST /api/recommendations` (FR-2)

```json
{ "dateOfBirth": "2000-01-10", "insuranceGoals": "Health cover for parents", "preferredTerm": "1 year", "preferredPolicyType": "HEALTH" }
```

`RecommendationService.recommend()` is deliberately **not** `@Transactional`: the AI can take seconds and must not hold a database connection meanwhile.

```
1 at least one field?                       no → 400
2 DOB = request DOB or the stored DOB;  in the future → 400
3 age = AgeCalculator.ageOn(dob, today)
4 candidates = availableProductsFor(customer)        ← Java decides what is ALLOWED:
              .filter(age within entry range)           active, not owned, age fits
5 candidates empty? → FALLBACK with "No products are available for your age…"
6 generate():
    GeminiRecommendationClient.suggest(input)
      ├─ key "not-configured"? → AiUnavailableException ─────────────┐
      ├─ ChatClient.prompt()                                         │
      │    .system(RecommendationPrompt.system)  "only use these codes, never invent"
      │    .user(RecommendationPrompt.user)      age, goals, term, type + candidate list
      │    .call().entity(AiSuggestionList.class)  ← JSON answer → Java records
      ├─ runs on a virtual thread, max 20 s (CompletableFuture.get(timeout))
      └─ timeout / error → AiUnavailableException ───────────────────┤
    validated(): keep only codes that are in the candidate list,     │
                 remove duplicates, sort by rank, max 3,             │
                 reason trimmed, suitability HIGH/MEDIUM/LOW         │
    nothing valid left? ─────────────────────────────────────────────┤
                                                                     ▼
                                          RuleBasedRecommender.rank()  (FALLBACK)
                                          +3 preferred type, +2 goal keywords,
                                          +2/+1 term close; ties → cheaper first
7 persist() inside transactionTemplate.execute(...)   ← short transaction
    ai_recommendation_request: age, goals, model name/version, prompt version
    ai_recommendation_item ×N: product, rank, reason, suitability
8 201 { recommendationRequestId, calculatedAge, source: AI|FALLBACK, modelName, message, recommendations[] }
```

**Guardrails:** the AI can only *rank and explain*. It never decides eligibility or prices, and anything it invents is dropped in `validated()`. Every request is stored (with `prompt_version`) for audit.

### 4.8 *Optional* FR-4: premium payment

| Endpoint | Code path | Notes |
|---|---|---|
| `GET /api/policies/{no}` | `PolicyService.getPolicyDetail` | `getOwnedPolicy(no, customerId)` → **404 for missing and for someone else's**; totals via `PremiumRules`; status counts with `Collectors.groupingBy` into an `EnumMap` |
| `GET /api/policies/{no}/installments` | `PolicyService.getInstallments` | each row: `effectiveStatus` (OVERDUE is **derived** from today's date), `penaltyIfPaidToday`, `amountDueToday`, `selectable` |
| `POST /api/policies/{no}/payments` | `PaymentService.pay` (`@Transactional`) | see below |
| `GET /api/policies/{no}/payments` | `PolicyService.getPaymentHistory` | newest first |

`PaymentService.pay()`:

```
getOwnedPolicy → 404 if not yours
switch (policy.status): ACTIVE ok · FULLY_PAID → 409 · LAPSED/MATURED/CANCELLED → 400
amounts map (same installment twice → 400)
findForUpdate(...)  → SELECT … FOR UPDATE: a second payment for the same installment WAITS here
for each installment:
   already PAID → 409
   past grace?  → must pay outstanding + 10% penalty exactly (else 400)
   within grace → any amount up to outstanding (partial allowed; more → 400)
   update paid_amount + status (switch on compareTo), insert premium_payment (TXN-HD-…)
all installments PAID? → policy FULLY_PAID
201 { receipts[], totalPaidNow, totalPremiumPaid, totalRemainingPremium, policyStatus, message }
```

One failing installment rolls back the **whole** request (tested by PAY-11).

### 4.9 *Optional* FR-5: `GET /api/policies/search`

1. `SearchCriteria.of(number, name, dob)`, a **sealed interface** with 3 record types; invalid combinations → 400 before touching the DB.
2. `switch (criteria)` with **record patterns**:
   - `ByPolicyNumber(String number)` → `findOwnedPolicy` → 404 if not yours
   - `ByPolicyNumberAndDateOfBirth(number, dob)` → same + DOB must match
   - `ByCustomerName(String name)` → `searchOwnedByCustomerName(customerId, name, pageable)` (paged, **own policies only**)
3. Payment totals for the page in **one** grouped query (`sumByPolicyIds`), then status FULLY_PAID / OVERDUE / ON_TRACK.
4. `PageResponse<T>`: `content, page, size, totalElements, totalPages, message?`

### 4.10 `GET /actuator/health`, public

Spring Boot Actuator: `{"status":"UP"}` when the app and database are fine. Used by Docker/AWS health checks later.

---

## Part 5 – Rules that apply everywhere

### 5.1 Data isolation (your decision in CLAUDE.md)

| Rule | How the code guarantees it |
|---|---|
| The customer id comes only from the JWT | Every controller method uses `CurrentCustomer.id(jwt)`; no DTO has a `customerId` field |
| Policies are loaded by `(policyNumber + customerId)` | `findOwnedPolicy` has `where p.policyNumber = :policyNumber and c.id = :customerId` |
| Another customer's policy → **404**, not 403 | Not found and not yours look identical, so the existence of the policy isn't revealed |
| Recommendation items too | `findOwnedItem(itemId, customerId)` |
| Proven by tests | PAY-09 and SRCH-02 (Step 11) |

### 5.2 Transactions map

| Method | Transaction | Why |
|---|---|---|
| `CustomerService`, `ProductService`, `PolicyService` (whole class) | `@Transactional(readOnly = true)` | Consistent reads + lazy relations can load; Hibernate skips dirty checks |
| `PolicyRegistrationService.preview` | read-only | Nothing saved |
| `PolicyRegistrationService.register` | `@Transactional` | Policy + installments + recommendation flag: all or nothing |
| `PaymentService.pay` | `@Transactional` + row locks | Several installments + payments: all or nothing, no double payment |
| `RecommendationService.recommend` | **none**, then `TransactionTemplate` only for the save | Don't hold a DB connection while waiting up to 20 s for the AI |

### 5.3 Money, dates and time

| Rule | Where |
|---|---|
| Money is `BigDecimal`, 2 decimals, `RoundingMode.HALF_UP`; compared with `compareTo`, never `equals` | `PremiumScheduleCalculator`, `PremiumRules`, `PaymentService` |
| Dates are `LocalDate`; periods `YearMonth`; audit timestamps `Instant` | DTOs, entities |
| "Today" always comes from the injected `Clock` (Asia/Kolkata) | `AppConfig.clock()`; tests replace it with a fixed clock |
| Age = completed years (`Period.between`) | `AgeCalculator` |

### 5.4 Errors: who answers what

```
service throws ─────────────► GlobalExceptionHandler (@RestControllerAdvice)
  BusinessRuleException          → 400   "Policy start date cannot be in the past"
  AuthenticationFailedException  → 401   "Invalid email or password"
  ResourceNotFoundException      → 404   "Policy HDP-2026-0001 not found"
  ConflictException              → 409   "You already have a policy for …"
  DataIntegrityViolationException→ 409   (DB constraint fired)
  any other Exception            → 500   "Something went wrong" (stack trace only in the server log)

security filter rejects ────► JsonSecurityErrorHandler → 401 / 403 (same ApiError shape)
```

### 5.5 Logging

| Logged | Level | Example |
|---|---|---|
| Successful registration / payment | INFO | `Customer 1003 registered policy HDP-2026-0003 for product HD-HEALTH-001` |
| Failed login | INFO (email only, never the password) | `Failed login for aarav…` |
| AI failure → fallback | WARN | `AI recommendation failed: GEMINI_API_KEY is not configured` |
| Unexpected errors | ERROR with stack trace | from the 500 handler |

---

## Part 6 – Which screen calls what

| React page | Endpoints |
|---|---|
| LoginPage | `POST /api/auth/login` |
| DashboardPage | `GET /api/dashboard`, `GET /api/products/available` |
| RecommendationPage | `POST /api/recommendations` |
| RegisterPolicyPage | `GET /api/policies/preview` (repeated), `POST /api/policies`; product picker: `GET /api/products/available` |
| *PaymentPage (optional)* | `GET /api/policies/{no}`, `…/installments`, `…/payments`, `POST …/payments` |
| *SearchPage (optional)* | `GET /api/policies/search` |

---

## Part 7 – "Where do I change…?" cheat sheet

| I want to change… | File |
|---|---|
| Grace period (1 month) | `PremiumScheduleCalculator.GRACE_PERIOD_MONTHS` |
| Late penalty (10%) | `PremiumRules.LATE_PENALTY_RATE` *(optional FR-4)* |
| Policy number format | `PolicyNumberGenerator` |
| Eligibility rules | `PolicyRegistrationService.eligibilityProblem` |
| Duplicate rule | `ProductService.availableProductsFor` + `PolicyRegistrationService` + DB unique constraint |
| Token lifetime (1 h) | `app.jwt.ttl` in `application.yml` |
| Allowed frontend origins | `app.cors.allowed-origins` / env `CORS_ALLOWED_ORIGINS` |
| AI model / timeout / number of recommendations | `GEMINI_MODEL`, `app.ai.timeout`, `app.ai.max-recommendations` |
| AI prompt text | `RecommendationPrompt` (then bump `app.ai.prompt-version`) |
| Fallback scoring | `RuleBasedRecommender` |
| Error messages / status codes | `GlobalExceptionHandler`, the exception classes |
| Public (no-login) URLs | `SecurityConfig.securityFilterChain` |
| Table structure | a **new** `V3__….sql` migration (never edit V1/V2) + the entity |
