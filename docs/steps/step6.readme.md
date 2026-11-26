# Step 6 – Policy Registration

> **In one line:** the customer picks a product, the backend checks every rule, calculates the dates and money, and saves the policy **together with its full payment schedule** in one transaction.

**Covers:** FR-3 (policy registration interface, business and validation rules)

---

## Part 1 – The theory

### 1.1 Two endpoints: preview, then register

| Endpoint | Why |
|---|---|
| `GET /api/policies/preview?productId=203` | Fills the registration form: customer details, product details, start date, **maturity date**, **total premium**, number of installments, and whether the customer is eligible. **Saves nothing.** |
| `POST /api/policies` | Actually registers. Runs every check again (the preview could be out of date) and saves. |

### 1.2 Dates with `LocalDate` (Java Time API)

`LocalDate` = a date without time or zone, exactly what an insurance date is.

```java
LocalDate start    = LocalDate.of(2026, 11, 20);
LocalDate maturity = start.plusMonths(240).minusDays(1);   // 2046-11-19 (last day of cover)
int age = Period.between(dateOfBirth, start).getYears();    // completed years only
```

**The `Clock` trick:** we never call `LocalDate.now()` directly. We call `LocalDate.now(clock)` with an injected `Clock` (set to Asia/Kolkata). In tests we inject a **fixed** clock, so "today" is always 26 Nov 2026 and the tests give the same result every day.

**Month-end safety:** every due date is calculated from the **start date** (`start.plusMonths(3 × k)`), not from the previous due date. Otherwise 31 Jan → 28 Feb → 28 Mar → … would slowly drift.

### 1.3 Money with `BigDecimal`

```java
0.1 + 0.2 == 0.30000000000000004   // double: wrong for money ❌
new BigDecimal("0.1").add(new BigDecimal("0.2"))   // 0.3 exactly ✅
```

- Always create from a **String** (`new BigDecimal("1250.00")`), never from a double.
- Use `.add()`, `.multiply()`, `.setScale(2, RoundingMode.HALF_UP)`.
- Compare with `compareTo`, not `equals` (`1250.0` vs `1250.00` are not `equals`).

### 1.4 `@Transactional`: all or nothing

Registering writes **1 policy row + up to 240 installment rows**. If row 150 fails, we must not keep a half-saved policy.

```java
@Transactional
public PolicyRegistrationResponse register(...) {
    policyRepository.save(policy);            // INSERT customer_policy
    installmentRepository.saveAll(list);      // INSERT premium_installment × N
}   // ← success: COMMIT everything.  Exception: ROLLBACK everything.
```

> 💡 **Analogy:** a bank transfer. Taking money from A and adding it to B happen together, or neither happens.

### 1.5 Validation in two layers

| Layer | Tool | Checks |
|---|---|---|
| **Request shape** | Bean Validation (`@NotNull`, `@Positive`) + `@Valid` | "productId is present and positive" → 400 with `fieldErrors` |
| **Business rules** | Java code in the service | age range, duplicate, dates, active product → 400 / 404 / 409 |

The **database constraints** from Step 2 are a third safety net (e.g. `UQ(customer_id, product_id)`).

---

## Part 2 – What we built

```
service/PremiumScheduleCalculator.java   ← pure maths: maturity, total, installment list
service/PolicyRegistrationService.java   ← preview() and register(): all the rules
service/PolicyNumberGenerator.java       ← HDP-<year>-<0001>
service/AgeCalculator.java               ← completed years between two dates
controller/PolicyController.java         ← GET /api/policies/preview, POST /api/policies
```

### The calculations (`PremiumScheduleCalculator`)

| Value | Formula | Example (Smart Savings, start 26 Nov 2026) |
|---|---|---|
| Installments | term months ÷ months per installment | 120 ÷ 3 (QUARTERLY) = **40** |
| Maturity date | start + term months − 1 day | **2036-11-25** |
| Total premium payable | premium × installments | 7,500 × 40 = **300,000.00** |
| Due date of installment *k* | start + (k − 1) × months per installment | #2 → **2027-02-26** |
| Grace period end | due date + 1 month | #2 → **2027-03-26** |

`PremiumFrequency` (from Step 3) knows its months: MONTHLY 1, QUARTERLY 3, HALF_YEARLY 6, YEARLY 12.

### The rules (in order)

| # | Rule | Result if broken |
|---|---|---|
| 1 | Product exists | **404** "Product 999 not found" |
| 2 | Customer doesn't already hold this product | **409** "You already have a policy for…" |
| 3 | Product is active | 400 |
| 4 | Premium, sum insured and term are positive | 400 |
| 5 | Start date is not in the past (default: today) | 400 |
| 6 | Date of birth is not in the future | 400 |
| 7 | Age **on the start date** is within min–max entry age | 400 "Your age … is outside the entry age range 21-55" |
| 8 | Maturity date is after the start date | 400 |

### Request flow

```
POST /api/policies  { "productId": 203 }        (Bearer token)
  │
  ▼
PolicyController.register()        @Valid → 400 if productId missing
  │   customerId from JWT
  ▼
PolicyRegistrationService.register()     @Transactional
  ├─ CustomerService.getCustomer()            → DB
  ├─ InsuranceProductRepository.findById()    → DB (404)
  ├─ existsByCustomer_IdAndProduct_Id()       → DB (409)
  ├─ eligibilityProblem()                     → 400
  ├─ PremiumScheduleCalculator.plan()         → dates + BigDecimal total + list of installments
  ├─ PolicyNumberGenerator.next()             → "HDP-2026-0003"
  ├─ policyRepository.save(policy)            → INSERT
  ├─ installmentRepository.saveAll(…)         → INSERT × 40
  └─ (optional) mark AI recommendation item as selected
  │
  ▼
201 Created   Location: /api/policies/HDP-2026-0003
{ policyNumber, message: "Policy HDP-2026-0003 registered successfully", policy, numberOfInstallments, totalPremiumPayable, firstDueDate }
```

### Policy number

`HDP-<year>-<4-digit sequence>`: the highest existing number for this year + 1. If two registrations ever happen at the very same moment and pick the same number, the database's unique constraint rejects the second one (409) instead of saving a duplicate.

### Registering from an AI recommendation

The request may include `recommendationItemId` (from Step 9). The service checks that the recommendation belongs to this customer and is for the same product, then sets `selected_for_registration = true` (audit trail: "this AI suggestion led to a sale").

### Why some values are copied onto the policy

`customer_policy` stores its own `premium_amount`, `premium_frequency` and `sum_insured`. If the product catalog changes next year, **policies already sold keep their original terms**.

---

## Part 3 – Run and check

Postman folder **3. Registration**:

| Request | Expected |
|---|---|
| REG-01 Preview Smart Savings | maturity 10 years − 1 day, 40 installments, total 300000.00, `eligible: true` |
| REG-03 Register Smart Savings | **201**, `HDP-2026-0003`, confirmation message |
| REG-04 Register Secure Term again | **409** duplicate |
| REG-05 Start date in the past | **400** |
| REG-07 Unknown product | **404** |

Check the database (pgAdmin):

```sql
SELECT installment_number, due_date, grace_period_end_date, scheduled_amount
FROM premium_installment i JOIN customer_policy p ON p.policy_id = i.policy_id
WHERE p.policy_number = 'HDP-2026-0003' ORDER BY installment_number;
```

**Automated tests:** `RegistrationIntegrationTest` (REG-01…09) and `PremiumScheduleCalculatorTest` (CALC-01…05, including the README sample: 240 × 1,250 = 300,000, maturity 2046-11-19).

---

## Part 4 – Screenshots

See `docs/screenshot-checklist.md` → rows **REG-01, REG-03, REG-04, REG-06, DB-REG** (slide 8).

---

## Quick recap

- **Preview** calculates without saving; **register** re-checks everything and saves.
- **`LocalDate` + injected `Clock`** for dates; **`BigDecimal`** for money.
- **`@Transactional`**: the policy and all its installments are saved together, or nothing is.
- **Rule failures:** 400 (rule), 404 (unknown product), 409 (duplicate).

**Next, Step 7:** viewing the schedule and paying premiums.
