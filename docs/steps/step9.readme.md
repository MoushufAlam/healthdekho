# Step 9 – AI Recommendations with Spring AI (Gemini)

> **In one line:** the customer describes their goals; **Java** decides which products are allowed, **Gemini** ranks them and explains why, and **Java** checks the answer before saving it. If the AI is down, a rule-based fallback answers instead.

**Covers:** FR-2 (AI-based policy recommendations, Java 21 expectations)

---

## Part 1 – The theory

### 1.1 What is an LLM, and why can't we trust it blindly?

An LLM (Large Language Model, e.g. Google **Gemini**) takes text in and gives text out. It's great at **explaining** ("this plan fits because…"), but it can:
- **invent** things (a product code that doesn't exist),
- **ignore rules** (suggest a product the customer already has),
- be **slow** or **unavailable**.

So the README's golden rule is: **AI must not replace business validations or calculations.**

### 1.2 Our design: Java decides, AI explains, Java checks

```
                ┌──────────────── JAVA (deterministic) ────────────────┐
customer input →│ 1. age from DOB (Java Time API)                      │
                │ 2. candidates = active products                      │
                │       − products the customer already holds          │
                │       − products outside the entry age               │
                └──────────────────────────┬───────────────────────────┘
                                           ▼
                ┌──────────────── AI (Gemini via Spring AI) ───────────┐
                │ 3. rank the candidates, give a reason + suitability  │
                └──────────────────────────┬───────────────────────────┘
                                           ▼
                ┌──────────────── JAVA again ───────────────────────────┐
                │ 4. keep only codes that are in the candidate list     │
                │    (drop invented / owned / duplicate ones)           │
                │ 5. save request + items to the DB (audit)             │
                └──────────────────────────────────────────────────────┘
        AI fails, times out, or returns nothing usable  →  rule-based fallback (step 3 done in Java)
```

The AI can **never** cause an ineligible or non-existent product to be recommended: step 4 filters its answer against the list Java built in step 2.

### 1.3 What Spring AI gives us

**Spring AI** is a Spring library that talks to many AI providers with the same code. We use the **Google GenAI** starter (`spring-ai-starter-model-google-genai`), configured in `application.yml`:

```yaml
spring.ai.google.genai.api-key: ${GEMINI_API_KEY:not-configured}
spring.ai.google.genai.chat.options.model: gemini-2.5-flash
```

The key piece is **`ChatClient`**, a fluent API:

```java
AiSuggestionList answer = chatClient.prompt()
        .system(SYSTEM_PROMPT)            // the rules for the model
        .user(customerAndProducts)        // this customer's data + candidate list
        .call()
        .entity(AiSuggestionList.class);  // ← structured output
```

**Structured output:** `.entity(SomeRecord.class)` makes Spring AI add JSON-format instructions to the prompt and convert the model's JSON answer into our Java record. No manual parsing.

### 1.4 The prompt

**System prompt** (the rules, `RecommendationPrompt.SYSTEM`):
> Recommend products ONLY from the CANDIDATE PRODUCTS list, using their exact productCode. Never invent products… Rank the best fit first… reason… suitability HIGH/MEDIUM/LOW. Return at most 3 products.

**User prompt** (the data):
```
CUSTOMER
- age: 26
- insurance goals: Health coverage for self and parents
- preferred term: 1 year
- preferred policy type: HEALTH

CANDIDATE PRODUCTS
- productCode=HD-HEALTH-001 | name=HealthDekho Family Health | type=HEALTH | termMonths=12 | ...
- productCode=HD-TERM-001   | ...
```

**Prompt version** (`app.ai.prompt-version: rec-v1`) is saved with every request. Change the prompt → bump the version, so you can always tell which prompt produced which recommendation.

### 1.5 Timeouts on a virtual thread

An AI call can hang. We run it with `CompletableFuture` on a **virtual thread** (Java 21: very cheap threads) and stop waiting after `app.ai.timeout` (20 s):

```java
CompletableFuture<AiSuggestionList> call = CompletableFuture.supplyAsync(() -> chatClient...call()..., aiExecutor);
call.get(20, SECONDS);   // TimeoutException → fallback
```

Spring AI's own retry is reduced to 2 attempts, so the fallback kicks in quickly.

### 1.6 No transaction around the AI call

`RecommendationService.recommend()` is deliberately **not** `@Transactional`. A database transaction holds a connection; holding it for a 10-second AI call would starve other requests. Only the final **save** runs in a short transaction (`TransactionTemplate`).

### 1.7 Pattern matching with `instanceof` (Java 16+)

Used to turn an exception into a friendly reason:

```java
if (ex instanceof AiUnavailableException unavailable && unavailable.getCause() instanceof TimeoutException) {
    return "the AI took too long to answer";
}
```

The check and the cast happen in one step (`unavailable` is already the right type).

---

## Part 2 – What we built

```
ai/RecommendationAiClient.java      ← our interface: suggest(input) → list of suggestions
ai/GeminiRecommendationClient.java  ← Spring AI ChatClient + timeout + "no key" check
ai/RecommendationPrompt.java        ← system + user prompt text
ai/AiSuggestion.java                ← structured-output record (productCode, rank, reason, suitability)
ai/AiRecommendationInput.java, AiUnavailableException.java
service/RecommendationService.java  ← the whole flow: candidates → AI → validate → fallback → save
service/RuleBasedRecommender.java   ← deterministic fallback ranking
controller/RecommendationController.java  ← POST /api/recommendations
```

**Why our own interface in front of Spring AI?** The service only knows `RecommendationAiClient`. Tests replace it with a mock (no network, no key, no cost), and switching to another model later only touches one class.

### Request

```json
POST /api/recommendations
{
  "dateOfBirth": "2000-01-10",          ← optional; defaults to the customer's stored DOB
  "insuranceGoals": "Health coverage for self and parents",
  "preferredTerm": "1 year",
  "preferredPolicyType": "HEALTH"
}
```

At least one field is required (400 otherwise). A future date of birth → 400.

### Response

```json
{
  "recommendationRequestId": 6003,
  "calculatedAge": 26,
  "source": "AI",                         ← or "FALLBACK"
  "modelName": "gemini-2.5-flash",        ← or "rule-based-fallback"
  "message": "Personalised recommendations generated by AI",
  "recommendations": [
    { "recommendationItemId": 7004, "rank": 1, "productId": 202, "productCode": "HD-HEALTH-001",
      "policyName": "HealthDekho Family Health", "policyType": "HEALTH", "termMonths": 12,
      "sumInsured": 1000000.00, "premiumFrequency": "YEARLY", "premiumAmount": 18000.00,
      "recommendationReason": "...", "suitability": "HIGH" }
  ]
}
```

These are all the FR-2 table columns. **Register button:** send `productId` + `recommendationItemId` to `POST /api/policies` (Step 6), which marks the item `selected_for_registration = true`.

### What gets saved (audit)

| Table | Row |
|---|---|
| `ai_recommendation_request` | customer, goals, preferred term, preferred type, **calculated age**, **model name**, **model version**, **prompt version**, time |
| `ai_recommendation_item` | one row per recommended product: rank, reason, suitability, selected flag |

Fallback results are saved too (model name `rule-based-fallback`), so the audit trail is complete.

### Validating the AI answer

| AI says | We do |
|---|---|
| A code not in the candidate list (invented, owned, too old/young) | Drop it |
| The same product twice | Keep the first |
| Lower-case code `hd-health-001` | Accept (normalised) |
| Suitability `"unknown"` | Use MEDIUM |
| Empty or very long reason | Default text / cut at 1000 chars |
| Nothing usable at all | Fallback |

### The fallback (`RuleBasedRecommender`)

Deterministic scoring, same input → same output:

| Signal | Points |
|---|---|
| Product type = preferred type | +3 |
| Goal text contains a keyword for the type (e.g. "parents" → HEALTH, "family/income" → TERM, "retire/education" → SAVINGS) | +2 |
| Term within 12 months of the preferred term ("20 years" = 240 months) | +2 (within 60 months: +1) |

Score ≥ 4 → HIGH, ≥ 2 → MEDIUM, else LOW. Ties → cheaper premium first. Top 3.

---

## Part 3 – Run and check

### Without a Gemini key (now)

Postman → **6. AI recommendations** → *Recommend for Rohan*:
`"source": "FALLBACK"`, message *"AI recommendations are unavailable right now (GEMINI_API_KEY is not configured)…"*, Family Health ranked 1st (HIGH).

### With a Gemini key (later)

1. Get a free key at **https://aistudio.google.com** → *Get API key*.
2. PowerShell (once), then reopen VS Code:
   ```powershell
   [Environment]::SetEnvironmentVariable("GEMINI_API_KEY", "<your key>", "User")
   ```
3. Restart the app → same request → `"source": "AI"`, `"modelName": "gemini-2.5-flash"`, AI-written reasons.
   (Optional: `GEMINI_MODEL` env var to try another model.)

### Check the audit rows

```sql
SELECT r.recommendation_request_id, r.calculated_age, r.model_name, r.prompt_version,
       i.rank_order, p.product_code, i.suitability, i.recommendation_reason, i.selected_for_registration
FROM ai_recommendation_request r
JOIN ai_recommendation_item i ON i.recommendation_request_id = r.recommendation_request_id
JOIN insurance_product p ON p.product_id = i.product_id
ORDER BY r.recommendation_request_id DESC, i.rank_order;
```

**Automated tests** (the AI is mocked, so no key or network is needed):
- `RecommendationIntegrationTest` AI-01…05: invented and owned codes filtered, persistence, timeout → fallback, useless answer → fallback, no eligible products → AI not called, invalid input.
- `GeminiRecommendationClientTest` AI-C-01…05: no key, structured answer, timeout, model error, prompt content.
- `RuleBasedRecommenderTest`.

---

## Part 4 – Screenshots

See `docs/screenshot-checklist.md` → rows **AI-01 … AI-04** (slide 7).

---

## Quick recap

- **Java builds the candidate list** (active, not owned, age-eligible). **AI only ranks and explains.**
- **Spring AI `ChatClient`** + **structured output** (`.entity(Record.class)`) → typed Java objects.
- **The AI answer is validated**: unknown or ineligible codes are dropped.
- **Timeout on a virtual thread**, then **rule-based fallback**. The app works even without a key.
- **Everything is saved for audit**, including model name and prompt version.

**Next, Step 10:** clean error handling everywhere.
