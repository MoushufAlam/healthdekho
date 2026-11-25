# Step 3 – Entities, Enums and Repositories

> **In one line:** we wrote Java classes that mirror our 7 tables, so the app can read and write the database with Java objects instead of SQL.

---

## Part 1 – The theory

### 1.1 Quick JPA refresher

You know this part already, so here's just the reminder:

| Piece | Job |
|---|---|
| `@Entity` class | One Java class = one table. One object = one row. |
| `@Column` | Field ↔ column (we write `name = "..."` because Java uses `camelCase`, SQL uses `snake_case`) |
| Repository | An interface; Spring writes the code for `save`, `findById`, `findAll`, `count`… |

**New this time:** the tables already exist (Flyway made them in Step 2). The entities must **match** them, not create them.

### 1.2 `ddl-auto: validate`: the safety check

On startup, Hibernate compares every entity with the real table:

```
Customer.java                     customer table (made by Flyway)
  fullName  → full_name ✅          full_name  VARCHAR ✅
  phone     → phone     ✅          phone      VARCHAR ✅
  emial     → emial     ❌  ──►  App STOPS: "missing column [emial] in table [customer]"
```

A typo shows up **immediately at startup**, not weeks later as a strange bug.

### 1.3 Enums: STRING, never ORDINAL

Our status columns contain text like `'ACTIVE'`. In Java we use enums:

```java
public enum PaymentStatus { PAID, PENDING, PARTIALLY_PAID, OVERDUE }

@Enumerated(EnumType.STRING)      // stores the NAME: 'PENDING'
private PaymentStatus paymentStatus;
```

**Why not `EnumType.ORDINAL`?** ORDINAL stores the *position* (PAID=0, PENDING=1…). If someone later adds a value in the middle, every number shifts, and old rows silently mean something else. STRING is readable in pgAdmin and safe.

**Enums can carry data.** `PremiumFrequency` knows its months:

```java
MONTHLY(1), QUARTERLY(3), HALF_YEARLY(6), YEARLY(12)

PremiumFrequency.QUARTERLY.getMonths()   // 3  → used in Step 6 to build the payment schedule
```

### 1.4 Relationships: `@ManyToOne` and LAZY loading

A policy belongs to one customer, and a customer can have many policies:

```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "customer_id")     // the foreign key column
private Customer customer;
```

- **LAZY** means: when you load a policy, Hibernate does **not** load the customer yet. It loads it only when you call `policy.getCustomer()`.
- **EAGER** would load the customer every single time, even when you don't need it, which means extra queries everywhere.

**One-way only.** The policy knows its customer, but the customer has no list of policies. To get them, we ask the repository (`policyRepository.findByCustomerId(...)`, added in Step 5). Nothing is lost: every foreign key in the ER diagram is mapped.

> ⚠️ **LAZY catch:** `getCustomer()` needs an open database session. In a service method marked `@Transactional` it works. Outside one you get `LazyInitializationException`. We'll handle this properly from Step 5.

### 1.5 Lombok, and why not `@Data`

Lombok writes boring code for you at compile time:

```java
@Getter @Setter @NoArgsConstructor     // = all getters + setters + empty constructor
public class Customer { ... }
```

We **don't** use `@Data` on entities. Its `equals/hashCode` uses the `id`, which changes after `save()`, and its `toString` touches LAZY relations, which can trigger hidden queries or infinite loops.

JPA itself **requires** the empty constructor (`@NoArgsConstructor`), because Hibernate creates the object first and fills the fields afterwards.

### 1.6 Other small things in the code

| Thing | Meaning |
|---|---|
| `Optional<Customer> findByEmail(...)` | Spring builds `WHERE email = ?` from the **method name**. `Optional` = "maybe found", so no `null` checks. |
| `BigDecimal` | Exact money maths. Compare with `compareTo`, not `equals` (`7500` vs `7500.00` are "not equal" with equals!). |
| `Instant` + `@CreationTimestamp` | Hibernate fills `created_at` automatically on insert; `@UpdateTimestamp` refreshes `updated_at` on every update. |
| `@Immutable` (on `PremiumPayment`) | Hibernate will never send an UPDATE for this entity. Payments are history. |

### 1.7 Entities never leave the backend

Entities are for the database only. The frontend will get **DTOs**, small Java `record`s with just the fields it needs (from Step 5). That way we never leak `password_hash`, and a database change doesn't break the API.

---

## Part 2 – What we built

```
backend/src/main/java/com/healthdekho/
├── entity/
│   ├── enums/   CustomerStatus, PolicyType, PremiumFrequency, PolicyStatus,
│   │            PaymentStatus, PaymentMethod, Suitability
│   ├── Customer.java                  → customer
│   ├── InsuranceProduct.java          → insurance_product
│   ├── CustomerPolicy.java            → customer_policy      (→ Customer, → InsuranceProduct)
│   ├── PremiumInstallment.java        → premium_installment  (→ CustomerPolicy)
│   ├── PremiumPayment.java            → premium_payment      (→ PremiumInstallment)  @Immutable
│   ├── AiRecommendationRequest.java   → ai_recommendation_request (→ Customer)
│   └── AiRecommendationItem.java      → ai_recommendation_item    (→ Request, → InsuranceProduct)
└── repository/
    └── 7 interfaces, e.g. CustomerRepository with findByEmail(...)
```

`pom.xml` now has **Lombok** too.

**How the pieces connect** (used from Step 4 onwards):

```
Controller → Service → Repository → Entity ↔ table in PostgreSQL
```

---

## Part 3 – Run and check

```powershell
cd backend
.\mvnw test
```

`RepositoryMappingTest` reads the Step 2 sample data back through the new classes:

| Test | Proves |
|---|---|
| `findByEmail_returnsSeededCustomer` | Columns, `LocalDate` and enum mapping work; the password is a BCrypt hash |
| `findByEmail_unknownEmail_returnsEmptyOptional` | `Optional.empty()` instead of `null` |
| `product_enumsAndMoneyAreMapped` | `QUARTERLY` → 3 months; `BigDecimal` money |
| `policy_manyToOneRelationsLoad` | LAZY `getCustomer()` / `getProduct()` load correctly |
| `aiItem_linksRequestAndProduct` | Two-level relation: item → request → customer |
| `seedRowCounts` | All 7 tables are readable (3, 3, 2, 241, 3, 2, 3 rows) |

**Verified on 2026-11-26:** 7 tests run, 0 failures, `BUILD SUCCESS` ✅. Schema validation passed ✅.

**Harmless warning:** *"Mockito is currently self-attaching…"*. It comes from the test library; ignore it.

**Screenshots:** none for this step. The passing tests appear on slide 12 later (Step 11).

---

## Quick recap

- **Entity = table, object = row.** Flyway owns the tables; Hibernate only **validates**.
- **Enums use `EnumType.STRING`**, never ORDINAL.
- **`@ManyToOne(LAZY)`** loads related rows only when asked; relations are one-way.
- **Lombok `@Getter @Setter @NoArgsConstructor`**, never `@Data`, on entities.
- **Entities stay inside the backend**; the API uses DTO records.

**Next, Step 4:** login with **Spring Security** + **JWT**. There's a full theory section, since this is new to you.
