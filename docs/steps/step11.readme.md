# Step 11 – Automated Tests and Coverage (JaCoCo)

> **In one line:** 85 automated tests check every feature and business rule, on a separate test database, and JaCoCo proves the service layer is over 80% covered.

**Covers:** PPT slides 12 (test execution) and 13 (coverage)

---

## Part 1 – The theory

### 1.1 Two kinds of tests

| | Unit test | Integration test |
|---|---|---|
| Tests | One class, alone | The whole app: HTTP → controller → service → repository → **real PostgreSQL** |
| Speed | Milliseconds | ~10 s to start, then fast |
| Spring started? | No | Yes (`@SpringBootTest`) |
| Database? | No (Mockito fakes if needed) | Yes, `healthdekho_test` |
| Ours | `PremiumRulesTest`, `PremiumScheduleCalculatorTest`, `SearchCriteriaTest`, `RuleBasedRecommenderTest`, `PolicyNumberGeneratorTest`, `GeminiRecommendationClientTest` | `AuthIntegrationTest`, `DashboardIntegrationTest`, `RegistrationIntegrationTest`, `PaymentIntegrationTest`, `SearchIntegrationTest`, `RecommendationIntegrationTest`, `RepositoryMappingTest` |

### 1.2 The tools

| Tool | Job | Example |
|---|---|---|
| **JUnit 5** | Runs tests | `@Test`, `@DisplayName("PAY-05 …")`, `@ParameterizedTest` |
| **AssertJ** | Readable checks | `assertThat(total).isEqualByComparingTo("300000.00")` |
| **MockMvc** | Sends fake HTTP requests to the app, no real server | `mockMvc.perform(post("/api/policies")…).andExpect(status().isCreated())` |
| **JsonPath** | Checks fields in the JSON response | `jsonPath("$.receipts[0].penaltyAmount").value(125.00)` |
| **Mockito** | Fake objects | the Gemini client is replaced by `@MockitoBean`, so no network or key is needed |
| **spring-security-test** | Fake logged-in user | `.with(as(AARAV))` = request with a JWT whose `sub` is 1001 |
| **JaCoCo** | Measures which lines the tests ran | HTML report + build fails if service coverage < 80% |

### 1.3 Making integration tests reliable

| Problem | Our fix |
|---|---|
| Tests would change your dev data | Separate DB **`healthdekho_test`** (`application-test.yml`); Flyway builds and seeds it automatically |
| Test A's payment breaks test B | Each test class is **`@Transactional`**, so every test is **rolled back** at the end |
| "Today" changes every day (grace periods!) | **Fixed `Clock`**: tests always run on 26 Nov 2026 |
| AI costs money / needs a network | `@MockitoBean RecommendationAiClient`: we script exactly what the "AI" answers |
| Testing a real rollback inside a rolled-back test | PAY-11 runs with `@Transactional(propagation = NOT_SUPPORTED)`, so the service's own transaction really rolls back |

### 1.4 Test IDs = evidence IDs

Every test has an ID in its `@DisplayName` (e.g. `PAY-09 ISOLATION: customer B cannot view or pay customer A's policy`). The PPT asks for **test case IDs on every screenshot**: use these IDs.

---

## Part 2 – What we built

### Test inventory (85 tests, all passing)

| Area | Class | IDs | Count |
|---|---|---|---|
| Login & JWT | `AuthIntegrationTest` | AUTH-01…08 | 8 |
| Dashboard & products | `DashboardIntegrationTest` | DASH-01/02, PROD-01/02, CUST-01 | 5 |
| Registration | `RegistrationIntegrationTest` | REG-01…09 | 9 |
| Payments | `PaymentIntegrationTest` | PAY-01…13 | 13 |
| Search | `SearchIntegrationTest` | SRCH-01…07 | 7 |
| AI | `RecommendationIntegrationTest` | AI-01…05 | 5 |
| AI client | `GeminiRecommendationClientTest` | AI-C-01…05 | 5 |
| Payment rules | `PremiumRulesTest` | RULE-01…05 (parameterized) | 13 |
| Schedule maths | `PremiumScheduleCalculatorTest` | CALC-01…05 | 5 |
| Search validation | `SearchCriteriaTest` | SRCH-V-01/02 | 2 |
| Fallback + age | `RuleBasedRecommenderTest` | FALLBACK-01…03, AGE-01 | 4 |
| Policy numbers | `PolicyNumberGeneratorTest` | PNUM-01/02 | 2 |
| Entity mapping | `RepositoryMappingTest` | (Step 3) | 6 |
| App starts | `HealthDekhoApplicationTests` | | 1 |

**Data-isolation negative tests (your requirement):** **PAY-09** (Step 7) and **SRCH-02** (Step 8). Customer A asks for customer B's policy → **404**, and nothing is paid.

### Coverage (JaCoCo, 26 Nov 2026)

| Package | Line | Branch |
|---|---|---|
| **service** (business rules) | **96.9%** | **83.4%** |
| controller | 100% | 100% |
| repository | 100% | 100% |
| security | 92.5% | 100% |
| ai | 91.5% | 56.2% |
| **whole project** | **94.4%** | **77.2%** |

**Build rule** (`pom.xml`): `mvnw verify` **fails** if the service package drops below 80% line coverage.

**Known gaps (for the PPT "coverage notes"):**
- `exception` package (~67% lines): handlers for rare framework errors (e.g. `ConstraintViolationException`, generic 500) aren't triggered by any test. Low risk, because they only format messages.
- `config` branches (50%): the "JWT_SECRET not set → random key" branch only runs in dev, not in tests (tests set a fixed secret).
- No load or concurrency test for two simultaneous payments; protected by the `SELECT … FOR UPDATE` lock (Step 7).

---

## Part 3 – Run and check

**One-time on each computer:** create an empty database **`healthdekho_test`** (pgAdmin → Databases → Create). Flyway fills it on the first test run.

```powershell
cd backend
.\mvnw test       # runs all 85 tests (~30 s)
.\mvnw verify     # tests + coverage report + 80% check
```

Expected ending:

```
[INFO] Tests run: 85, Failures: 0, Errors: 0, Skipped: 0
[INFO] All coverage checks have been met.
[INFO] BUILD SUCCESS
```

**Coverage report:** open `backend/target/site/jacoco/index.html` in your browser, then click `com.healthdekho.service` for per-class detail (green = tested, red = not).

**Run one test class:** `.\mvnw test -Dtest=PaymentIntegrationTest`

**In VS Code:** the Testing panel (flask icon) shows every test with its `@DisplayName`, so you can run them one by one.

---

## Part 4 – Screenshots

See `docs/screenshot-checklist.md` → rows **TEST-01 … TEST-04** (slides 12–13).

---

## Quick recap

- **Unit tests** check pure rules fast; **integration tests** check the real app end to end on a real database.
- **Reliable tests:** separate test DB, rollback after each test, fixed clock, mocked AI.
- **85 tests, 0 failures; service layer 96.9% line / 83.4% branch.**
- **`mvnw verify` fails if coverage drops**, so quality can't silently decrease.

**Next:** Step 12, the React frontend (a separate run), using `docs/api-reference.md`.
