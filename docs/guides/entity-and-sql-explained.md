# Entities and SQL, Explained Together

> **In one line:** the SQL file (`V1__create_schema.sql`) **builds** the tables. The entity classes (`entity/*.java`) **describe** those same tables to Java, so we can work with objects instead of writing SQL.
>
> "Entity" and "model" mean the same thing here: a Java class that represents one table.

---

## Part 1 – The 7 tables at a glance

**Legend:** 🔑 = Primary Key · 🔗 = Foreign Key (points to another table) · ⭐ = Unique · ✔️ = CHECK rule

### `customer`: people who log in
| Column | SQL type | Rules | Java field |
|---|---|---|---|
| customer_id | BIGINT (auto) | 🔑 | `Long id` |
| email | VARCHAR(150) | ⭐ not null | `String email` |
| password_hash | VARCHAR(100) | not null | `String passwordHash` |
| full_name | VARCHAR(150) | not null | `String fullName` |
| date_of_birth | DATE | ✔️ not in the future | `LocalDate dateOfBirth` |
| phone | VARCHAR(15) | not null | `String phone` |
| status | VARCHAR(20) | ✔️ ACTIVE / INACTIVE / LOCKED | `CustomerStatus status` (enum) |
| created_at, updated_at | TIMESTAMPTZ | auto-filled | `Instant createdAt, updatedAt` |

### `insurance_product`: the catalog of products
| Column | SQL type | Rules | Java field |
|---|---|---|---|
| product_id | BIGINT (auto) | 🔑 | `Long id` |
| product_code | VARCHAR(30) | ⭐ | `String productCode` |
| policy_name | VARCHAR(150) | not null | `String policyName` |
| policy_type | VARCHAR(20) | ✔️ TERM / HEALTH / SAVINGS / LIFE | `PolicyType` (enum) |
| term_months | INTEGER | ✔️ > 0, fits the frequency | `Integer termMonths` |
| premium_frequency | VARCHAR(20) | ✔️ MONTHLY / QUARTERLY / HALF_YEARLY / YEARLY | `PremiumFrequency` (enum) |
| premium_amount | NUMERIC(19,2) | ✔️ > 0 | `BigDecimal premiumAmount` |
| sum_insured | NUMERIC(19,2) | ✔️ > 0 | `BigDecimal sumInsured` |
| min_entry_age, max_entry_age | INTEGER | ✔️ min ≤ max | `Integer minEntryAge, maxEntryAge` |
| active | BOOLEAN | default TRUE | `Boolean active` |
| created_at, updated_at | TIMESTAMPTZ | auto-filled | `Instant` |

### `customer_policy`: a product a customer bought
| Column | SQL type | Rules | Java field |
|---|---|---|---|
| policy_id | BIGINT (auto) | 🔑 | `Long id` |
| policy_number | VARCHAR(30) | ⭐ e.g. HDP-2026-0001 | `String policyNumber` |
| customer_id | BIGINT | 🔗 → customer | `Customer customer` |
| product_id | BIGINT | 🔗 → insurance_product | `InsuranceProduct product` |
| start_date, maturity_date | DATE | ✔️ maturity > start | `LocalDate` |
| premium_frequency | VARCHAR(20) | ✔️ 4 allowed values | `PremiumFrequency` |
| premium_amount, sum_insured, total_premium_payable | NUMERIC(19,2) | ✔️ > 0 | `BigDecimal` |
| status | VARCHAR(20) | ✔️ ACTIVE / FULLY_PAID / LAPSED / MATURED / CANCELLED | `PolicyStatus` |
| created_at, updated_at | TIMESTAMPTZ | auto-filled | `Instant` |

⭐ **(customer_id + product_id) together are unique**: one customer can't buy the same product twice.

### `premium_installment`: each scheduled payment of a policy
| Column | SQL type | Rules | Java field |
|---|---|---|---|
| installment_id | BIGINT (auto) | 🔑 | `Long id` |
| policy_id | BIGINT | 🔗 → customer_policy | `CustomerPolicy policy` |
| installment_number | INTEGER | ✔️ > 0 | `Integer installmentNumber` |
| due_date, grace_period_end_date | DATE | ✔️ grace ≥ due | `LocalDate` |
| scheduled_amount | NUMERIC(19,2) | ✔️ > 0 | `BigDecimal scheduledAmount` |
| paid_amount | NUMERIC(19,2) | ✔️ 0 … scheduled + 10% | `BigDecimal paidAmount` |
| payment_status | VARCHAR(20) | ✔️ PAID / PENDING / PARTIALLY_PAID / OVERDUE | `PaymentStatus` |
| created_at, updated_at | TIMESTAMPTZ | auto-filled | `Instant` |

⭐ **(policy_id + installment_number) together are unique**: no two "installment #2" rows for the same policy.

### `premium_payment`: every actual payment (never edited)
| Column | SQL type | Rules | Java field |
|---|---|---|---|
| payment_id | BIGINT (auto) | 🔑 | `Long id` |
| installment_id | BIGINT | 🔗 → premium_installment | `PremiumInstallment installment` |
| transaction_reference | VARCHAR(40) | ⭐ e.g. TXN-HD-000001 | `String transactionReference` |
| payment_date | DATE | not null | `LocalDate paymentDate` |
| amount_paid | NUMERIC(19,2) | ✔️ > 0 | `BigDecimal amountPaid` |
| penalty_amount | NUMERIC(19,2) | ✔️ ≥ 0 | `BigDecimal penaltyAmount` |
| payment_method | VARCHAR(20) | ✔️ UPI / NET_BANKING / CARD / CASH / CHEQUE | `PaymentMethod` |
| created_at | TIMESTAMPTZ | auto-filled | `Instant createdAt` |

🔒 A trigger blocks any UPDATE or DELETE on this table.

### `ai_recommendation_request`: each time a customer asked the AI
| Column | SQL type | Rules | Java field |
|---|---|---|---|
| recommendation_request_id | BIGINT (auto) | 🔑 | `Long id` |
| customer_id | BIGINT | 🔗 → customer | `Customer customer` |
| insurance_goals | VARCHAR(1000) | optional | `String insuranceGoals` |
| preferred_term | VARCHAR(50) | optional | `String preferredTerm` |
| preferred_policy_type | VARCHAR(20) | optional | `PolicyType` |
| calculated_age | INTEGER | ✔️ ≥ 0 | `Integer calculatedAge` |
| model_name, model_version, prompt_version | VARCHAR | name + prompt required | `String` |
| requested_at | TIMESTAMPTZ | auto-filled | `Instant requestedAt` |

### `ai_recommendation_item`: products the AI suggested in a request
| Column | SQL type | Rules | Java field |
|---|---|---|---|
| recommendation_item_id | BIGINT (auto) | 🔑 | `Long id` |
| recommendation_request_id | BIGINT | 🔗 → ai_recommendation_request | `AiRecommendationRequest request` |
| product_id | BIGINT | 🔗 → insurance_product | `InsuranceProduct product` |
| rank_order | INTEGER | ✔️ > 0 | `Integer rankOrder` |
| recommendation_reason | VARCHAR(1000) | not null | `String recommendationReason` |
| suitability | VARCHAR(10) | ✔️ HIGH / MEDIUM / LOW | `Suitability` |
| selected_for_registration | BOOLEAN | default FALSE | `Boolean selectedForRegistration` |

⭐ In one request: the same product can't appear twice, and the same rank can't appear twice.

### How they connect

```
customer ─┬─< customer_policy >── insurance_product ──┐
          │        │                                  │
          │        └─< premium_installment            │
          │                 │                         │
          │                 └─< premium_payment       │
          │                                           │
          └─< ai_recommendation_request               │
                   │                                  │
                   └─< ai_recommendation_item >───────┘

A ─< B  means "one A has many B" (one customer has many policies)
```

---

## Part 2 – Reading the SQL, piece by piece

Here is a real table from our file, then every piece explained:

```sql
CREATE TABLE customer_policy (
    policy_id      BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    policy_number  VARCHAR(30)   NOT NULL,
    customer_id    BIGINT        NOT NULL REFERENCES customer (customer_id),
    start_date     DATE          NOT NULL,
    maturity_date  DATE          NOT NULL,
    premium_amount NUMERIC(19,2) NOT NULL,
    status         VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
    created_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT uq_policy_number           UNIQUE (policy_number),
    CONSTRAINT uq_policy_customer_product UNIQUE (customer_id, product_id),
    CONSTRAINT ck_policy_dates            CHECK (maturity_date > start_date),
    CONSTRAINT ck_policy_status           CHECK (status IN ('ACTIVE', 'FULLY_PAID', ...))
);
CREATE INDEX idx_policy_customer ON customer_policy (customer_id);
```

### Data types

| SQL | Meaning | Example value |
|---|---|---|
| `BIGINT` | Whole number (very large range) | `3001` |
| `INTEGER` | Whole number (normal range) | `240` |
| `VARCHAR(30)` | Text, **at most** 30 characters | `'HDP-2026-0001'` |
| `NUMERIC(19,2)` | Exact decimal: up to 19 digits, **2 after the point**. Used for money. | `1250.00` |
| `DATE` | Date only, no time | `2026-11-20` |
| `TIMESTAMPTZ` | Date + time + time zone | `2026-11-20 10:15:00+05:30` |
| `BOOLEAN` | `TRUE` / `FALSE` | `TRUE` |

### Column keywords

| SQL | Meaning |
|---|---|
| `GENERATED BY DEFAULT AS IDENTITY` | The database numbers new rows itself (1, 2, 3, …), but we may also give an ID ourselves (the seed data does). |
| `PRIMARY KEY` | The unique ID of each row. Can't be empty, can't repeat. |
| `NOT NULL` | The column must have a value. |
| `DEFAULT 'ACTIVE'` | If no value is given on insert, use `'ACTIVE'`. |
| `DEFAULT now()` | If no value is given, use the current date and time. |
| `REFERENCES customer (customer_id)` | **Foreign key.** The value must exist in `customer.customer_id`, so you can't create a policy for customer 9999 if no such customer exists. |

### Constraints (named rules)

`CONSTRAINT <name> <rule>` gives a rule a name, so errors are readable (`violates uq_policy_customer_product`).

| SQL | Meaning |
|---|---|
| `UNIQUE (policy_number)` | No two rows with the same policy number. |
| `UNIQUE (customer_id, product_id)` | No two rows with the same **pair**. Customer 1001 + product 201 can exist only once. |
| `CHECK (maturity_date > start_date)` | Every row must satisfy this condition, or the insert/update fails. |
| `CHECK (status IN ('ACTIVE', 'FULLY_PAID', ...))` | The value must be one of this list. |

**Naming habit:** `uq_` = unique, `ck_` = check, `idx_` = index, `_pkey` / `_fkey` = names PostgreSQL makes for primary and foreign keys.

### Indexes

```sql
CREATE INDEX idx_policy_customer ON customer_policy (customer_id);
CREATE INDEX idx_customer_name_ci ON customer (lower(full_name));
```

An **index** is like the index at the back of a book. Without one, finding "all policies of customer 1001" means reading every row. With one, PostgreSQL jumps straight to them.
`lower(full_name)` indexes the name in lowercase, so searching "aarav" finds "Aarav" quickly (used by the search screen in Step 8).

### The less common bits in our file

**CHECK with CASE** (`insurance_product`):
```sql
CHECK (term_months % CASE premium_frequency
                         WHEN 'MONTHLY' THEN 1  WHEN 'QUARTERLY' THEN 3
                         WHEN 'HALF_YEARLY' THEN 6  ELSE 12 END = 0)
```
`%` is the remainder. "The term must split into whole installments." A 120-month QUARTERLY product → 120 % 3 = 0 ✅. A 10-month QUARTERLY product → 10 % 3 = 1 ❌.

**Trigger** (`premium_payment`):
```sql
CREATE FUNCTION prevent_payment_change() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'premium_payment rows are immutable ...';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_premium_payment_immutable
    BEFORE UPDATE OR DELETE ON premium_payment
    FOR EACH ROW EXECUTE FUNCTION prevent_payment_change();
```
- **Function:** a small program stored in the database. This one just throws an error.
- **Trigger:** "**before** anyone UPDATEs or DELETEs a row in `premium_payment`, run that function". So every edit or delete fails, and payment history can't be changed.

### The seed file (`V2__seed_data.sql`)

| SQL | Meaning |
|---|---|
| `INSERT INTO customer (...) VALUES (...), (...);` | Add rows. Several rows in one statement, separated by commas. |
| `generate_series(4, 240) AS n` | Produces the numbers 4, 5, 6 … 240, like a `for` loop. We use it to create installments 4–240. |
| `DATE '2026-11-20' + (n - 1) * INTERVAL '1 month'` | Date maths: start date + (n−1) months = due date of installment n. |
| `setval(pg_get_serial_sequence('customer', 'customer_id'), 1003)` | "The next auto ID for customer should continue after 1003." Needed because we inserted fixed IDs. |

---

## Part 3 – Each annotation and the SQL it matches

### Class level

```java
@Entity                                   // "this class is a database table"
@Table(name = "customer_policy",          // ⇄  CREATE TABLE customer_policy
       uniqueConstraints = @UniqueConstraint(
           name = "uq_policy_customer_product",
           columnNames = {"customer_id", "product_id"}))   // ⇄  CONSTRAINT uq_policy_customer_product UNIQUE (customer_id, product_id)
@Getter @Setter @NoArgsConstructor        // Lombok – no SQL meaning
public class CustomerPolicy {
```

| Annotation | Matching SQL | Used at runtime? |
|---|---|---|
| `@Entity` | – (marks the class for JPA) | ✅ yes |
| `@Table(name = "...")` | `CREATE TABLE customer_policy` | ✅ yes, tells Hibernate the table name |
| `uniqueConstraints = ...` | `CONSTRAINT ... UNIQUE (...)` | ❌ no, documentation only |
| `@Getter @Setter @NoArgsConstructor` | – | Lombok writes getters, setters and an empty constructor |

### Primary key

```java
@Id                                                   // ⇄  PRIMARY KEY
@GeneratedValue(strategy = GenerationType.IDENTITY)   // ⇄  GENERATED BY DEFAULT AS IDENTITY
@Column(name = "policy_id")                           // ⇄  policy_id
private Long id;                                      // ⇄  BIGINT
```
`IDENTITY` means: "on `save()`, let the database pick the number, then read it back into `id`".

### Normal columns

```java
@Column(name = "policy_number", nullable = false, unique = true, length = 30)
private String policyNumber;
// ⇄  policy_number VARCHAR(30) NOT NULL  +  UNIQUE (policy_number)
```

| Part | Matching SQL | Used at runtime? |
|---|---|---|
| `name = "policy_number"` | the column name | ✅ yes |
| `nullable = false` | `NOT NULL` | ❌ documentation |
| `unique = true` | `UNIQUE (...)` | ❌ documentation |
| `length = 30` | `VARCHAR(30)` | ❌ documentation |
| `precision = 19, scale = 2` | `NUMERIC(19,2)` | ❌ documentation |
| `updatable = false` (on `created_at`) | – | ✅ yes, Hibernate never includes this column in an UPDATE |

> **Why do some parts do nothing?** They're only used when Hibernate **creates** tables. We use `ddl-auto: validate`, so the real rules live in the SQL and the database enforces them. We keep these parts so the Java class shows the rules too.

### Java type ⇄ SQL type

| Java | SQL |
|---|---|
| `Long` | `BIGINT` |
| `Integer` | `INTEGER` |
| `String` | `VARCHAR(n)` |
| `BigDecimal` | `NUMERIC(19,2)` |
| `LocalDate` | `DATE` |
| `Instant` | `TIMESTAMPTZ` |
| `Boolean` | `BOOLEAN` |
| an `enum` + `@Enumerated(STRING)` | `VARCHAR` + `CHECK (... IN (...))` |

### Enums

```java
@Enumerated(EnumType.STRING)                      // store the enum's NAME as text
@Column(nullable = false, length = 20)
private PolicyStatus status = PolicyStatus.ACTIVE; // ⇄  DEFAULT 'ACTIVE'
// ⇄  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
//    CHECK (status IN ('ACTIVE', 'FULLY_PAID', 'LAPSED', 'MATURED', 'CANCELLED'))
```
The enum's values must be spelled **exactly** like the CHECK list. The enum is Java's version of that list.

### Foreign keys (relations)

```java
@ManyToOne(fetch = FetchType.LAZY, optional = false)   // many policies → one customer
@JoinColumn(name = "customer_id", nullable = false)    // ⇄  customer_id BIGINT NOT NULL
private Customer customer;                             // ⇄  REFERENCES customer (customer_id)
```

| Part | Meaning |
|---|---|
| `@ManyToOne` | Many rows here point to one row there. |
| `@JoinColumn(name = "customer_id")` | The foreign key column in **this** table. |
| `Customer customer` | In Java you get the **whole Customer object**, not just the number 1001. |
| `fetch = LAZY` | Don't load the customer until `getCustomer()` is called. |
| `optional = false` | A policy must always have a customer (matches `NOT NULL`). |

### Timestamps

```java
@CreationTimestamp                                         // Hibernate sets it on INSERT
@Column(name = "created_at", nullable = false, updatable = false)
private Instant createdAt;                                 // ⇄  created_at TIMESTAMPTZ NOT NULL DEFAULT now()

@UpdateTimestamp                                           // Hibernate refreshes it on every UPDATE
@Column(name = "updated_at", nullable = false)
private Instant updatedAt;
```
The SQL's `DEFAULT now()` covers rows inserted with plain SQL (like the seed). The annotations cover rows saved from Java.

### Immutable

```java
@Entity
@Immutable                    // Hibernate never sends UPDATE for this entity
public class PremiumPayment { // ⇄  the trigger that blocks UPDATE/DELETE
```
Two locks on the same door: Java won't try to edit, and even if it did, the database would refuse.

### Not in SQL at all

| Annotation | Where it lives |
|---|---|
| CHECK rules (`premium_amount > 0`, `maturity > start`, age range…) | **Only in SQL.** JPA has no annotation for them. In Java, the service layer checks them first (Steps 6–7), so users get a friendly error before the database ever refuses. |
| Indexes | Only in SQL. |
| Trigger | Only in SQL (`@Immutable` is the Java-side partner). |

---

## Part 4 – One full example, side by side

```sql
-- SQL (V1__create_schema.sql)                        -- Java (PremiumInstallment.java)
CREATE TABLE premium_installment (                    -- @Entity @Table(name = "premium_installment")
  installment_id BIGINT GENERATED BY DEFAULT          -- @Id @GeneratedValue(strategy = IDENTITY)
                 AS IDENTITY PRIMARY KEY,             -- @Column(name = "installment_id") Long id;
  policy_id      BIGINT NOT NULL                      -- @ManyToOne(fetch = LAZY)
                 REFERENCES customer_policy,          -- @JoinColumn(name = "policy_id") CustomerPolicy policy;
  installment_number INTEGER NOT NULL,                -- @Column(name = "installment_number") Integer installmentNumber;
  due_date       DATE NOT NULL,                       -- @Column(name = "due_date") LocalDate dueDate;
  scheduled_amount NUMERIC(19,2) NOT NULL,            -- @Column(precision = 19, scale = 2) BigDecimal scheduledAmount;
  paid_amount    NUMERIC(19,2) NOT NULL DEFAULT 0,    -- BigDecimal paidAmount = BigDecimal.ZERO;
  payment_status VARCHAR(20) NOT NULL                 -- @Enumerated(STRING)
                 DEFAULT 'PENDING',                   -- PaymentStatus paymentStatus = PaymentStatus.PENDING;
  CONSTRAINT uq_installment_policy_number             -- @UniqueConstraint(name = "uq_installment_policy_number",
    UNIQUE (policy_id, installment_number),           --   columnNames = {"policy_id", "installment_number"})
  CONSTRAINT ck_installment_paid                      -- (no annotation, SQL only;
    CHECK (paid_amount >= 0 AND                       --  the service checks this in Step 7)
           paid_amount <= ROUND(scheduled_amount * 1.10, 2))
);
```

---

## Quick recap

- **SQL builds and guards** the tables: types, keys, unique, check, trigger, index.
- **The entity describes** the same table to Java. `@Table` and `@Column(name)` do the real mapping; the rest is mostly documentation.
- **`@ManyToOne` + `@JoinColumn`** turn a foreign key number into a real Java object.
- **CHECK rules, indexes and triggers exist only in SQL.** Java repeats the important checks in services, to give friendly errors.
