# Screenshot Checklist (Steps 4–12)

Everything here can be taken at any time. Work top to bottom: the order matters for a few rows (marked ⚠️).

## Before you start

1. **Fresh data:** pgAdmin → database `healthdekho` → Query Tool → run `docs/demo/reset-dev-db.sql`.
2. **Start the app:** `cd backend` → `.\mvnw spring-boot:run` (Flyway reloads the sample data).
3. **Postman:** *Import* → `docs/postman/HealthDekho.postman_collection.json`.
4. **Log in:** run the 3 login requests in folder **1. Auth** (they save the tokens).

**What a good screenshot shows** (PPT slide 2 rules):
- In Postman: the **method + URL**, the **request body** (if any), and the **response status + body**.
- Put the **evidence ID** (first column) in the file name, and write it on the slide with the requirement.
- **Hide secrets:** never show the full `accessToken`. Crop it, or collapse the response so only `"tokenType"`, `"expiresAt"` and `"customer"` are visible.

---

## Slide 5 – React UI (Step 12) → `evidence/slide05-ui/` (and the slide named per row)

**Extra setup:** after step 2 above, start the frontend in a second terminal: `cd frontend` → `npm run dev` → open `http://localhost:5173`. Browser window maximised; keep the address bar visible so the URL is in the shot.

| ✔ | ID | What to do | Must be visible | File |
|---|---|---|---|---|
| ☐ | UI-01 | Open `/login` | Brand panel + login form | `slide05-ui/UI-01-login.png` |
| ☐ | UI-02 | Aarav's email + wrong password → **Log in** | Red "Login failed / Invalid email or password" | `slide06-validation/UI-02-login-401.png` |
| ☐ | UI-03 | Click **Log in** with both fields empty | Red messages under both fields | `slide06-validation/UI-03-login-required-fields.png` |
| ☐ | UI-04 | Log in as **Rohan** | "No policies yet" message + Available products table with **Register** | `slide05-ui/UI-04-dashboard-empty.png` |
| ☐ | UI-05 | Log out, log in as **Aarav** | My policies table (all 10 columns) + 2 available products | `slide05-ui/UI-05-dashboard-policies.png` |
| ☐ | UI-06 | Aarav → **Get AI recommendations** → type *Savings*, goals "Save for my child's education" → **Get recommendations** | Ranked table with reason, suitability, Register; source badge | `slide07-ai/UI-06-ai-recommendations.png` |
| ☐ | UI-07 | Click **Register** on HD-SAV-001 | Pre-filled customer + product, maturity date, **Total premium payable ₹3,00,000.00**, purple "From your AI recommendation" badge | `slide08-registration/UI-07-registration-form.png` |
| ☐ | UI-08 | Change the start date to yesterday | Field error + yellow "Policy start date cannot be in the past", button disabled | `slide06-validation/UI-08-past-start-date.png` |
| ☐ | UI-09 ⚠️ | Set today's date → **Confirm registration** (once) | Success screen with policy number `HDP-2026-0003` | `slide08-registration/UI-09-registered.png` |
| ☐ | UI-10 | **Go to dashboard** | New policy in My policies; HD-SAV-001 gone from Available | `slide08-registration/UI-10-dashboard-after.png` |
| ☐ | UI-11 | *(optional FR-4)* **Pay Premium** → HDP-2026-0001 | Totals + schedule with checkboxes | `slide09-payment/UI-11-payment-page.png` |
| ☐ | UI-12 | *(optional FR-5)* **Search** → name "aarav" | Results table + pagination | `slide10-search/UI-12-search.png` |

> ⚠️ UI-09 registers HD-SAV-001 for Aarav, which is the same product as REG-03 in Postman. Do **either** UI-09 **or** REG-03, or reset the database in between.

## Slide 5 – API happy path (Postman) → `evidence/slide05-ui/`

| ✔ | ID | Postman request | Must be visible | File |
|---|---|---|---|---|
| ☐ | AUTH-01 | 1. Auth → *AUTH-01 Login Aarav* | 200, `tokenType: Bearer`, `customer` block (token cropped) | `AUTH-01-login-success.png` |
| ☐ | DASH-01 | 2 → *DASH-01 Dashboard (Aarav)* | 200, policy table fields for HDP-2026-0001 | `DASH-01-dashboard.png` |
| ☐ | DASH-02 | 2 → *DASH-02 Dashboard (Rohan)* | `policies: []` + empty message | `DASH-02-dashboard-empty.png` |
| ☐ | PROD-01 | 2 → *PROD-01 Available products* | only HD-HEALTH-001 and HD-SAV-001 (Secure Term excluded) | `PROD-01-available-products.png` |

## Slide 6 – Validation evidence → `evidence/slide06-validation/`

| ✔ | ID | Postman request | Must be visible | File |
|---|---|---|---|---|
| ☐ | AUTH-02 | 1 → *AUTH-02 Wrong password* | 401 "Invalid email or password" | `AUTH-02-wrong-password.png` |
| ☐ | AUTH-04 | 1 → *AUTH-04 Invalid body* | 400 + `fieldErrors` for email and password | `AUTH-04-field-validation.png` |
| ☐ | AUTH-06 | 1 → *AUTH-06 Dashboard without token* | 401 "Authentication required…" | `AUTH-06-no-token.png` |
| ☐ | ERR-01 | 7 → *ERR-01 Invalid enum value* | 400 "Invalid value 'CAR' for field 'preferredPolicyType'" | `ERR-01-invalid-enum.png` |
| ☐ | ERR-02 | 7 → *ERR-02 Malformed JSON* | 400 "Request body is missing or is not valid JSON" | `ERR-02-malformed-json.png` |

## Slide 8 – Policy registration → `evidence/slide08-registration/`

| ✔ | ID | Postman request | Must be visible | File |
|---|---|---|---|---|
| ☐ | REG-01 | 3 → *REG-01 Preview Smart Savings* | customer pre-fill, `maturityDate`, `numberOfInstallments: 40`, `totalPremiumPayable: 300000.00`, `eligible: true` | `REG-01-preview.png` |
| ☐ | REG-03 ⚠️ | 3 → *REG-03 Register Smart Savings* (**run only once**) | 201, `policyNumber`, "registered successfully" | `REG-03-registered.png` |
| ☐ | REG-04 | 3 → *REG-04 Register same product again* | 409 "You already have a policy…" | `REG-04-duplicate-409.png` |
| ☐ | REG-05 | 3 → *REG-05 Start date in the past* | 400 "Policy start date cannot be in the past" | `REG-05-past-start-date.png` |
| ☐ | DB-REG | pgAdmin query below | 40 rows, due dates 3 months apart, grace +1 month, 7500.00 each | `DB-REG-installment-schedule.png` |

```sql
-- DB-REG
SELECT p.policy_number, i.installment_number, i.due_date, i.grace_period_end_date, i.scheduled_amount, i.payment_status
FROM premium_installment i JOIN customer_policy p ON p.policy_id = i.policy_id
WHERE p.policy_number = 'HDP-2026-0003' ORDER BY i.installment_number;
```

## Slide 9 – Premium payment → `evidence/slide09-payment/`

| ✔ | ID | Postman request | Must be visible | File |
|---|---|---|---|---|
| ☐ | PAY-01 | 4 → *PAY-01 Policy details* | totalPremiumPayable / Paid / Remaining | `PAY-01-policy-lookup.png` |
| ☐ | PAY-02 | 4 → *PAY-02 Installment schedule* | first 3 rows: PAID (selectable false), PARTIALLY_PAID, PENDING | `PAY-02-schedule.png` |
| ☐ | PAY-03 ⚠️ | 4 → *PAY-03 Partial payment* (once) | 201, `PARTIALLY_PAID`, `TXN-HD-…`, new remaining | `PAY-03-partial-payment.png` |
| ☐ | PAY-05 | 4 → *PAY-05 Overpayment* | 400 "exceeds the outstanding premium" | `PAY-05-overpayment.png` |
| ☐ | PAY-06 | 4 → *PAY-06 Already paid installment* | 409 "Installment #1 is already paid" | `PAY-06-duplicate-payment.png` |
| ☐ | PAY-09 | 4 → *PAY-09 ISOLATION Meera views Aarav's policy* | 404 "Policy HDP-2026-0001 not found" | `PAY-09-isolation-404.png` |

**Late-payment penalty** (pgAdmin: run `docs/demo/demo-overdue-policy.sql` once, app running):

| ✔ | ID | Postman request | Must be visible | File |
|---|---|---|---|---|
| ☐ | PAY-08 | 4 → *PAY-08 Overdue schedule (Rohan)* | #1, #2 `OVERDUE`, `penaltyIfPaidToday: 750.00`, `amountDueToday: 8250.00` | `PAY-08-overdue-schedule.png` |
| ☐ | PAY-07a | 4 → *PAY-07a Late payment without penalty* | 400 "Pay the full amount due 8250.00 (… + 10% penalty 750.00)" | `PAY-07a-penalty-required.png` |
| ☐ | PAY-07b ⚠️ | 4 → *PAY-07b Late payment with 10% penalty* (once) | 201, `penaltyAmount: 750.00`, `PAID` | `PAY-07b-penalty-paid.png` |

## Slide 10 – Policy search → `evidence/slide10-search/`

| ✔ | ID | Postman request | Must be visible | File |
|---|---|---|---|---|
| ☐ | SRCH-01 | 5 → *SRCH-01 By policy number* | result row with all FR-5 columns | `SRCH-01-by-number.png` |
| ☐ | SRCH-04 | 5 → *SRCH-04 By customer name* | results for "aarav" | `SRCH-04-by-name.png` |
| ☐ | SRCH-05 | 5 → *SRCH-05 By name, paginated* (after REG-03) | `size: 1`, `totalElements: 2`, `totalPages: 2` | `SRCH-05-pagination.png` |
| ☐ | SRCH-04b | 5 → *SRCH-04b No match* | empty `content` + "No policies match…" | `SRCH-04b-no-results.png` |
| ☐ | SRCH-02 | 5 → *SRCH-02 ISOLATION* | 404 for HDP-2026-0002 as Aarav | `SRCH-02-isolation-404.png` |
| ☐ | SRCH-07 | 5 → *SRCH-07 Date of birth alone* | 400 explanation | `SRCH-07-invalid-criteria.png` |

## Slide 7 – AI recommendations → `evidence/slide07-ai/`

| ✔ | ID | Postman request / query | Must be visible | File |
|---|---|---|---|---|
| ☐ | AI-02 | 6 → *Recommend for Rohan* (no key) | `source: FALLBACK`, message about the AI being unavailable, `calculatedAge: 26`, ranked list with reason + suitability | `AI-02-fallback.png` |
| ☐ | AI-DB | pgAdmin query from `docs/steps/step9.readme.md` (Part 3) | request row (age, model name, prompt version) + its items | `AI-DB-persisted.png` |
| ☐ | REG-08 | 6 → *REG-08 Register the top recommendation* | 201; then re-run AI-DB query showing `selected_for_registration = true` | `REG-08-register-from-ai.png` |
| ☐ | AI-01 | *(after you add GEMINI_API_KEY and restart)* same request | `source: AI`, `modelName: gemini-…`, AI-written reasons | `AI-01-gemini-response.png` |

## Slide 12 – Test execution → `evidence/slide12-tests/`

| ✔ | ID | What to run | Must be visible | File |
|---|---|---|---|---|
| ☐ | TEST-01 | `.\mvnw test` in the terminal | `Tests run: 85, Failures: 0` + `BUILD SUCCESS` | `TEST-01-suite-passing.png` |
| ☐ | TEST-02 | VS Code Testing panel (flask icon) → expand **PAY** and **SRCH** | green ticks next to the PAY-09 and SRCH-02 ISOLATION test names | `TEST-02-isolation-tests.png` |
| ☐ | TEST-03 | open `backend/src/test/java/.../PaymentIntegrationTest.java` at `latePayment_penalty` | the assertions (1375.00, penalty 125.00) | `TEST-03-penalty-assertions.png` |

## Slide 13 – Coverage → `evidence/slide13-coverage/`

| ✔ | ID | What to run | Must be visible | File |
|---|---|---|---|---|
| ☐ | TEST-04 | `.\mvnw verify`, then open `backend/target/site/jacoco/index.html` | package table with line/branch % (service ≈ 97% / 83%) | `TEST-04-jacoco-overview.png` |
| ☐ | TEST-04b | same report → click `com.healthdekho.service` | per-class coverage | `TEST-04b-jacoco-service.png` |

Coverage numbers and known gaps for the slide text: `docs/steps/step11.readme.md` → "Coverage".

---

## When you're done

Run `docs/demo/reset-dev-db.sql` and restart the app to get clean data back.
