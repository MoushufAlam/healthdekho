# Step 8 – Policy Search with Pagination

> **In one line:** search your policies by policy number, by name, or by policy number + date of birth. Results come in **pages**, and someone else's policy is never found.

**Covers:** FR-5 (customer insurance policy search)

---

## Part 1 – The theory

### 1.1 Exactly three ways to search

| Search | Example | Allowed? |
|---|---|---|
| Policy number | `?policyNumber=HDP-2026-0001` | ✅ |
| Customer name | `?customerName=aarav` | ✅ (partial, case-insensitive) |
| Policy number + date of birth | `?policyNumber=HDP-2026-0001&dateOfBirth=1990-04-15` | ✅ both must match |
| Nothing | `/search` | ❌ 400 |
| Date of birth alone | `?dateOfBirth=1990-04-15` | ❌ 400 |
| Number **and** name | both given | ❌ 400 |

### 1.2 Sealed interfaces + records + pattern-matching switch (Java 21)

We model "one of three search types" directly in Java:

```java
public sealed interface SearchCriteria {
    record ByPolicyNumber(String policyNumber) implements SearchCriteria { }
    record ByCustomerName(String customerName) implements SearchCriteria { }
    record ByPolicyNumberAndDateOfBirth(String policyNumber, LocalDate dateOfBirth) implements SearchCriteria { }
}
```

- **`sealed`** = "these three, and no others". The compiler knows the full list.
- **Record patterns** in a switch take the record apart and give you its fields as variables:

```java
Page<CustomerPolicy> policies = switch (criteria) {
    case ByPolicyNumber(String number)                     -> ...;
    case ByPolicyNumberAndDateOfBirth(String number, LocalDate dob) -> ...;
    case ByCustomerName(String name)                       -> ...;
};   // no "default" needed: the compiler checks all 3 cases are covered
```

If someone adds a fourth search type later, the code **won't compile** until it is handled.

### 1.3 Pagination

Returning 10,000 rows at once is slow and useless. **Pagination** returns one page at a time:

```
?page=0&size=10   → rows 1-10
?page=1&size=10   → rows 11-20
```

Spring Data does the hard work:

```java
Pageable pageable = PageRequest.of(page, size, Sort.by(DESC, "startDate"));
Page<CustomerPolicy> result = repository.searchOwnedByCustomerName(customerId, name, pageable);
// → SQL ... ORDER BY start_date DESC LIMIT 10 OFFSET 0   + a COUNT(*) query for the total
```

Our response always has the same shape:

```json
{ "content": [ ... ], "page": 0, "size": 10, "totalElements": 3, "totalPages": 1 }
```

`size` is limited to **1–50** (`@Min`, `@Max`), which is the "bounded result retrieval" the README asks for.

### 1.4 Efficient totals: aggregate in SQL

Each search row shows **total paid** and **remaining**. Loading all 240 installments per policy just to add them up would be wasteful. One grouped SQL query does it for the whole page:

```sql
SELECT policy_id,
       SUM(CASE WHEN status = 'PAID' THEN scheduled_amount ELSE paid_amount END) AS premium_paid,
       SUM(CASE WHEN status <> 'PAID' AND grace_period_end_date < :today THEN 1 ELSE 0 END) AS overdue
FROM premium_installment WHERE policy_id IN (...) GROUP BY policy_id
```

In JPQL this fills the `PolicyPaymentTotals` record directly (`select new ...PolicyPaymentTotals(...)`). The results go into a map with `Collectors.toMap(PolicyPaymentTotals::policyId, identity())`.

### 1.5 Scoped to the logged-in customer

Every branch of the search includes `customerId` from the JWT:
- Another customer's policy number → **404** (same rule as Step 7).
- Searching another customer's **name** → empty list + "No policies match the search criteria".

---

## Part 2 – What we built

```
service/SearchCriteria.java       ← sealed interface + validation (3 combinations, formats)
service/PolicySearchService.java  ← switch over criteria, pagination, totals, payment status
repository/CustomerPolicyRepository.searchOwnedByCustomerName(...)   ← paged JPQL + count query
repository/PremiumInstallmentRepository.sumByPolicyIds(...)          ← grouped totals
dto/search/PolicySearchResultResponse, PremiumPaymentStatus
dto/common/PageResponse<T>        ← stable JSON for pages
```

### Request flow

```
GET /api/policies/search?customerName=aarav&page=0&size=10      (Bearer token)
  │
  ▼
PolicyController.search()     @Min/@Max on page/size → 400
  │   customerId from JWT
  ▼
PolicySearchService.search()       @Transactional(readOnly = true)
  ├─ SearchCriteria.of(...)        → 400 for bad combinations / formats
  ├─ switch (criteria)             → repository query, always "AND customer_id = :me"
  │      number      → findOwnedPolicy        (404 if not found)
  │      number+DOB  → findOwnedPolicy + DOB  (404 if no match)
  │      name        → searchOwnedByCustomerName(pageable)   (empty page if none)
  ├─ sumByPolicyIds(ids)           → one grouped SQL for the page
  └─ map to PolicySearchResultResponse
  │
  ▼
PageResponse { content[], page, size, totalElements, totalPages, message? }
```

### Result columns (FR-5B)

Policy number, customer name, date of birth, policy name, policy type, start date, maturity date, total premium payable, **premium payment status**, total premium paid, remaining premium.

**Premium payment status** (policy level):

| Value | When |
|---|---|
| `FULLY_PAID` | Policy is FULLY_PAID or nothing remains |
| `OVERDUE` | At least one unpaid installment is past its grace period |
| `ON_TRACK` | Otherwise |

### Input validation

| Input | Rule |
|---|---|
| Policy number | letters, digits, `-`, 3–30 characters (converted to upper case) |
| Customer name | 2–100 letters, spaces, `.`, `'`, `-`. This also keeps SQL `LIKE` wildcards (`%`, `_`) out. |
| Date of birth | ISO format `yyyy-MM-dd`, otherwise 400 "Parameter 'dateOfBirth' has an invalid value" |
| page / size | page ≥ 0, 1 ≤ size ≤ 50 |

---

## Part 3 – Run and check

Postman folder **5. Search**:

| Request | Expected |
|---|---|
| SRCH-01 By policy number | 1 row with paid 1750.00, remaining 298250.00, `ON_TRACK` |
| SRCH-03 Number + DOB | 1 row |
| SRCH-04 By name `aarav` | Aarav's policies |
| SRCH-05 By name, size=1 | `totalPages` > 1 if Aarav has more than one policy (register one in Step 6 first) |
| SRCH-04b Name `Meera` (as Aarav) | empty + message |
| SRCH-02 Other customer's number | **404** (isolation) |
| SRCH-07 DOB alone | **400** |

**Automated tests:** `SearchIntegrationTest` (SRCH-01…07, including **SRCH-02 customer A → customer B's policy = 404** and pagination with 3 policies at page size 2) and `SearchCriteriaTest`.

---

## Part 4 – Screenshots

See `docs/screenshot-checklist.md` → rows **SRCH-01 … SRCH-07** (slide 10).

---

## Quick recap

- **Three allowed searches**, modelled as a **sealed interface** and handled by a **pattern-matching switch**.
- **Pagination** with `Pageable` → `LIMIT/OFFSET` + total count; page size capped at 50.
- **Totals in one grouped SQL query** instead of loading every installment.
- **Always scoped to the logged-in customer**: other customers' policy numbers → 404.

**Next, Step 9:** AI recommendations.
