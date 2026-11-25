# HealthDekho – Instructions for Claude Code

## At the start of EVERY session (mandatory)
1. Read `ROADMAP.md`.
2. Read the **latest** `docs/steps/stepN.readme.md` (highest N) to see where we left off.
3. Tell the user in 2-3 lines: last completed step, what is next. Then wait for "go".
If no step files exist yet, we are at Step 1.

## Working agreement
- The problem statement is `README.md`. Follow it strictly. PPT is only an evidence template.
- **One step at a time.** Never start the next step until the user says "next"/"go".
- Before each step: give a short plan and wait for confirmation.
- After each step: write `docs/steps/stepN.readme.md` explaining what was built, why, the request flow
  (controller -> service -> repository -> DB), key Java 21/Spring concepts, how to run/test, and which FR it covers.
- Keep explanations short and beginner-friendly. User is learning; no giant dumps of code without explanation.
- Be token-efficient. No long answers.
- Do not ask to draw images/diagrams unless user permits; ask before any token-heavy task.

## Decisions already made
- Java 21, Spring Boot 3.5.7, Maven, PostgreSQL, Flyway, Spring Security + JWT, Spring AI with Google Gemini.
- Records for DTOs; never expose entities; BigDecimal for money; LocalDate for dates.
- Exactly 7 tables (see README section 5.2). Claims (FE-1) out of scope.
- AI only suggests products from DB; all validations/calculations are deterministic Java in the service layer.
- Secrets (DB password, Gemini key) only via environment variables, never committed.
- Local Postgres for now (port 5432, db `healthdekho`, confirm with user); Docker later (Step 13 / optional compose).
- BMAD is installed (engine folders are git-ignored; reinstall with `npx bmad-method install`). Use it only for planning docs
  (PRD, architecture, stories) in `_bmad-output/`. Code is written step by step with the user.
- Evidence screenshots for the PPT go in `evidence/slideN-name/`.

## Environment
- Windows 11, PowerShell. Two computers synced via GitHub (pull before work, commit + push after).
