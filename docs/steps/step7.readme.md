# Step 7 – Premium Payments

> **In one line:** the customer looks up a policy, sees every installment with what's due today, and pays one or more installments. The backend enforces partial payments, the 10% late penalty, and no double or over-payment.

**Covers:** FR-4 (policy lookup, payment schedule, payment processing)

---

## Part 1 – The theory

### 1.1 Our payment model (the business rules)

| Situation | What the customer may pay |
|---|---|
| **Within the grace period** (due date … due date + 1 month) | Any amount from 0.01 up to the outstanding premium → **partial payments allowed**, no penalty |
| **After the grace period** | Outstanding premium **+ 10% penalty**, in **one full payment** |
| Installment already **PAID** | Nothing → **409** (prevents duplicate payments) |
| More than what's due | Rejected → **400** (prevents over-payment) |

**Why must late installments be paid in full?** The README says "apply a 10% penalty on the outstanding premium". If late partial payments were allowed, the penalty would have to be split across payments, which is complicated and easy to get wrong. "Partial payments wherever applicable" allows us to limit partial payments to the grace period. One clean rule: late = settle everything.

**Example** (installment of 7,500, grace ended 20 Jun 2026, paid 26 Nov 2026):

```
outstanding premium   7,500.00
10% penalty         +   750.00   (rounded HALF_UP to 2 decimals)
must pay            = 8,250.00   → anything else is rejected with this exact breakdown
```

### 1.2 Payment statuses: stored vs. shown

| Status | Meaning |
|---|---|
| `PENDING` | Nothing paid, still within grace |
| `PARTIALLY_PAID` | Some premium paid, still within grace |
| `OVERDUE` | Not fully paid and the grace period has ended |
| `PAID` | Fully paid (including any penalty) |

`OVERDUE` depends on **today's date**, so we calculate it at read time instead of trusting a stored value. A switch expression keeps this readable:

```java
return switch (installment.getPaymentStatus()) {
    case PAID -> PAID;
    case PENDING, PARTIALLY_PAID, OVERDUE -> {
        if (isPastGrace(installment, today)) yield OVERDUE;
        yield paidAmount > 0 ? PARTIALLY_PAID : PENDING;
    }
};
```

**Switch expressions** (Java 14+) return a value, list several cases with commas, and the compiler checks that **every enum value is handled**.

### 1.3 Stopping double payments: database locks

Two clicks on "Pay" → two requests at the same moment → both read "PENDING" → both pay. ❌

We lock the selected installment rows while paying:

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)      // SQL: SELECT ... FOR UPDATE
List<PremiumInstallment> findForUpdate(Long policyId, Collection<Long> ids);
```

The second request **waits** until the first commits, then sees `PAID` and gets **409**.

> 💡 **Analogy:** a fitting-room key. Only one person can be inside; the next one waits at the door.

### 1.4 One transaction for the whole payment

Paying installments #2 and #5 together is **all or nothing**. If #5 is an over-payment, #2 is **not** paid either (rollback). This is tested in PAY-11.

### 1.5 Data isolation (your decision)

Every lookup is `policyNumber + customerId (from the JWT)`:

```java
@Query("... where p.policyNumber = :policyNumber and c.id = :customerId")
Optional<CustomerPolicy> findOwnedPolicy(String policyNumber, Long customerId);
```

Meera asking for Aarav's `HDP-2026-0001` → no row → **404 "Policy HDP-2026-0001 not found"**. We answer 404 rather than 403 on purpose, so nobody can even learn that the policy exists.

### 1.6 Java 21 features used here

| Feature | Where |
|---|---|
| `YearMonth` | Each installment has a `period` like `"2026-12"` (FR-4D) |
| Switch expressions | `effectiveStatus`, `statusAfterPayment`, policy-status check |
| `Collectors.groupingBy` | Status counts in the policy header: `{PAID: 1, PENDING: 238, PARTIALLY_PAID: 1}` |
| `Collectors.toMap` with a merge function | Detects the same installment selected twice |
| `BigDecimal` | Every amount, penalty and total |

---

## Part 2 – What we built

| Endpoint | Purpose |
|---|---|
| `GET /api/policies/{policyNumber}` | FR-4A header: customer, product, dates, **total payable / paid / remaining**, penalties paid, status counts |
| `GET /api/policies/{policyNumber}/installments` | FR-4B schedule: number, period, amount, due date, grace end, paid, outstanding, **penalty if paid today**, **amount due today**, status, `selectable` (for the checkbox) |
| `POST /api/policies/{policyNumber}/payments` | FR-4C: pay one or more installments |
| `GET /api/policies/{policyNumber}/payments` | Payment history (immutable `premium_payment` rows) |

**Payment request:**

```json
{
  "paymentMethod": "UPI",
  "installments": [
    { "installmentId": 4002, "amount": 750.00 },
    { "installmentId": 4005, "amount": 1250.00 }
  ]
}
```

`paymentMethod` is only recorded (UPI / NET_BANKING / CARD / CASH / CHEQUE). No real money moves; there is no payment gateway in scope.

### Files

```
service/PremiumRules.java     ← pure rules: outstanding, penalty, amount due, statuses
service/PaymentService.java   ← pay(): lock, validate, update, record, totals
service/PolicyService.java    ← detail, installments, history, getOwnedPolicy (404)
repository/PremiumInstallmentRepository.java  ← findForUpdate (lock), sumByPolicyIds (totals in SQL)
```

### Request flow: paying

```
POST /api/policies/HDP-2026-0001/payments      (Bearer token)
  │
  ▼
PolicyController.pay()      @Valid: method present, 1-24 lines, amount > 0, max 2 decimals → 400
  │   customerId from JWT
  ▼
PaymentService.pay()        @Transactional (all or nothing)
  ├─ PolicyService.getOwnedPolicy(number, customerId)      → 404 if not yours
  ├─ policy must be ACTIVE (switch)                         → 409 fully paid / 400 lapsed...
  ├─ toAmountMap()                                          → 400 if an installment is listed twice
  ├─ findForUpdate(policyId, ids)  SELECT … FOR UPDATE      → 404 if an id isn't on this policy
  ├─ for each installment: applyPayment()
  │     ├─ already PAID?                                    → 409
  │     ├─ past grace? amount must equal outstanding + 10%  → 400 with the exact amount
  │     ├─ within grace? amount ≤ outstanding               → 400 over-payment
  │     ├─ paidAmount += amount; new status (switch)
  │     └─ INSERT premium_payment (TXN-HD-…, date = today, penalty)
  ├─ all installments PAID? → policy.status = FULLY_PAID
  └─ totals via one SQL aggregate (sumByPolicyIds)
  │
  ▼
201 { receipts[], totalPaidNow, totalPremiumPaid, totalRemainingPremium, policyStatus, message }
```

### How totals are calculated

- **Total premium paid** = for each installment: `scheduledAmount` if PAID, else `paidAmount`. **Penalties are not premium**, so they're reported separately as `totalPenaltyPaid`.
- **Remaining** = total premium payable − total premium paid.
- The policy becomes **FULLY_PAID** only when **every** installment is PAID.

---

## Part 3 – Run and check

Postman folder **4. Policy lookup & payments** (run 1. Auth first):

| Request | Expected |
|---|---|
| PAY-01 Policy details | payable 300000.00, paid 1750.00, remaining 298250.00 |
| PAY-02 Schedule | 240 rows; #1 PAID (not selectable), #2 PARTIALLY_PAID, due today 750.00 |
| PAY-03 Partial payment 500 on #3 | **201**, #3 → PARTIALLY_PAID, remaining drops by 500 |
| PAY-05 Over-payment | **400** "exceeds the outstanding premium" |
| PAY-06 Pay #1 again | **409** "already paid" |
| PAY-09 Meera → Aarav's policy | **404** (isolation) |

**Late penalty:** run `docs/demo/demo-overdue-policy.sql` in pgAdmin first, then:

| Request | Expected |
|---|---|
| PAY-08 Overdue schedule (Rohan) | #1 and #2 OVERDUE, penalty 750.00, due 8250.00 |
| PAY-07a Pay 7500 | **400** "Pay the full amount due 8250.00 (outstanding premium 7500.00 + 10% penalty 750.00)" |
| PAY-07b Pay 8250 | **201**, `penaltyAmount: 750.00`, status PAID |

To start over: `docs/demo/reset-dev-db.sql`, then restart the app.

**Automated tests:** `PaymentIntegrationTest` (PAY-01…13, including **PAY-09 customer A → customer B's policy = 404** and PAY-11 rollback) and `PremiumRulesTest` (RULE-01…05: penalty boundaries, rounding, statuses).

---

## Part 4 – Screenshots

See `docs/screenshot-checklist.md` → rows **PAY-01 … PAY-09** (slide 9).

---

## Quick recap

- **Within grace:** partial payments OK. **After grace:** outstanding + 10% in one go.
- **`SELECT … FOR UPDATE`** lock + `@Transactional` = no double payments, all-or-nothing.
- **OVERDUE is calculated from today's date** with a switch expression.
- **Another customer's policy → 404**, because every query includes the customer ID from the JWT.

**Next, Step 8:** searching policies.
