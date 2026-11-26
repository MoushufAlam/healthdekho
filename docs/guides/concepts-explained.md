# Every Concept in HealthDekho, Explained From Zero

> **In one line:** every technical idea used in this project, backend and frontend, explained with plain words, an analogy and **where exactly we use it**. Read it top to bottom once, then use it as a dictionary.
>
> Companion guides: [`backend-flow-explained.md`](backend-flow-explained.md) (how a request travels through the code) and [`entity-and-sql-explained.md`](entity-and-sql-explained.md) (the 7 tables and their Java entities).

**Contents**
1. [The web: HTTP, REST, JSON](#1-the-web-http-rest-json)
2. [Spring Boot basics](#2-spring-boot-basics)
3. [Beans and dependency injection](#3-beans-and-dependency-injection)
4. [Layered architecture, DTOs and records](#4-layered-architecture-dtos-and-records)
5. [Validation](#5-validation)
6. [Database: JPA, repositories, queries](#6-database-jpa-repositories-queries)
7. [Flyway: database migrations](#7-flyway-database-migrations)
8. [Transactions and locking](#8-transactions-and-locking)
9. [Spring Security, passwords and JWT](#9-spring-security-passwords-and-jwt)
10. [Error handling](#10-error-handling)
11. [Spring AI and the fallback](#11-spring-ai-and-the-fallback)
12. [Java 21 and modern Java features](#12-java-21-and-modern-java-features)
13. [Money, dates and time](#13-money-dates-and-time)
14. [Lombok, logging, configuration, Actuator](#14-lombok-logging-configuration-actuator)
15. [Testing, Maven and coverage](#15-testing-maven-and-coverage)
16. [Frontend: React, Router, Axios, Tailwind](#16-frontend-react-router-axios-tailwind)
17. [Glossary A–Z](#17-glossary-az)

---

## 1. The web: HTTP, REST, JSON

### Client and server

```
CLIENT (browser / Postman)                 SERVER (Spring Boot)
  "GET me the dashboard"  ── request ──►     looks it up in the database
                          ◄── response ──   "here it is" (JSON)
```

> 💡 **Analogy:** a restaurant. You (client) order from the waiter (HTTP); the kitchen (server) cooks and sends back a dish (response). You never walk into the kitchen.

### An HTTP request and response

```
POST /api/policies HTTP/1.1                     ← method + path
Authorization: Bearer eyJhbGciOi...             ← headers (extra info)
Content-Type: application/json

{ "productId": 203, "startDate": "2026-12-01" } ← body (the data)
```
```
HTTP/1.1 201 Created                            ← status code
Location: /api/policies/HDP-2026-0003
Content-Type: application/json

{ "policyNumber": "HDP-2026-0003", ... }
```

### Methods (verbs)

| Method | Meaning | Ours |
|---|---|---|
| GET | Read, changes nothing | dashboard, products, preview, search |
| POST | Create something / do an action | login, register policy, pay, recommendations |
| PUT / DELETE | Replace / delete | not used |
| OPTIONS | "What am I allowed to do?" (CORS preflight, sent by the browser itself) | automatic |

### Status codes

| Code | Name | When we send it |
|---|---|---|
| 200 | OK | successful read / login |
| 201 | Created | policy registered, payment recorded, recommendation saved |
| 400 | Bad Request | invalid input or business rule broken |
| 401 | Unauthorized | wrong login, missing/expired token |
| 403 | Forbidden | logged in but not allowed (not used by our rules, handled anyway) |
| 404 | Not Found | doesn't exist **or isn't yours** |
| 405 | Method Not Allowed | `DELETE /api/dashboard` |
| 409 | Conflict | duplicate policy, already paid |
| 500 | Internal Server Error | a bug on our side |

Rule of thumb: **2xx = it worked, 4xx = the client did something wrong, 5xx = the server did.**

### JSON

The text format both sides understand:

```json
{ "policyNumber": "HDP-2026-0001", "premiumAmount": 1250.00, "active": true, "policies": [ ] }
```

Objects `{ }`, lists `[ ]`, text in quotes, numbers and true/false without quotes.

### REST

A **style** of designing APIs: URLs name **things** (nouns), methods say **what to do**.

| Not REST-like | REST-like (ours) |
|---|---|
| `POST /api/doPayment?policy=1` | `POST /api/policies/HDP-2026-0001/payments` |
| `GET /api/getPolicyDetails` | `GET /api/policies/HDP-2026-0001` |

**Stateless:** each request carries everything needed (the JWT); the server remembers no "session" between requests.

---

## 2. Spring Boot basics

| Concept | What it is | Analogy | In our code |
|---|---|---|---|
| **Spring Framework** | A large Java toolkit: web, data, security, DI | A fully equipped workshop | everything |
| **Spring Boot** | Spring with sensible defaults + an embedded server; `main()` starts everything | A pre-assembled workshop: plug in and work | `HealthDekhoApplication` |
| **Starter** | One dependency that brings a whole feature set | A "kit" instead of buying parts one by one | `spring-boot-starter-web`, `-data-jpa`, `-security`, `-validation`, `-actuator`, `spring-ai-starter-model-google-genai` |
| **Auto-configuration** | Boot sees a library on the classpath and configures it (e.g. PostgreSQL driver + URL → DataSource) | Plug-and-play USB | DataSource, Jackson, Tomcat, Flyway, ChatClient |
| **Embedded Tomcat** | The web server runs **inside** the app; no separate install | – | port 8080 |
| **`@SpringBootApplication`** | Marks the start class: enables auto-config + component scan | The ignition key | `HealthDekhoApplication` |

---

## 3. Beans and dependency injection

### Inversion of Control (IoC) and beans

Normally your code creates objects with `new`. With Spring, **Spring creates them** and hands them to you. Every object Spring manages is a **bean**; the place that holds them is the **application context**.

### Dependency injection (DI)

```java
@Service
@RequiredArgsConstructor                         // Lombok writes the constructor below for us
public class ProductService {
    private final InsuranceProductRepository productRepository;  // "I need these"
    private final CustomerPolicyRepository policyRepository;
    // public ProductService(InsuranceProductRepository a, CustomerPolicyRepository b) { ... }
}
```

Spring sees the constructor, finds beans of those types and passes them in. `ProductService` never says `new`.

> 💡 **Analogy:** a hotel room. You don't bring your own bed and towels (`new`); you list what you need and the hotel (Spring) provides them.

**Why bother?** Classes stay small and testable: in tests we swap the real Gemini client for a fake (`@MockitoBean`) without touching `RecommendationService`.

### Annotations that create beans

| Annotation | Meaning | Ours |
|---|---|---|
| `@Component` | "Spring, manage this class" | `RuleBasedRecommender`, `PremiumScheduleCalculator`, `GeminiRecommendationClient` |
| `@Service` | Same, but says "business logic lives here" | all `*Service` classes |
| `@Repository` / Spring Data interfaces | Database access; Spring **generates** the implementation | `*Repository` |
| `@RestController` | Handles HTTP and returns JSON | the 4 controllers |
| `@Configuration` + `@Bean` | A class whose methods build beans by hand | `AppConfig.clock()`, `SecurityConfig.passwordEncoder()`, `JwtConfig.jwtDecoder()` |

**Singleton:** by default there is **one** instance of each bean shared by all requests. That's why services keep no per-request data in fields (only `final` dependencies).

---

## 4. Layered architecture, DTOs and records

### Layers

| Layer | Knows about | Never does |
|---|---|---|
| **Controller** | HTTP: URL, method, status code, request/response DTOs | business rules, SQL |
| **Service** | Business rules, calculations, transactions | HTTP details |
| **Repository** | SQL/JPA | rules |
| **Entity** | One table row | leave the service layer |

> 💡 **Analogy:** a bank. The cashier (controller) talks to customers, the back office (service) applies the rules, the vault (repository) stores the money. The cashier never opens the vault directly.

### DTO (Data Transfer Object)

The shape of data **on the wire**, separate from entities.

| Why not return entities? | |
|---|---|
| Security | `Customer` has `passwordHash`; a DTO simply doesn't have that field |
| Stability | Renaming a column doesn't break the API |
| Shape | The API can combine data: `PolicySummaryResponse` = policy + product name |
| Lazy loading | Serialising an entity could trigger extra queries or crash after the transaction |

### Java records

A compact, **immutable** data class:

```java
public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) { }
```

The compiler generates the constructor, the accessors (`email()`, `password()`), and `equals`, `hashCode` and `toString`. Fields are `final`, so a record can't be changed after creation. All our DTOs are records. A **static factory** like `ProductResponse.from(product)` converts an entity to a DTO.

### Jackson

The library that converts **JSON ↔ Java objects** automatically:
- request body → `@RequestBody LoginRequest`
- returned record → JSON response
- `@JsonInclude(NON_NULL)` → leave out fields that are `null` (dashboard `message`)
- `LocalDate` ↔ `"2026-12-01"`, `BigDecimal` ↔ `1250.00`, enums ↔ `"HALF_YEARLY"`

---

## 5. Validation

**Bean Validation** = rules written as annotations on fields, checked automatically when a controller parameter has `@Valid`.

| Annotation | Rule | Example |
|---|---|---|
| `@NotBlank` | not null, not empty, not just spaces | password |
| `@NotNull` | must be present | `productId` |
| `@Email` | looks like an email | login email |
| `@Positive` | > 0 | `productId`, `amount` |
| `@Size(max = 1000)` | length limit | `insuranceGoals` |
| `@Min` / `@Max` | number range | search `size` 1–50 |
| `@Digits(fraction = 2)` | at most 2 decimals | payment amount |

Failure → 400 with `fieldErrors` per field.

**Annotations vs service rules:**

| Annotations (shape) | Service code (meaning) |
|---|---|
| "email is required" | "is this customer old enough for this product?" |
| "amount > 0" | "does this amount exceed what's outstanding?" |
| no database needed | may need the database and today's date |

---

## 6. Database: JPA, repositories, queries

### ORM, JPA, Hibernate

| Term | Meaning |
|---|---|
| **ORM** (Object-Relational Mapping) | Work with Java objects; the library writes the SQL |
| **JPA** | The Java *standard* (annotations like `@Entity`, `@Id`, `@ManyToOne`) |
| **Hibernate** | The *implementation* that actually does it |
| **Entity** | A class mapped to a table; one object = one row (full details in the entity guide) |
| **`ddl-auto: validate`** | Hibernate only **checks** entities match the tables; Flyway creates them |
| **Lazy loading** (`@ManyToOne(fetch = LAZY)`) | Related object loaded only when first used |
| **Dirty checking** | Inside a transaction, change an entity's field and Hibernate writes the `UPDATE` at commit; no explicit `save` needed |
| **`open-in-view: false`** | The DB session closes when the service returns, so no hidden queries happen while JSON is written |

### Spring Data JPA repositories

You write an **interface**; Spring generates the class.

```java
public interface InsuranceProductRepository extends JpaRepository<InsuranceProduct, Long> {
    List<InsuranceProduct> findByActiveTrueOrderByProductCodeAsc();   // derived query
}
```

| Kind | How | Ours |
|---|---|---|
| Inherited methods | `save`, `saveAll`, `findById`, `findAll`, `getReferenceById`… | registration, AI persistence |
| **Derived query** | Spring reads the **method name** and builds the SQL | `findByActiveTrueOrderByProductCodeAsc`, `existsByCustomer_IdAndProduct_Id`, `countByPolicy_IdAndPaymentStatusNot` |
| **`@Query` (JPQL)** | You write the query using **entity and field names**, not table names | `findOwnedPolicy`, `findAllByCustomerIdWithProduct` |
| **Constructor expression** | `select new …PolicyPaymentTotals(…)` builds records directly from a query | `sumByPolicyIds` |
| **Text blocks** `"""…"""` | Multi-line strings for readable queries | all `@Query` |

### Optional

`findById` returns `Optional<Customer>`, a box that may be empty. You have to decide what happens if it is:

```java
customerRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
```

No more surprise `NullPointerException`s. README FR-1E/3C asks for "Optional for repository lookups".

### The N+1 problem and `join fetch`

```
BAD : 1 query for 10 policies  + 10 queries for their products  = 11 queries ("N+1")
GOOD: select p from CustomerPolicy p join fetch p.product …      = 1 query
```

Used in `findAllByCustomerIdWithProduct`, `findOwnedPolicy` and `findHistoryByPolicyId`.

### Pagination (optional FR-5)

`Pageable` (page number + size + sort) goes in, `Page<T>` (content + totals) comes out. We convert it into our own `PageResponse` record so the JSON shape stays stable.

---

## 7. Flyway: database migrations

**Problem:** the database structure must be the same on your PC, the second PC, the test DB and AWS.
**Solution:** numbered SQL files that Flyway runs **once each, in order**, and records in `flyway_schema_history`.

```
db/migration/
  V1__create_schema.sql   ← 7 tables, constraints, indexes, trigger
  V2__seed_data.sql       ← 3 customers, 3 products, 2 policies, …
  (V3__… for any future change)
```

| Rule | Why |
|---|---|
| Never edit a file that already ran (not even a comment) | Flyway stores a **checksum**; a changed file stops the app ("checksum mismatch") |
| Change = a **new** file `V3__add_x.sql` | Every database replays the same history |
| Empty database = run everything | That's how `healthdekho_test` builds itself |

> 💡 **Analogy:** a recipe book where you only ever **add** pages. Anyone can cook from page 1 and get the same dish.

---

## 8. Transactions and locking

### Transaction

A group of database changes that **all succeed or all fail** (ACID: Atomic, Consistent, Isolated, Durable).

```java
@Transactional
public PolicyRegistrationResponse register(...) {
    policyRepository.save(policy);          // 1
    installmentRepository.saveAll(list);    // 2 ← if this fails…
    item.setSelectedForRegistration(true);  // 3
}                                           //   …1 is undone too (rollback)
```

| Term | Meaning | Ours |
|---|---|---|
| **Commit** | Make the changes permanent | method returns normally |
| **Rollback** | Undo everything | a `RuntimeException` leaves the method |
| `readOnly = true` | Read-only transaction; Hibernate skips change tracking | `PolicyService`, `ProductService`, `CustomerService` |
| `TransactionTemplate` | Start a transaction in code, around just one block | `RecommendationService`: only the save, not the slow AI call |
| Propagation `NOT_SUPPORTED` | Run **without** the caller's transaction | test PAY-11, to see a real rollback |

> 💡 **Analogy:** a bank transfer. Taking ₹100 from A and adding it to B happen together, or neither happens.

### Pessimistic locking (optional FR-4)

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)   // SQL: SELECT … FOR UPDATE
List<PremiumInstallment> findForUpdate(...)
```

The first payment request **locks** the installment rows; a second, simultaneous request **waits**, then sees "already PAID" → 409. No double payment.

### Database constraints: the last line of defence

Java checks first (friendly messages), but the database also has `UNIQUE`, `CHECK` and `FOREIGN KEY` rules. If two requests slip past Java at the same instant, the database refuses the second → `DataIntegrityViolationException` → 409.

---

## 9. Spring Security, passwords and JWT

### Authentication vs authorization

| | Question | Ours |
|---|---|---|
| **Authentication** | *Who are you?* | email + password at login; then the JWT on every request |
| **Authorization** | *What may you do?* | everything except login/health requires a valid token; data limited to **your** customer id |

### The security filter chain

Every request passes through a chain of **filters** before reaching a controller:

```
request → CORS → BearerToken (JWT check) → Authorization (is the URL allowed?) → controller
                         │ invalid/missing
                         └──► 401 JSON (JsonSecurityErrorHandler)
```

> 💡 **Analogy:** airport security. Several checkpoints in a row; fail one and you never reach the gate (controller).

Configured in `SecurityConfig.securityFilterChain`: `permitAll()` for `POST /api/auth/login`, `/actuator/health` and `/error`; `authenticated()` for everything else.

### Storing passwords: hashing with BCrypt

| Never | Always |
|---|---|
| store `Password@123` | store a **hash**: `{bcrypt}$2a$10$N9qo8uLO…` |

- A **hash** is one-way: you can't turn it back into the password.
- **BCrypt** adds a random **salt** (same password → different hash) and is deliberately **slow** (hard to brute-force).
- At login: hash the typed password the same way and compare.
- `{bcrypt}` prefix: `DelegatingPasswordEncoder` reads it to know which algorithm was used, so the algorithm can be upgraded later.

### The login machinery

| Piece | Job | Ours |
|---|---|---|
| `UserDetails` | Spring's view of a user (username, hash, enabled?) | `CustomerUserDetails` record |
| `UserDetailsService` | Load a user by username | `CustomerUserDetailsService` → `findByEmail` |
| `PasswordEncoder` | Compare typed password with the hash | `DelegatingPasswordEncoder` (BCrypt) |
| `AuthenticationManager` / `DaoAuthenticationProvider` | Runs the whole check | used by `AuthService.login` |

### JWT (JSON Web Token)

After login the server gives the client a **signed token**. The client sends it with every request: `Authorization: Bearer <token>`.

```
eyJhbGciOiJIUzI1NiJ9 . eyJzdWIiOiIxMDAxIiwiZW1haWwiOi4uLn0 . 7mJ0x…
      HEADER                       PAYLOAD (claims)               SIGNATURE
 {"alg":"HS256"}       {"iss":"healthdekho","sub":"1001",      HMAC-SHA256(header.payload,
                        "email":"…","iat":…,"exp":…}               JWT_SECRET)
```

| Claim | Meaning | Ours |
|---|---|---|
| `sub` (subject) | who | **customer id**: the only source of identity |
| `iss` (issuer) | who issued it | `healthdekho` (checked on every request) |
| `iat` / `exp` | issued at / expires | 1 hour lifetime |

- **Signed, not encrypted:** anyone can *read* the payload (so no secrets inside), but nobody can *change* it without the secret: the signature would no longer match.
- **HS256** = HMAC with SHA-256 and one shared secret (`JWT_SECRET`, ≥ 32 characters). Without `JWT_SECRET` in dev, a random key is made at start-up, so tokens die on restart.
- **Stateless:** the server stores no sessions; the token itself proves who you are.

> 💡 **Analogy:** a festival wristband. Checked at the entrance (login), then you just show it; staff can see it's genuine (signature) and when it expires.

| Spring piece | Job |
|---|---|
| `JwtEncoder` (Nimbus) | creates and signs tokens (`JwtTokenService.issue`) |
| `JwtDecoder` (Nimbus) | verifies signature, expiry, issuer on every request |
| OAuth2 **Resource Server** | the ready-made filter that reads `Bearer` tokens |
| `@AuthenticationPrincipal Jwt jwt` | hands the verified token to the controller |
| `CurrentCustomer.id(jwt)` | `Long.valueOf(jwt.getSubject())` |

### CSRF and CORS

| | What | Our setting |
|---|---|---|
| **CSRF** (Cross-Site Request Forgery) | An attack that abuses **cookies** the browser sends automatically | **Disabled**: we use no cookies; a token in a header can't be sent by another site |
| **CORS** (Cross-Origin Resource Sharing) | Browser rule: a page from origin A may read responses from origin B only if B allows A | Allow `http://localhost:5173` (and `:3000`), methods GET/POST/PUT/DELETE/OPTIONS, headers `Authorization`, `Content-Type` |

**Origin** = protocol + host + port. `localhost:5173` ≠ `localhost:8080`.

### Security choices you can defend

| Choice | Reason |
|---|---|
| Same message for wrong email and wrong password | Attackers can't discover which emails exist (user enumeration) |
| Another customer's policy → 404, not 403 | Doesn't even confirm that the policy exists |
| Customer id never accepted from the request | Can't be tampered with; only the signed token counts |
| Secrets only in environment variables | Never in git |

---

## 10. Error handling

| Concept | Meaning | Ours |
|---|---|---|
| **Exception** | Java's way to say "this can't continue" | services `throw new BusinessRuleException("…")` |
| **Custom exception** | Its **name** says what went wrong | `BusinessRuleException` 400, `AuthenticationFailedException` 401, `ResourceNotFoundException` 404, `ConflictException` 409 |
| **`@RestControllerAdvice`** | One class that catches exceptions from **all** controllers | `GlobalExceptionHandler` |
| **`@ExceptionHandler(X.class)`** | "When X is thrown, run this method" | one handler per exception type |
| **`ApiError`** | The single JSON error shape | `{ timestamp, status, error, message, path, fieldErrors? }` |
| **AuthenticationEntryPoint** | Handles security errors that happen **before** controllers | `JsonSecurityErrorHandler` |

> 💡 **Analogy:** a hospital front desk. Every department sends problems to one desk, which always replies in the same polite format.

---

## 11. Spring AI and the fallback

| Concept | Meaning | Ours |
|---|---|---|
| **LLM** (Large Language Model) | AI that reads and writes text | Google **Gemini** (`gemini-2.5-flash` by default) |
| **Spring AI** | Spring's library for talking to AI models with one API | `spring-ai-starter-model-google-genai` |
| **`ChatClient`** | Fluent API: build a prompt, call the model, read the answer | `chatClient.prompt().system(…).user(…).call()` |
| **System prompt** | Rules for the AI ("only use these product codes, never invent") | `RecommendationPrompt.SYSTEM` |
| **User prompt** | The actual question with data (age, goals, candidate products) | `RecommendationPrompt.user(input)` |
| **Structured output** | Ask for JSON and map it straight into Java records | `.entity(AiSuggestionList.class)` |
| **Temperature** | Randomness: 0 = predictable, 1 = creative | `0.2` |
| **Retry** | Try again on temporary errors | `spring.ai.retry.max-attempts: 2` |
| **Timeout** | Stop waiting after 20 s | `CompletableFuture.get(timeout)` |
| **Fallback** | A plan B that always works | `RuleBasedRecommender` (deterministic scoring) |
| **Guardrails** | Never trust the output blindly | drop unknown codes, duplicates, owned/ineligible products; trim reasons |
| **Prompt versioning / audit** | Store which prompt and model produced each answer | `prompt_version`, `model_name` columns |

**The golden rule (README):** *AI never replaces validations or calculations.* Java decides which products are allowed; the AI only **ranks and explains** them.

```
Java: eligible candidates ──► AI: rank + reasons ──► Java: validate ──► save ──► respond
                                   │ fails / no key / timeout
                                   └──► RuleBasedRecommender (FALLBACK)
```

**Interface + implementation:** `RecommendationAiClient` is an interface; `GeminiRecommendationClient` implements it. Tests replace it with a mock, and switching to another AI provider would only need a new implementation.

---

## 12. Java 21 and modern Java features

| Feature | What it does | Example in our code |
|---|---|---|
| **Records** | Immutable data classes in one line | every DTO, `PremiumPlan`, `IssuedToken`, `Ranked` |
| **Sealed interface** | Only the listed types may implement it, so the compiler knows all cases | `SearchCriteria` *(optional FR-5)* |
| **Pattern matching for `instanceof`** | Test the type and bind a variable in one go | `if (principal instanceof CustomerUserDetails customer)` (`AuthService`) |
| **Switch expressions** (`->`, returns a value) | No `break`, no fall-through; the compiler checks every enum case is covered | `PremiumRules.effectiveStatus`, `PaymentService.ensureAcceptsPayments` |
| **`yield`** | Return a value from a `{ }` block inside a switch expression | `effectiveStatus` |
| **Record patterns in switch** | Take a record apart while matching | `case ByPolicyNumber(String number) -> …` (`PolicySearchService`) |
| **Text blocks** `"""` | Readable multi-line strings | JPQL queries, AI prompt |
| **`String.formatted(...)`** | `"Policy %s not found".formatted(no)` | everywhere |
| **Sequenced collections** | `list.getFirst()`, `getLast()` | `PremiumPlan.firstDueDate()` |
| **Virtual threads** | Very cheap threads; thousands can wait for I/O without wasting OS threads | all requests (`spring.threads.virtual.enabled`), AI calls (`newVirtualThreadPerTaskExecutor`) |
| **`CompletableFuture`** | A result that arrives later; can wait with a timeout | `GeminiRecommendationClient.suggest` |

### Streams and Collectors

A **stream** is a pipeline over a collection: filter, transform, collect.

```java
products.stream()                                         // source
        .filter(product -> !owned.contains(product.getId()))  // keep some
        .map(ProductResponse::from)                       // transform (method reference)
        .toList();                                        // collect
```

| Operation | Ours |
|---|---|
| `filter` | exclude owned products; age-eligible candidates |
| `map` | entity → DTO |
| `collect(Collectors.toSet())` | owned product ids |
| `collect(Collectors.toMap(...))` | product by code (AI validation), amount by installment |
| `collect(Collectors.groupingBy(..., counting()))` | installment status counts *(FR-4)* |
| `reduce(BigDecimal.ZERO, BigDecimal::add)` | sum money |
| `IntStream.rangeClosed(1, n)` | build installments 1…n |
| `sorted(Comparator.comparing(...))`, `limit(n)` | rank recommendations |

**Lambda** `x -> x * 2` = a small anonymous function. **Method reference** `ProductResponse::from` = shorthand for `p -> ProductResponse.from(p)`.

---

## 13. Money, dates and time

### BigDecimal: why not `double`?

```java
0.1 + 0.2            // double → 0.30000000000000004  ❌ never for money
new BigDecimal("0.1").add(new BigDecimal("0.2"))  // → 0.3  ✅
```

| Rule | Ours |
|---|---|
| Create from **strings**, not doubles | `new BigDecimal("0.10")` |
| Fix the scale and rounding | `.setScale(2, RoundingMode.HALF_UP)` |
| Compare with `compareTo`, not `equals` | `1250.0` and `1250.00` are `compareTo`-equal but **not** `equals` |
| Database type | `NUMERIC(19,2)` |

### java.time

| Class | Holds | Ours |
|---|---|---|
| `LocalDate` | a date without time: 2026-12-01 | DOB, start, maturity, due dates |
| `YearMonth` | year + month: 2026-12 | installment `period` *(FR-4)* |
| `Period` | difference in years/months/days | `Period.between(dob, today).getYears()` = age |
| `Instant` | an exact moment (UTC) | token expiry, `created_at` |
| `Duration` | an amount of time | JWT ttl `PT1H`, AI timeout `PT20S` |
| `ZoneId` | a time zone | `Asia/Kolkata` |
| **`Clock`** | "what time is it?" as an injectable bean | `LocalDate.now(clock)` everywhere |

**Why inject a `Clock`?** Grace periods and ages depend on *today*. In tests we inject a **fixed** clock (26 Nov 2026), so tests give the same result every day. And with `Asia/Kolkata`, "today" is the Indian date even on a server in another country.

Date maths used: `plusMonths`, `minusDays`, `isBefore`, `isAfter`. `plusMonths` is **calendar-aware**: 31 Jan + 1 month = 28/29 Feb.

---

## 14. Lombok, logging, configuration, Actuator

### Lombok

Generates boilerplate at compile time.

| Annotation | Generates | Rule in this project |
|---|---|---|
| `@Getter @Setter` | getters/setters | on entities |
| `@NoArgsConstructor` | empty constructor (JPA needs it) | on entities |
| `@RequiredArgsConstructor` | constructor for all `final` fields → DI | on services/controllers |
| `@Slf4j` | a `log` field | services that log |
| `@Data` | getters + setters + `equals/hashCode/toString` on **all** fields | **never on entities**: `toString`/`hashCode` would walk lazy relations and can trigger queries or infinite loops |

### Logging (SLF4J + Logback)

`log.info("Customer {} registered policy {}", id, number)`: the `{}` placeholders are only filled if the line is actually printed. Levels: `ERROR > WARN > INFO > DEBUG`. Never log passwords or tokens.

### Configuration and profiles

| Piece | Meaning |
|---|---|
| `application.yml` | settings shared by all environments |
| `application-dev.yml` / `-prod.yml` / `-test.yml` | per-environment overrides, chosen by **profile** |
| `${DB_PASSWORD}` | value comes from an **environment variable** |
| `${GEMINI_MODEL:gemini-2.5-flash}` | env variable **with default** after the `:` |
| `@ConfigurationProperties("app")` + record | typed access: `properties.jwt().ttl()` (`AppProperties`) |

**Secrets** (DB password, JWT secret, Gemini key) live **only** in environment variables, never in files committed to git.

### Actuator

Ready-made operational endpoints. We expose only `/actuator/health` (`{"status":"UP"}`), which load balancers and Docker use to check the app is alive.

---

## 15. Testing, Maven and coverage

| Concept | Meaning | Ours |
|---|---|---|
| **Maven** | Build tool: downloads dependencies, compiles, tests, packages | `pom.xml` |
| **Maven Wrapper** `mvnw` | Downloads the right Maven version; nothing to install | `.\mvnw test` |
| **Lifecycle** | `compile → test → package → verify` | `verify` also checks coverage |
| **Unit test** | One class alone, milliseconds | `PremiumScheduleCalculatorTest` |
| **Integration test** | Whole app + real PostgreSQL | `RegistrationIntegrationTest` |
| **JUnit 5** | Runs tests (`@Test`, `@ParameterizedTest`, `@DisplayName`) | all tests |
| **AssertJ** | Readable checks `assertThat(x).isEqualTo(y)` | – |
| **MockMvc** | Fake HTTP requests without a real server | integration tests |
| **Mockito** / `@MockitoBean` | Replace a bean with a scripted fake | the AI client |
| **Test isolation** | `@Transactional` tests roll back; separate `healthdekho_test` DB; fixed clock | `IntegrationTestBase` |
| **JaCoCo** | Measures which lines/branches tests executed; build fails under 80% for `service` | `target/site/jacoco/index.html` |

Details: [`../steps/step11.readme.md`](../steps/step11.readme.md).

---

## 16. Frontend: React, Router, Axios, Tailwind

### The toolchain

| Concept | Meaning | Ours |
|---|---|---|
| **Node.js / npm** | JavaScript runtime / package manager (like JVM / Maven) | `npm install`, `npm run dev` |
| **ES modules** | `import { api } from './client.js'` / `export function …` | every file |
| **Vite** | Dev server with instant reload; `npm run build` makes optimised static files | `vite.config.js` |
| **SPA** | One HTML page; JavaScript swaps the screens | `index.html` + `main.jsx` |
| **Env variables** | `import.meta.env.VITE_API_BASE_URL`; only `VITE_*` reach the browser | `.env.example` |

### React

| Concept | Meaning | Ours |
|---|---|---|
| **Component** | A function that returns UI | `DashboardPage`, `Button`, `Card` |
| **JSX** | HTML-like syntax in JavaScript; `{expr}` inserts values | every component |
| **Props** | Inputs from the parent | `<StatusBadge status="ACTIVE" />` |
| **State** (`useState`) | Data that changes and redraws the UI | form fields, `submitting` |
| **Effect** (`useEffect`) | Code that runs after drawing: fetch data, timers | `useApiData`, auto-logout |
| **Context** (`createContext`/`useContext`) | Share data with any component without passing props down | `AuthContext` → `useAuth()` |
| **Ref** (`useRef`) | Remember something without redrawing | last preview, scroll target |
| **Custom hook** | Reusable `use…` function | `useApiData` |
| **Controlled input** | Input value always comes from state | all forms |
| **Conditional rendering** | `{error && <Alert>…}` / ternaries | loading/empty/error states |
| **Lists and `key`** | `items.map(i => <Tr key={i.id}>…)`; `key` lets React track rows | every table |
| **StrictMode** | Dev-only checks; runs effects twice to catch bugs | `main.jsx` |
| **`startTransition`** | Mark state updates as one low-priority batch | `logout()` |

### React Router

| Concept | Ours |
|---|---|
| `<BrowserRouter>` | wraps the app (`main.jsx`) |
| `<Routes>` / `<Route path element>` | `App.jsx` |
| Nested routes + `<Outlet />` | `AppLayout` draws header/nav; the page goes into the Outlet |
| Guard route | `ProtectedRoute` → `<Navigate to="/login" state={{ from }}>` |
| `Link`, `NavLink` | navigation without reload; NavLink highlights the active item |
| `useNavigate`, `useParams`, `useSearchParams`, `useLocation` | redirect after login; `/payments/:policyNumber`; `?productId=`; `state.from` |

### Axios and async JavaScript

| Concept | Meaning | Ours |
|---|---|---|
| **Promise** | "The result will come later" | every API call |
| **`async` / `await`** | Write asynchronous code like normal code | `handleSubmit` functions |
| **`try / catch`** | Catch a failed request | show the server's message |
| **`Promise.all`** | Run several requests in parallel | dashboard, payment page |
| **Axios instance** | Shared base URL + timeout | `api/client.js` |
| **Interceptors** | Code that runs on every request/response | add `Authorization`; logout on 401 |

### Browser storage

| Storage | Lifetime | Ours |
|---|---|---|
| `sessionStorage` | until the tab is closed | **the session (token + customer)** |
| `localStorage` | until deleted | not used (keeps tokens too long) |
| Cookies | sent automatically with requests | not used (that's why CSRF doesn't apply) |

### Tailwind CSS

| Concept | Meaning |
|---|---|
| **Utility classes** | Small single-purpose classes: `p-4`, `text-sm`, `rounded-xl`, `bg-brand-600` |
| **Variants** | `hover:`, `focus-visible:`, `disabled:` |
| **Responsive prefixes** | `sm:` `md:` `lg:` = from that screen width up (mobile-first) |
| **`@theme` tokens** | Our colours (`brand-*`), font and shadow defined once in `index.css` |
| **Flexbox / Grid** | `flex items-center gap-3`, `grid sm:grid-cols-3` for layouts |
| **Reusable components** | Long class lists hidden inside `Button`, `Card`… |

### Accessibility basics we followed

`<label htmlFor>` linked to inputs; `aria-invalid` and `aria-describedby` on fields with errors; `role="alert"` on error messages; icon-only buttons have `aria-label`; everything works with the keyboard (visible focus rings).

---

## 17. Glossary A–Z

| Term | One-line meaning |
|---|---|
| ACID | Atomic, Consistent, Isolated, Durable: what a transaction guarantees |
| API | The set of URLs a program offers to other programs |
| Bean | An object created and managed by Spring |
| BCrypt | Slow, salted, one-way password hashing algorithm |
| BigDecimal | Exact decimal numbers for money |
| Claim | One piece of information inside a JWT (`sub`, `exp`…) |
| Clock | Injectable "current time" so tests can fix today's date |
| Component (React) | Function returning UI |
| CORS | Browser rule for reading responses from another origin |
| CSRF | Attack using automatically-sent cookies; not relevant without cookies |
| Derived query | Repository method whose **name** becomes the SQL |
| DI | Dependency injection: Spring passes in what a class needs |
| DTO | Object shaped for the API, not the database |
| Entity | Java class mapped to a table |
| Fallback | Plan B when the main path (AI) fails |
| Filter chain | The security checkpoints before a controller |
| Flyway | Runs versioned SQL migrations once each |
| Hook | React function (`use…`) giving components state/effects |
| Idempotent | Doing it twice has the same effect as once (GET is; our POSTs are not) |
| Interceptor | Code that runs on every Axios request/response |
| JPQL | Query language using entity names instead of table names |
| JSON | Text data format of the API |
| JWT | Signed token proving who you are |
| Lazy loading | Related data loaded only when first used |
| LLM | Large language model (the AI) |
| Migration | One versioned database change script |
| N+1 | One query for a list plus one per row; fixed with `join fetch` |
| Optional | A box that may be empty; forces handling "not found" |
| Origin | protocol + host + port of a web page |
| Pagination | Returning results page by page |
| Pessimistic lock | `SELECT … FOR UPDATE`: others wait until you're done |
| Profile | A named set of settings (`dev`, `prod`, `test`) |
| Prompt | The instructions and data sent to the AI |
| Props | Inputs to a React component |
| Record | Immutable Java data class |
| Repository | Interface for database access |
| REST | API style: nouns in URLs, HTTP verbs for actions |
| Rollback | Undo all changes of a failed transaction |
| Sealed interface | Interface with a fixed, known list of implementations |
| SPA | Single-page application |
| State (React) | Changing data that redraws the UI |
| Stateless | Server keeps no session; each request carries its token |
| Stream | Pipeline for processing collections |
| Transaction | All-or-nothing group of DB changes |
| Virtual thread | Lightweight Java thread, cheap to block on I/O |
