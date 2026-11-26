# HealthDekho – Handoff Summary (read this first on any computer)

> **In one line:** where the project stands on 26 Nov 2026, how we work, what was decided, what is built, what is left, and how to finish and deploy it.
>
> **For Claude Code:** read this file together with `CLAUDE.md` and `ROADMAP.md` at the start of every session. The personal memory Claude had on the first computer does **not** travel with git; everything important from it is written here.

---

## 1. The project in one paragraph

**HealthDekho** is a customer-only insurance portal (capstone, problem statement in `README.md`). A customer logs in, sees their policies, gets AI product recommendations and registers new policies. Backend: Java 21 + Spring Boot 3.5.7 + PostgreSQL + Flyway + Spring Security/JWT + Spring AI (Gemini). Frontend: React 19 + Vite + React Router + Axios + Tailwind v4. **Only FR-1 to FR-3 are evaluated**; FR-4 (payment) and FR-5 (search) are built but optional.

### Status at a glance

| Area | Status |
|---|---|
| Steps 1–11: backend | ✅ done, 85 tests pass, service coverage 96.9% line / 83.4% branch |
| Step 12: React frontend | ✅ done, verified end to end in a browser |
| Guides (steps 1–12, concepts, backend flow, entity/SQL, new-PC setup) | ✅ done |
| Everything committed and pushed | ✅ commit `576a401` (plus the small changes listed in section 9) |
| Screenshots for the PPT | ⏳ only slide 11 (database) done; rest pending |
| Gemini API key | ⏳ not set yet; the rule-based fallback works without it |
| Step 13: Docker + AWS | ⏳ next |
| Step 14: BMAD docs + PPT | ⏳ last |

---

## 2. How we work (the user's preferences)

These are binding. Most are also in `CLAUDE.md`.

### Working style
- **One step at a time.** Before a step, Claude gives a **short plan** and **waits for "go"**. Never start the next step without "next"/"go". (Exception: Steps 4–11 were approved as one batch run.)
- **Short answers, token-efficient.** No long dumps. Ask before any token-heavy task. Don't draw images/diagrams unless allowed (ASCII inside docs is fine).
- **Never commit or push unless the user asks.**
- The user is **learning**. They know Java and Spring Data JPA; Spring Security, Flyway, JWT, Spring AI and React are new to them, so explain from zero.
- When the user says "do what's best", pick the safest option that **keeps their code** (e.g. they rejected commenting out FR-4/FR-5; they want to delete things personally).

### Step guides (`docs/steps/stepN.readme.md`)
Structure: one-line summary → **Part 1 Theory** (from zero, analogies, small diagrams) → **Part 2 What we built** (files, request flow controller → service → repository → DB) → **Part 3 Run and check** → **Part 4 Screenshots** → **Quick recap + next step**. Clean, not cluttered (the user rejected a cluttered first version).

### Code comments
Like a real developer: short "why" notes on business rules, gotchas and non-obvious choices. **Not** zero comments, **not** tutorial-style, **not** obvious ones. Teaching belongs in the step guides.

### Screenshots (PPT evidence)
- Tell the user **exactly when** to take a screenshot, **what must be visible**, and the **path** `evidence/slideNN-name/ID-description.png`.
- One screenshot at a time, wait for "done" (or use `docs/screenshot-checklist.md` as a batch list).
- Never show full JWT tokens in screenshots.
- Folders: slide03-summary, slide05-ui, slide06-validation, slide07-ai, slide08-registration, slide09-payment, slide10-search, slide11-database, slide12-tests, slide13-coverage, slide14-aws, slide15-aws-runtime.

### Rules that must never be broken
- **Secrets only in environment variables** (DB password, JWT secret, Gemini key). Never write them into any file, commit or note.
- **Flyway:** never edit a migration that already ran, **not even a comment** (the user learned this the hard way: it breaks the checksum). Add `V3__...sql` instead.
- Lombok on entities: `@Getter @Setter @NoArgsConstructor`, **never `@Data`**. Only `@ManyToOne(LAZY)`, no two-way lists.
- DTOs are records; never expose entities; `BigDecimal` for money; `LocalDate` for dates.
- Exactly **7 tables**. Claims (FE-1) out of scope.

---

## 3. Decisions already made

| Topic | Decision |
|---|---|
| Users | **Customer-only app.** No ADMIN role, no role columns |
| Data isolation | Customer id comes **only from the JWT** (`sub`). Policies are loaded by `(policyNumber + customerId)`. Another customer's policy → **404 "not found"** (not 403). Proven by tests PAY-09 and SRCH-02 |
| Duplicate rule | One policy per customer per product (`UNIQUE(customer_id, product_id)`) |
| FR-4 / FR-5 | **Optional** (not evaluated). Code kept and working, tagged `OPTIONAL: FR-4` / `OPTIONAL: FR-5` in backend and frontend. The user may delete it personally; Claude must not remove or comment it out |
| Payment model (FR-4) | Partial payments only within grace; after grace pay outstanding + 10% penalty in full; PAID → 409; over-payment → 400; FULLY_PAID when all installments paid; OVERDUE derived from today's date. Payment method is a label only (no gateway) |
| AI (FR-2) | Java decides eligible products; AI only ranks and explains; invented products are dropped; no key / timeout / error → rule-based fallback; every request saved with model + prompt version |
| Time | All "today" values from an injected `Clock` in Asia/Kolkata; tests pin 26 Nov 2026 |
| Auth | JWT HS256, 1 hour; `JWT_SECRET` optional in dev (random key per start) |
| Frontend | Plain JavaScript (no TypeScript), Tailwind v4, token in `sessionStorage`, port **5173 fixed** (CORS) |
| BMAD | Only for planning docs in `_bmad-output/` (Step 14). Engine folders are git-ignored; reinstall with `npx bmad-method install` |

---

## 4. What was done, in order (and what changed along the way)

| # | What | Notes / changes |
|---|---|---|
| 1 | Step 1: Spring Boot skeleton, dev/prod profiles | |
| 2 | Step 2: Flyway V1 (7 tables, constraints, trigger) + V2 seed data | The user once edited a comment in V1 → checksum problem → reverted. Lesson recorded |
| 3 | Step 3: entities, enums, repositories | Explained `@Data` vs `@Getter/@Setter` and why no two-way lists; wrote `docs/guides/entity-and-sql-explained.md` |
| 4 | Readmes rewritten | First versions were "too cluttered" → theory-first style |
| 5 | Code comments | Tutorial comments removed, then developer-style "why" comments added (the user wanted neither none nor obvious ones) |
| 6 | Evidence folders renamed | From `step1-setup` style to slide names (`slide11-database` …) |
| 7 | Decision: customer-only + data isolation | Added to `CLAUDE.md` |
| 8 | Steps 4–11 in one batch | Auth/JWT, dashboard, registration, payment, search, AI, error handling, 85 tests + JaCoCo; Postman collection (42 requests), API reference, demo SQL scripts, screenshot checklist |
| 9 | FR-4 / FR-5 made optional | First plan was commenting them out; **the user rejected that** → only `OPTIONAL` banners added, all code still runs |
| 10 | Step 12: React frontend | Tailwind chosen by the user; optional FR-4/5 pages in `frontend/src/optional/`; tested with a headless browser; fixed a logout bug (next user landed on the previous user's page) |
| 11 | Guides | `step12.readme.md`, `docs/guides/backend-flow-explained.md` (every endpoint's flow), `docs/guides/concepts-explained.md` (every concept + glossary) |
| 12 | Second-computer guide | `docs/run-on-another-computer.md` (install, DBs, env vars, run, smoke test, troubleshooting) |
| 13 | Commit + push by the user | `576a401 Frontend & Backend Completed` |
| 14 | This handoff + `.gitignore` fix | `frontend/.env.example` was wrongly ignored by `.env.*` → `!.env.example` added |

---

## 5. What we have (inventory)

```
Capstone Accenture/
├── README.md                     ← problem statement (the contract)
├── CLAUDE.md                     ← rules for Claude Code
├── ROADMAP.md                    ← 14 steps + progress line
├── HANDOFF.md                    ← this file
├── backend/                      ← Spring Boot app (mvnw, pom.xml, src/main, src/test)
├── frontend/                     ← React app (package.json, src/, src/optional/)
├── docs/
│   ├── steps/step1…step12.readme.md
│   ├── guides/                   ← entity-and-sql, backend-flow, concepts
│   ├── run-on-another-computer.md
│   ├── api-reference.md
│   ├── screenshot-checklist.md
│   ├── postman/HealthDekho.postman_collection.json
│   └── demo/                     ← reset-dev-db.sql, demo-overdue-policy.sql
├── evidence/slide11-database/    ← 6 screenshots done (DB-01 … DB-05)
├── FDE_Capstone_Solution_Submission_Template.pptx   ← PPT template (fill at the end)
└── _bmad-output/                 ← empty for now (Step 14)
```

| Fact | Value |
|---|---|
| Ports | backend 8080, frontend 5173, Postgres 5432 |
| Databases | `healthdekho` (app), `healthdekho_test` (tests); both start empty, Flyway fills them |
| Env vars | `DB_USERNAME`, `DB_PASSWORD` required; `JWT_SECRET`, `GEMINI_API_KEY`, `GEMINI_MODEL` optional |
| Demo logins (password `Password@123`) | Aarav 1001 (has HDP-2026-0001 Term), Meera 1002 (HDP-2026-0002 Health), Rohan 1003 (no policies) |
| Products | 201 HD-TERM-001, 202 HD-HEALTH-001, 203 HD-SAV-001 |
| Reset sample data | run `docs/demo/reset-dev-db.sql` in pgAdmin, then restart the backend |

---

## 6. What is left

| # | Task | Details |
|---|---|---|
| 1 | **Screenshots** | Follow `docs/screenshot-checklist.md` top to bottom: UI-01…12 (React), AUTH/DASH/PROD/ERR (Postman), REG, AI, TEST-01…04 (tests + JaCoCo). Optional slides 9–10 only if you keep FR-4/5. Slide 11 is already done |
| 2 | **Gemini key** (optional but nice) | Get a free key at Google AI Studio → env var `GEMINI_API_KEY` → restart backend → screenshot AI-01 (`source: AI`) |
| 3 | **Decide FR-4/FR-5** | Keep (extra credit, nothing to do) or delete (see section 8) **before** screenshots and deployment |
| 4 | **Step 13: Docker + AWS** | See section 8; screenshots for slides 14–15 |
| 5 | **Step 14: BMAD docs + PPT** | PRD, architecture, epics/stories in `_bmad-output/` (slides 16–17), then fill the PPT with evidence IDs |
| 6 | Open question | Confirm with the trainer whether BMAD is required (ROADMAP checkbox) |

---

## 7. What to do next (recommended order)

1. On the new computer: follow `docs/run-on-another-computer.md` until the smoke test passes.
2. (Optional) Add `GEMINI_API_KEY`.
3. Decide on FR-4/FR-5 (keep or delete).
4. Take the screenshots (`docs/screenshot-checklist.md`).
5. Tell Claude: **"Start Step 13"** → plan → "go" → Docker + AWS.
6. Tell Claude: **"Start Step 14"** → BMAD docs + PPT.
7. After evaluation: **delete the AWS resources** (they cost money).

### How Claude should guide the user next
1. Read `CLAUDE.md`, `ROADMAP.md`, this file, and the latest `docs/steps/stepN.readme.md`.
2. Tell the user in 2–3 lines: last completed step + what's next. Wait for "go".
3. Give a short plan for the step, wait for confirmation, then build.
4. Cue screenshots as you go (what, when, path).
5. Finish with `docs/steps/stepN.readme.md` in the agreed style and update the progress line in `ROADMAP.md`.
6. Don't commit; remind the user to commit + push so the other computer stays in sync.

---

## 8. Deleting and deploying the right way

### 8.1 What gets deployed and what doesn't

Deployment ships **two things only**: the backend (as a Docker image) and the frontend's built files (`frontend/dist/`). Nothing else leaves your repo.

| Item | Deployed? | Keep in git? | Why |
|---|---|---|---|
| `backend/` code | ✅ (as a Docker image) | ✅ | the app |
| `frontend/` code | ✅ (only the `npm run build` output) | ✅ | the UI |
| `README.md`, `CLAUDE.md`, `ROADMAP.md`, `HANDOFF.md` | ❌ | ✅ | project/AI instructions, needed for the next session |
| `docs/` (guides, Postman, demo SQL) | ❌ | ✅ | learning + submission material |
| `evidence/`, the `.pptx` | ❌ | ✅ | for the PPT |
| `_bmad/`, `.claude/`, `.agents/`, `.github/` | ❌ | ❌ (already ignored) | tools, reinstallable |
| `node_modules/`, `dist/`, `target/` | ❌ | ❌ (already ignored) | rebuilt every time |
| Any `.env` / real secrets | ❌ | ❌ **never** | go into AWS Secrets Manager / env vars instead |

You don't have to delete docs to deploy. A `.dockerignore` (written in Step 13) keeps them out of the image.

Things that are already safe in production:
- **Demo-account panel** on the login page: only shown in `npm run dev`, never in a production build.
- **Seed data:** Flyway V2 also runs in production, so the demo customers exist there. That's wanted for a capstone demo. For a real product you would move seed data to a dev-only location.
- **`docs/demo/*.sql`**: never run them against production (they drop the schema).

### 8.2 If you decide to delete FR-4 / FR-5

Search the whole project for `OPTIONAL: FR-4` (or `OPTIONAL: FR-5`) and delete what each banner says. In short:

| Layer | FR-4 (payment) | FR-5 (search) |
|---|---|---|
| Backend services | `PaymentService`, `PremiumRules` | `PolicySearchService`, `SearchCriteria` |
| Backend DTOs | `dto/payment/*`, `PolicyDetailResponse`, `InstallmentResponse` | `dto/search/*`, `dto/common/PageResponse`, `repository/PolicyPaymentTotals` |
| Shared files (delete only the marked parts) | `PolicyController` (4 endpoints + field), `PolicyService` (everything below `getDashboard`), repository queries tagged FR-4 | `PolicyController` (search endpoint + field), repository queries tagged FR-5 |
| Tests | `PaymentIntegrationTest`, `PremiumRulesTest` | `SearchIntegrationTest`, `SearchCriteriaTest` |
| Frontend | `src/optional/fr4-payment/`, routes in `App.jsx`, nav item, "Pay" column on the dashboard, "View payment schedule" button | `src/optional/fr5-search/`, route, nav item |
| Docs | Postman folders 4/5, checklist slides 9/10 | same |

Then check: `.\mvnw verify` (backend builds, tests pass, coverage ≥ 80%) and `npm run build` (frontend builds).

⚠️ The `findOwnedPolicy` query and the data-isolation tests PAY-09/SRCH-02 belong to FR-4/5. If you delete both features, the isolation rule is still proven on the dashboard and the AI items (`findOwnedItem`); mention that on the slide.

### 8.3 Recommended AWS setup (Step 13; to be confirmed with the user before building)

```
                         ┌──────────── CloudFront (one HTTPS URL, free certificate) ────────────┐
 Browser ── https ──►    │  /*            → S3 bucket: frontend/dist (static files)             │
                         │  /api/*, /actuator/health → Load Balancer → backend container         │
                         └───────────────────────────────────────────────────────────────────────┘
                                                                  │
                                              ECS Fargate (Docker image from ECR)
                                                                  │
                                              RDS PostgreSQL (private, not public)
                         Secrets Manager / SSM: DB_PASSWORD, JWT_SECRET, GEMINI_API_KEY
```

Why this shape:
- **One URL for both:** the frontend and the API share the same origin, so there are no CORS problems and you get HTTPS for free through CloudFront.
- It matches the PPT (slides 14–15: architecture + runtime).
- Each piece is the standard AWS service for its job.

**Simpler alternative** if time is short: one EC2 instance running Docker Compose (backend + nginx serving the frontend) + RDS. Fewer pieces, still a live URL.

**Production settings:**

| Where | Variable | Value |
|---|---|---|
| backend | `SPRING_PROFILES_ACTIVE` | `prod` |
| backend | `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | the RDS endpoint and its user |
| backend | `JWT_SECRET` | random, 32+ characters (**required** in prod) |
| backend | `GEMINI_API_KEY` | optional |
| backend | `CORS_ALLOWED_ORIGINS` | the CloudFront URL (only needed if the frontend and API are on different origins) |
| frontend build | `VITE_API_BASE_URL` | empty (same origin through CloudFront) or the API URL; confirm in Step 13 |

**Step 13 will add:**
- `backend/Dockerfile` (multi-stage: Maven build → small Java 21 runtime image)
- `.dockerignore`
- optional `docker-compose.yml` for running everything locally
- the AWS steps, and screenshots for slides 14–15

**Cost safety:**
- Use the free tier sizes.
- Set an AWS budget alert.
- Tear everything down after the evaluation.

---

## 9. Uncommitted changes made after `576a401`

- `HANDOFF.md` (this file)
- `.gitignore`: `!.env.example` so `frontend/.env.example` is shared
- `CLAUDE.md`: session start now also reads `HANDOFF.md`
- `ROADMAP.md`: points to `HANDOFF.md`

→ **Commit and push these from this computer before switching**, e.g. message `Add handoff summary`.

---

## 10. Quick start on the next computer

```powershell
git pull
# Terminal 1
cd backend;  .\mvnw spring-boot:run        # wait for "Started HealthDekhoApplication"
# Terminal 2
cd frontend; npm install; npm run dev      # http://localhost:5173
# Terminal 3 (optional)
cd backend;  .\mvnw verify                 # 85 tests, BUILD SUCCESS
```

First time on a computer? Do `docs/run-on-another-computer.md` sections 1–4 first (installs, two databases, env vars).
