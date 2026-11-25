# HealthDekho – Build Roadmap & Working Agreement

## How we work (rules)

1. **One step at a time.** Nothing from the next step is built until you say "next".
2. **Backend first.** Frontend comes after the backend works (tested with Postman/curl).
3. **Every step produces a `stepN.readme.md`** in `docs/steps/` that explains:
   - What we built and why
   - How the flow works (request -> controller -> service -> repository -> DB)
   - The key Java 21 / Spring concepts used
   - How to run and test it
   - Which README requirement (FR-x) it satisfies
4. **Before each step** I give a short plan; you confirm; then I build.
5. **After each step** you run it, and we check it works before moving on.
6. Business rules (age, duplicates, money, penalty) live in **Java service code**, never in the AI.

## Big picture flow

```
React UI  --Axios-->  Controller  -->  Service (business rules)  -->  Repository  -->  PostgreSQL
                                          |
                                          +--> AI layer (Spring AI) -- only suggests products from DB
```

Layers: Controller (HTTP) | Service (rules) | Repository (DB) | Entity (tables) | DTO (API shapes, Java records) | AI (recommendations).

## Roadmap

| Step | Goal | Covers | Output you can verify |
|---|---|---|---|
| 0 | Prerequisites: Java 21, Maven, Postgres, IDE, Git | - | `java -version` shows 21; DB reachable |
| 1 | Project skeleton: Spring Boot 3.5.7, dependencies, configs (dev/prod), app starts | Tech stack | App starts, health check works |
| 2 | Database: Flyway migration for 7 tables, constraints, indexes, seed data (BCrypt) | Sec. 5 | Tables + sample rows visible in Postgres |
| 3 | Entities, enums, repositories | Sec. 5 | App boots with schema validation passing |
| 4 | Authentication with Spring Security (login, BCrypt, JWT) | FR-1A | Login works; wrong password gives 401 |
| 5 | Dashboard + available products (stream filtering, DTO records) | FR-1B, 1C | Each customer sees only own policies |
| 6 | Policy registration: eligibility, duplicate rule, maturity date, BigDecimal totals, installment schedule, `@Transactional` | FR-3 | Policy + schedule saved; bad input rejected |
| 7 | Premium payment: lookup, partial pay, 10% penalty after grace, no duplicate/over-payment, status switch expression | FR-4 | Payment statuses update correctly |
| 8 | Policy search with validation + pagination | FR-5 | Search by ID / name / ID+DOB |
| 9 | AI recommendations: Spring AI, prompt, parse, filter to DB products, persist, fallback | FR-2 | Recommendations saved; AI failure -> fallback |
| 10 | Global exception handling + correct HTTP codes | 2.1 | Clean JSON errors (400/401/404/409) |
| 11 | Automated tests + JaCoCo coverage (service >= 80%) | PPT | Passing suite + coverage report |
| 12 | React frontend (login, dashboard, register, pay, search, AI) | 2.2 | UI works end to end |
| 13 | Dockerfile + AWS deployment (ECS/RDS/ALB or similar) | PPT | Live URL |
| 14 | BMAD docs + PPT evidence (screenshots, test IDs) | PPT | Submission ready |

## Decisions to make before Step 1

- [x] Build tool: Maven
- [x] AI provider for Spring AI: Google Gemini (free key, stored in env var)
- [x] Auth style: JWT
- [x] Database tooling: Flyway migrations + Docker Compose Postgres (port 5433)
- [x] Duplicate rule: one policy per customer per product (matches `UQ(customer_id, product_id)`)
- [ ] Is BMAD required? Confirm with trainer; PPT only asks for BMAD-style docs (we use BMAD for planning docs only; code is written step by step)

## Setup checklist (before Step 1)

- [ ] Git installed (`winget install --id Git.Git -e`), name/email configured
- [ ] GitHub repo `healthdekho` created, first push done, cloned on second computer
- [ ] `npx bmad-method install` run in repo (Claude Code + BMM), committed
- [ ] Java 21, Maven, Node 20+ verified
- [ ] Postgres reachable (pgAdmin), DB `healthdekho` created
- [ ] Docker Desktop installed (needed before Step 13; Postgres container from Step 1)

## BMAD + PPT workflow

- BMAD is used only to produce planning docs (PRD, architecture/ADRs, epics and stories) -> PPT slides 16-17.
- Code is written step by step, each with a `stepN.readme.md`.
- After each step, save screenshots (success + failure) in `evidence/slideN-name/` for the PPT.
- Fill the PPT at the very end.

## Hard rules from the problem statement (do not forget)

- Java 21, Spring Boot 3.5.7, PostgreSQL, Spring AI, React + Axios
- Never expose JPA entities; use DTO records
- Money = `BigDecimal` / `NUMERIC(19,2)`; dates = `LocalDate`; audit = timestamptz
- Exactly 7 tables; claim tables (FE-1) are out of scope
- AI never replaces validations or calculations
- Secrets in environment variables, never committed

## Folder plan

```
healthdekho/
  backend/   (Spring Boot)
  frontend/  (React, later)
  docs/steps/step1.readme.md, step2.readme.md, ...
  ROADMAP.md
```
