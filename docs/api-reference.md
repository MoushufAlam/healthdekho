# HealthDekho API Reference

Base URL (dev): `http://localhost:8080`
All endpoints except login and health need the header `Authorization: Bearer <accessToken>`.
The customer is always taken from the token, so no endpoint takes a customer ID.

| Method | Path | Purpose | FR | Success |
|---|---|---|---|---|
| POST | `/api/auth/login` | Log in, get a JWT | 1A | 200 |
| GET | `/api/customers/me` | Logged-in customer's profile | 3A | 200 |
| GET | `/api/dashboard` | My policies + empty-state message | 1B | 200 |
| GET | `/api/products/available` | Active products I don't hold yet | 1C | 200 |
| GET | `/api/policies/preview?productId=&startDate=` | Pre-filled registration form, calculated dates/total, eligibility | 3A | 200 |
| POST | `/api/policies` | Register a policy | 3 | 201 + `Location` |
| GET | `/api/policies/{policyNumber}` | Policy header with totals | 4A | 200 |
| GET | `/api/policies/{policyNumber}/installments` | Payment schedule | 4B | 200 |
| POST | `/api/policies/{policyNumber}/payments` | Pay one or more installments | 4C | 201 |
| GET | `/api/policies/{policyNumber}/payments` | Payment history | 4C | 200 |
| GET | `/api/policies/search?policyNumber=&customerName=&dateOfBirth=&page=&size=` | Search (paged) | 5 | 200 |
| POST | `/api/recommendations` | AI recommendations (with fallback) | 2 | 201 |
| GET | `/actuator/health` | Health check (public) | – | 200 |

## Request bodies

```jsonc
// POST /api/auth/login
{ "email": "aarav.sharma@example.com", "password": "Password@123" }

// POST /api/policies   (startDate optional = today; recommendationItemId optional)
{ "productId": 203, "startDate": "2026-12-01", "recommendationItemId": 7004 }

// POST /api/policies/{policyNumber}/payments   (paymentMethod: UPI | NET_BANKING | CARD | CASH | CHEQUE)
{ "paymentMethod": "UPI", "installments": [ { "installmentId": 4003, "amount": 500.00 } ] }

// POST /api/recommendations   (all optional, at least one; preferredPolicyType: TERM | HEALTH | SAVINGS | LIFE)
{ "dateOfBirth": "2000-01-10", "insuranceGoals": "Health cover for parents", "preferredTerm": "1 year", "preferredPolicyType": "HEALTH" }
```

## Error shape (every error)

```json
{ "timestamp": "...", "status": 400, "error": "Bad Request", "message": "...", "path": "/api/...",
  "fieldErrors": { "field": "message" } }
```

`fieldErrors` only appears for field validation errors.

| Status | Meaning |
|---|---|
| 400 | Invalid input or business rule broken (message explains) |
| 401 | Wrong login, or missing/invalid/expired token |
| 404 | Not found, **or belongs to another customer** |
| 405 | Wrong HTTP method |
| 409 | Duplicate policy, installment already paid, policy fully paid |
| 500 | Unexpected server error |

## Formats

- Dates: `yyyy-MM-dd`. Timestamps: ISO-8601 UTC. Installment `period`: `yyyy-MM`.
- Money: JSON numbers with 2 decimals (`1250.00`).
- Search pages: `{ content[], page, size, totalElements, totalPages, message? }`. `size` is 1–50, default 10.

## Demo logins

| Customer | Email | Password | Has |
|---|---|---|---|
| Aarav Sharma (1001) | aarav.sharma@example.com | Password@123 | HDP-2026-0001 (Secure Term) |
| Meera Iyer (1002) | meera.iyer@example.com | Password@123 | HDP-2026-0002 (Family Health) |
| Rohan Verma (1003) | rohan.verma@example.com | Password@123 | nothing (+ HDP-DEMO-0001 after the demo script) |

Postman collection: `docs/postman/HealthDekho.postman_collection.json`
