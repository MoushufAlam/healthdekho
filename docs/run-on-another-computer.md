# Run HealthDekho on Another Computer

> **In one line:** a checklist to set up a fresh computer, start the backend and the frontend, and confirm everything works. Takes about 15 minutes the first time.

---

## 1. Install once (skip what you already have)

| Tool | Version | Check with | Get it |
|---|---|---|---|
| Git | any | `git --version` | `winget install --id Git.Git -e` |
| Java (JDK) | **21** | `java -version` → shows `21` | `winget install --id EclipseAdoptium.Temurin.21.JDK -e` |
| Node.js | **20.19+ or 22.12+** (Vite 8 needs this) | `node -v` | `winget install --id OpenJS.NodeJS.LTS -e` |
| PostgreSQL | 16+ (we use 18) | pgAdmin opens | postgresql.org/download |

Maven is **not** needed: the project includes the Maven Wrapper (`mvnw`).

Close and reopen the terminal after installing, so the new commands are found.

---

## 2. Get the code

```powershell
git clone <your-repo-url>          # first time only
cd "Capstone Accenture"
git pull                           # every time: get the latest changes
```

> ⚠️ Only **committed and pushed** work reaches this computer. If something is missing, commit and push on the other PC first.

---

## 3. Create the two databases (pgAdmin, once)

pgAdmin → right-click **Databases** → **Create → Database…**

| Database | Used by | Leave it empty |
|---|---|---|
| `healthdekho` | the running app | ✅ Flyway creates the tables and sample data on the first start |
| `healthdekho_test` | the automated tests | ✅ Flyway fills it on the first test run |

Port must be **5432** (the default).

---

## 4. Environment variables (once)

**Windows key → "Edit environment variables for your account" → New…**

| Name | Value | Required? |
|---|---|---|
| `DB_USERNAME` | your Postgres user (usually `postgres`) | ✅ yes |
| `DB_PASSWORD` | your Postgres password **on this computer** | ✅ yes |
| `JWT_SECRET` | any random text, **32+ characters** | optional (without it you're logged out after every backend restart) |
| `GEMINI_API_KEY` | your Google AI Studio key | optional (without it the AI page shows rule-based suggestions) |

Then **close and reopen** VS Code / the terminal (open terminals don't see new variables).

Check in a new PowerShell:

```powershell
echo $env:DB_USERNAME     # must print the user
```

> Never put these values in a file in the project; they would end up on GitHub.

---

## 5. Start and check the backend

```powershell
cd backend
.\mvnw spring-boot:run
```

First start takes a few minutes (downloads dependencies). Wait for:

```
Started HealthDekhoApplication in … seconds
```

**Check 1:** open http://localhost:8080/actuator/health → `{"status":"UP"}` ✅

**Check 2 (pgAdmin):** database `healthdekho` → Query Tool:

```sql
SELECT (SELECT count(*) FROM customer)            AS customers,      -- 3
       (SELECT count(*) FROM insurance_product)   AS products,       -- 3
       (SELECT count(*) FROM customer_policy)     AS policies,       -- 2
       (SELECT count(*) FROM premium_installment) AS installments;   -- 241
```

Leave this terminal running.

---

## 6. Run the automated tests (optional but recommended)

In a **second** terminal (the app can keep running):

```powershell
cd backend
.\mvnw verify
```

Expected ending:

```
Tests run: 85, Failures: 0, Errors: 0, Skipped: 0
All coverage checks have been met.
BUILD SUCCESS
```

---

## 7. Start and check the frontend

In a new terminal:

```powershell
cd frontend
npm install        # first time only, and after package.json changes (~1 min)
npm run dev
```

Expected:

```
VITE v8.x  ready in … ms
➜  Local:   http://localhost:5173/
```

Open **http://localhost:5173**.

---

## 8. Smoke test (2 minutes)

| # | Do | Expect |
|---|---|---|
| 1 | Open http://localhost:5173 | Login page (teal panel on the left) |
| 2 | Aarav's email + password `wrong` → **Log in** | Red "Invalid email or password" |
| 3 | Click the **Rohan Verma** demo button → **Log in** | Dashboard: "No policies yet" + 3 products |
| 4 | **Get AI recommendations** → goals "Health cover for my parents" → **Get recommendations** | Ranked table (source badge: AI or Rule-based) |
| 5 | **Register** on a row → **Confirm registration** | "Policy registered!" with `HDP-2026-0003` |
| 6 | **Go to dashboard** | The new policy is in "My policies" |
| 7 | **Logout** | Back on the login page |

All 7 pass → this computer is ready. ✅

**Clean up afterwards** (to get the original sample data back): pgAdmin → `healthdekho` → Query Tool → run `docs/demo/reset-dev-db.sql`, then restart the backend.

---

## 9. If something fails

| Symptom | Cause | Fix |
|---|---|---|
| `java -version` shows 17 or "not recognized" | Wrong/no JDK on PATH | Install JDK 21, reopen terminal; set `JAVA_HOME` to the JDK 21 folder |
| Backend: `password authentication failed for user` | `DB_PASSWORD` / `DB_USERNAME` wrong or not loaded | Fix the variables, **reopen** the terminal |
| Backend: `database "healthdekho" does not exist` | Step 3 skipped | Create the database |
| Backend: `Connection refused … 5432` | PostgreSQL not running | Windows Services → start `postgresql-x64-…` |
| Backend: `Port 8080 was already in use` | Another backend is still running | Close it (or `Get-NetTCPConnection -LocalPort 8080` → stop that process) |
| Backend: `Validate failed: Migrations have failed validation` / checksum mismatch | A migration file was edited | `git checkout -- backend/src/main/resources/db/migration`, then run `docs/demo/reset-dev-db.sql` |
| Tests: `database "healthdekho_test" does not exist` | Test DB missing | Create it (step 3) |
| `npm` says Node version unsupported | Node too old | Install Node 22 LTS |
| `npm run dev`: `Port 5173 is in use` | Another dev server is open | Close it (the port is fixed on purpose, CORS expects 5173) |
| Page says "Cannot reach the server…" | Backend not running | Step 5 |
| Browser console: *blocked by CORS policy* | Frontend not on port 5173 | Use http://localhost:5173 exactly |
| Logged out after restarting the backend | No `JWT_SECRET` | Log in again, or set `JWT_SECRET` |
| AI page says "Rule-based suggestions" | No `GEMINI_API_KEY` | Expected; add the key + restart the backend for real AI answers |

---

## Daily routine (after the first setup)

```powershell
git pull
# terminal 1
cd backend;  .\mvnw spring-boot:run
# terminal 2
cd frontend; npm install; npm run dev      # npm install is quick when nothing changed
```

When done: commit and push, so the other computer gets your work.
