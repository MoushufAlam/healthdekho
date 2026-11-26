# Step 5 – Dashboard and Available Products

> **In one line:** a logged-in customer sees **their own** policies, plus the products they **haven't bought yet**.

**Covers:** FR-1B (customer dashboard), FR-1C (view available insurance policies)

---

## Part 1 – The theory

### 1.1 DTOs: why we never return entities

An **entity** is a copy of a table row. A **DTO** (Data Transfer Object) is the shape we **choose** to send to the frontend.

```
Customer entity (DB shape)               CustomerProfileResponse (API shape)
  id, email, passwordHash ❌,              customerId, fullName, email,
  fullName, dateOfBirth, phone,     ──►    phone, dateOfBirth, age
  status, createdAt, updatedAt
```

Why this matters:
1. **Safety:** `passwordHash` can never leak by accident.
2. **Stability:** renaming a DB column doesn't break the React app.
3. **No lazy-loading surprises:** Jackson never touches a LAZY relation outside a transaction.

### 1.2 Java records: perfect DTOs

```java
public record PolicySummaryResponse(String policyNumber, String policyName, ...) { }
```

One line gives you a constructor, getters (`policyNumber()`), `equals`, `hashCode` and `toString`. Records are **immutable**: once created, they can't change, which is exactly what a response should be.

Each DTO has a small factory method `from(entity)`, so the conversion lives in one place:

```java
policies.stream().map(PolicySummaryResponse::from).toList();
```

### 1.3 The Stream API: "filter what's left"

FR-1C says: show active products, **minus** the ones the customer already has. With streams this reads almost like the sentence:

```java
Set<Long> ownedProductIds = myPolicies.stream()
        .map(policy -> policy.getProduct().getId())   // policy → its product id
        .collect(Collectors.toSet());                 // {201}

return activeProducts.stream()
        .filter(product -> !ownedProductIds.contains(product.getId()))  // drop owned ones
        .toList();                                                       // [202, 203]
```

| Stream step | Meaning |
|---|---|
| `.stream()` | Start a pipeline over the list |
| `.map(x -> y)` | Turn each element into something else |
| `.filter(x -> condition)` | Keep only elements where the condition is true |
| `.collect(Collectors.toSet())` | Gather the results into a Set (fast `contains`) |
| `.toList()` | Gather into an unmodifiable List |

### 1.4 The N+1 query problem and `join fetch`

A policy's product is **LAZY**. Loading 10 policies and then calling `getProduct()` on each runs **1 + 10 queries**. Our repository fetches them together in **one** query:

```java
@Query("""
        select p from CustomerPolicy p
        join fetch p.product                ← load the product in the same SQL
        where p.customer.id = :customerId
        """)
List<CustomerPolicy> findAllByCustomerIdWithProduct(Long customerId);
```

### 1.5 `@Transactional(readOnly = true)`

Services that only read are marked `readOnly`. Hibernate then skips change-tracking (faster), and the DB session stays open while we map entities to DTOs.

---

## Part 2 – What we built

| Endpoint | Returns |
|---|---|
| `GET /api/customers/me` | Profile of the logged-in customer (used to pre-fill forms) |
| `GET /api/dashboard` | Customer name, number of policies, the policy table, and a message when empty |
| `GET /api/products/available` | Active products the customer doesn't own yet (each has a `productId` for the Register button) |

### Request flow: dashboard

```
GET /api/dashboard  (Bearer token)
  │
  ▼
CustomerController.dashboard(@AuthenticationPrincipal Jwt jwt)
  │   customerId = CurrentCustomer.id(jwt)           ← from the token
  ▼
PolicyService.getDashboard(customerId)
  ├─ CustomerService.getCustomer(id)          → customer name
  └─ CustomerPolicyRepository.findAllByCustomerIdWithProduct(id)   → one SQL with JOIN
        └─ stream().map(PolicySummaryResponse::from).toList()
  │
  ▼
DashboardResponse { customerName, totalPolicies, policies[], message }
```

**Dashboard table fields (FR-1B):** policy number, policy name, policy type, term, start date, maturity date, premium frequency, premium amount, sum insured, status.

**Empty dashboard:** `policies: []` plus `"message": "You have not registered any insurance policy yet..."`. The frontend just shows the message.

### Request flow: available products

```
GET /api/products/available
  → CustomerController → ProductService.getAvailableProducts(customerId)
       ├─ policyRepository.findAllByCustomerIdWithProduct(id)  → owned product ids (Set)
       ├─ productRepository.findByActiveTrueOrderByProductCodeAsc()  → active products
       └─ filter out owned → map to ProductResponse
```

**Duplicate rule:** one policy per customer per product (`UQ(customer_id, product_id)` in the DB). That's why "already owned" means "hide it".

---

## Part 3 – Run and check

Postman folder **2. Dashboard & products**:

| Request | Expected |
|---|---|
| DASH-01 Dashboard (Aarav) | 1 policy: `HDP-2026-0001`, Secure Term |
| DASH-02 Dashboard (Rohan) | `policies: []` + message |
| PROD-01 Available (Aarav) | Family Health + Smart Savings (**not** Secure Term, which he owns) |
| CUST-01 My profile | Aarav's name, DOB, phone, age 36 |

**Automated tests:** `DashboardIntegrationTest` (DASH-01, DASH-02, PROD-01, PROD-02 inactive products hidden, CUST-01).

---

## Part 4 – Screenshots

See `docs/screenshot-checklist.md` → rows **DASH-01, DASH-02, PROD-01**.

---

## Quick recap

- **Entities stay inside; records (DTOs) go out.**
- **Streams:** `map` transforms, `filter` keeps, `collect`/`toList` gathers.
- **`join fetch`** avoids the N+1 problem.
- **Every query uses the customer ID from the token**, so each customer sees only their own data.

**Next, Step 6:** registering a new policy.
