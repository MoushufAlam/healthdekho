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
- **Never commit or push** unless the user asks.
- **Screenshots:** when something is worth evidencing, tell the user exactly when to take it, what must be visible,
  and the path: `evidence/slideNN-name/ID-description.png`. Give one screenshot at a time and wait for "done".
  Folder map: slide03-summary, slide05-ui, slide06-validation, slide07-ai, slide08-registration, slide09-payment,
  slide10-search, slide11-database, slide12-tests, slide13-coverage, slide14-aws, slide15-aws-runtime.
- **Step docs style:** the user knows Java + Spring Data JPA only (not Flyway, Spring Security, JWT, Spring AI).
  Structure each `stepN.readme.md`: one-line summary -> Part 1 Theory (from zero, with analogies and small diagrams) ->
  Part 2 What we built -> Part 3 Run and check -> Part 4 Screenshots (if any) -> Quick recap + next step.
  Keep it clean, not cluttered.
- **Code comments:** write them like a real developer: short "why" notes on business rules, gotchas and
  non-obvious choices (e.g. `// may go up to scheduledAmount + 10% when the late-payment penalty applies`).
  Not zero comments, not tutorial-style explanations, not obvious ones. Teaching goes in the step readme.
- **Flyway:** never edit a migration that already ran (even comments); add a new `V{n}__...sql` instead.
- Lombok: use `@Getter @Setter @NoArgsConstructor` on entities, never `@Data`. Only `@ManyToOne(LAZY)` relations
  (no bidirectional lists) unless a step truly needs one.

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
