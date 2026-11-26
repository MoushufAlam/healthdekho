# Step 12 – React Frontend

> **In one line:** a React app in `frontend/` gives customers a real interface for everything the backend does: login, dashboard, AI recommendations and policy registration. It talks to the Spring Boot API with Axios, using the JWT from login.

**Covers:** README 2.2 (React, functional components, hooks, Axios), FR-1A/1B/1C, FR-2 and FR-3 user interfaces. Also has *optional* screens for FR-4 (payment) and FR-5 (search).

---

## Part 1 – The theory

### 1.1 What is a frontend, and what is a "single-page app"?

The backend (Steps 1–11) only speaks **JSON**. People don't read JSON, so we need a **frontend**: the screens that run in the browser.

```
 ┌──────────── Browser ────────────┐            ┌──────── Spring Boot (8080) ────────┐
 │  React app (HTML + CSS + JS)    │  HTTP/JSON │  /api/auth/login                   │
 │  LoginPage, DashboardPage, ...  │ ─────────► │  /api/dashboard ...                │
 │                                 │ ◄───────── │  (Steps 4–11)                      │
 └─────────────────────────────────┘            └────────────────────────────────────┘
          ▲ first load only: index.html + JS bundle
          │
 ┌────────┴─────────┐
 │ Vite dev server  │  (port 5173, only during development)
 └──────────────────┘
```

**Single-page app (SPA):** the browser downloads **one** HTML page and **one** JavaScript bundle once. After that, clicking "Dashboard" doesn't load a new page from a server; React just swaps what's on the screen and fetches **data** (JSON) from the API.

> 💡 **Analogy:** a TV. You don't buy a new TV for every channel (classic websites); you keep one TV and only change what it shows (SPA).

### 1.2 The tools around React

| Tool | What it is | Analogy |
|---|---|---|
| **Node.js** | Runs JavaScript outside the browser; needed for the build tools | The JVM for JavaScript tooling |
| **npm** | Downloads libraries listed in `package.json` into `node_modules/` | Maven for JavaScript |
| **`package.json`** | Project name, scripts (`npm run dev`) and dependencies | `pom.xml` |
| **`package-lock.json`** | Exact versions that were installed, so every PC gets the same | Maven's resolved dependency tree, frozen |
| **Vite** | Dev server with instant reload + production build (`npm run build` → `dist/`) | `mvnw spring-boot:run` + `mvnw package` |
| **React** | Library for building the UI out of components | – |
| **React Router** | Maps URLs (`/dashboard`) to pages | `@GetMapping`, but in the browser |
| **Axios** | Sends HTTP requests to the backend | `RestClient` / Postman inside the app |
| **Tailwind CSS** | Styling with small ready-made CSS classes | – |
| **lucide-react** | The icon set (shield, sparkles, wallet…) | – |

### 1.3 React in 5 minutes

**A component is a function that returns what should be on the screen.**

```jsx
function Greeting({ name }) {          // { name } = "props", the component's inputs
  return <h1>Welcome back, {name}</h1> // JSX: HTML-like syntax inside JavaScript
}

<Greeting name="Aarav" />              // using it, like an HTML tag
```

| Idea | Meaning | Java comparison |
|---|---|---|
| **Component** | Function returning JSX | A small class that renders one piece of UI |
| **JSX** | `<div className="...">{value}</div>`: HTML-like code; `{ }` inserts JavaScript | String template, but type-checked by the compiler |
| **Props** | Inputs passed by the parent (`name="Aarav"`) | Constructor arguments |
| **State** | Data that belongs to the component and can change (form text, loading flag) | A field; changing it redraws the screen |
| **Re-render** | When state or props change, React calls the function again and updates only what changed | – |

`className` is used instead of `class` (a reserved word in JavaScript).

### 1.4 Hooks: how components remember and react

Hooks are functions starting with `use…`. They can only be called **at the top of a component** (never inside `if`), because React matches them by their order.

| Hook | What it does | Where we use it |
|---|---|---|
| `useState` | A value that survives re-renders; calling the setter redraws | form fields, `submitting`, `result` |
| `useEffect` | Run code **after** rendering, e.g. load data; can clean up | `useApiData` (fetching), auto-logout timer |
| `useContext` | Read shared data without passing props through every level | `useAuth()` gives any page the logged-in customer |
| `useRef` | A box that keeps a value without causing a re-render | last good preview on the registration page, scrolling to results |
| `useMemo` / `useCallback` | Keep the same object/function between renders unless inputs change | the `AuthContext` value and `login`/`logout` |
| **Custom hook** | Your own `use…` function combining hooks | `useApiData(loader, deps)` → `{ data, error, loading, reload }` |

```jsx
const [startDate, setStartDate] = useState(todayIso())   // state + setter
<input value={startDate} onChange={(e) => setStartDate(e.target.value)} />  // "controlled input"
```

**Controlled input:** the input shows the state, and typing updates the state. React state is the single source of truth for the form.

### 1.5 React Router: pages and URLs

```jsx
<Routes>
  <Route path="/login" element={<LoginPage />} />
  <Route element={<ProtectedRoute />}>          ← guard: logged in?
    <Route element={<AppLayout />}>             ← header + nav, page goes into <Outlet />
      <Route path="/dashboard" element={<DashboardPage />} />
      <Route path="/register"  element={<RegisterPolicyPage />} />
    </Route>
  </Route>
</Routes>
```

| Piece | Job |
|---|---|
| `<Route path element>` | "At this URL, show this component" |
| **Nested routes** + `<Outlet />` | The parent (layout) draws the frame; the child page appears where `<Outlet />` is |
| `<Link to>` / `<NavLink>` | Links that change the page without reloading (NavLink knows if it's active, for highlighting) |
| `useNavigate()` | Change page from code (after login → `/dashboard`) |
| `useParams()` | Read `/payments/:policyNumber` → `policyNumber` |
| `useSearchParams()` | Read `?productId=203&recommendationItemId=7004` |
| `<Navigate to>` | Redirect while rendering |

**ProtectedRoute**: if there is no session, redirect to `/login` and remember where the user wanted to go:

```
user opens /recommendations ──► ProtectedRoute: logged in? ── no ──► /login (state.from = /recommendations)
                                         │ yes                                │ after login
                                         ▼                                    ▼
                                 show the page                    back to /recommendations
```

⚠️ This guard is only for a nice user experience. Anyone can edit JavaScript in their browser. **The real protection is the backend**, which rejects every request without a valid JWT (Step 4) and only returns the logged-in customer's data.

### 1.6 Axios: talking to the API

```js
// api/client.js: one configured instance for the whole app
export const api = axios.create({ baseURL: 'http://localhost:8080', timeout: 30_000 })

// request interceptor: runs before EVERY request
api.interceptors.request.use((config) => {
  config.headers.Authorization = `Bearer ${token}`   // the JWT from login
  return config
})

// response interceptor: runs after EVERY response
// 401 (token expired/invalid) → log out and go to /login
```

> 💡 **Interceptor = airport security gate.** Every passenger (request) passes through it, so you write the "attach the token" rule once instead of in 12 places.

**Promises and `async/await`:** network calls take time, so Axios returns a **Promise** ("an answer will come later"). `await` waits for it without freezing the page:

```js
async function handleSubmit() {
  try {
    const response = await registerPolicy(body)  // waits for the server
    setRegistered(response)                       // 201 → success screen
  } catch (err) {
    setSubmitError(toApiError(err).message)       // 400/409 → show the server's message
  }
}
```

`Promise.all([a(), b()])` runs two calls **in parallel** and waits for both (dashboard: policies + products).

### 1.7 The login session

```
LoginPage ──POST /api/auth/login──► { accessToken, expiresAt, customer }
     │
     ▼
sessionStore.set(...)   → sessionStorage (browser tab memory)
AuthContext state       → every component knows "logged in as Aarav"
     │
     ▼
every Axios request: Authorization: Bearer eyJhbGciOi...
```

| Question | Our answer | Why |
|---|---|---|
| Where is the token kept? | `sessionStorage` | Survives a refresh (F5), deleted when the tab closes. `localStorage` would keep it for days. |
| What if it expires? | A timer logs out exactly at `expiresAt` (1 hour), and any 401 response also logs out | The user sees "Your session has expired" instead of broken pages |
| Is the customer id sent anywhere? | **Never.** The backend reads it from the token | Data isolation rule (CLAUDE.md): you can't ask for someone else's data |
| Is the password stored? | No, only the token | – |

### 1.8 CORS: why the browser needed permission

The frontend runs on `http://localhost:5173`, the API on `http://localhost:8080`. Different port = different **origin**. Browsers block JavaScript from reading responses from another origin **unless that server says it's OK**.

```
Browser (page from :5173)                         Spring Boot (:8080)
   │  OPTIONS /api/dashboard   ← "preflight": may I?    │
   │  Origin: http://localhost:5173                      │
   │ ──────────────────────────────────────────────────► │ SecurityConfig.corsConfigurationSource
   │ ◄────────────────────────────────────────────────── │ Access-Control-Allow-Origin: http://localhost:5173
   │  GET /api/dashboard  + Authorization header         │
   │ ──────────────────────────────────────────────────► │
```

The allowed origins come from `app.cors.allowed-origins` (`CORS_ALLOWED_ORIGINS` env var). That's why `vite.config.js` uses `strictPort: true`: if 5173 were busy, Vite would quietly use 5174, and every call would fail with a CORS error.

### 1.9 Tailwind CSS: styling with small classes

Instead of writing CSS files, you combine small classes directly on elements:

```jsx
<button className="rounded-xl bg-brand-600 px-4 py-2 font-semibold text-white hover:bg-brand-700">
```

| Class | Means |
|---|---|
| `px-4 py-2` | padding left/right 1rem, top/bottom 0.5rem |
| `rounded-xl` | rounded corners |
| `bg-brand-600 text-white` | background colour / text colour |
| `hover:bg-brand-700` | different colour **on hover** |
| `sm:` `md:` `lg:` | apply only from that screen width up (responsive) |
| `grid sm:grid-cols-3` | 1 column on phones, 3 columns from small tablets up |

Our colours (`brand-50 … brand-950`, teal) and the font are defined **once** in `src/index.css` inside `@theme`, like constants. Change one hex value and the whole app changes colour.

To avoid repeating long class lists, they live in small components (`Button`, `Card`, `Badge`, `Alert`, `Field`, `Table`), so a page says `<Button icon={Sparkles}>` instead of 15 classes.

### 1.10 Validation: client and server together

| | Client (React) | Server (Spring) |
|---|---|---|
| Purpose | **Fast feedback** while typing | **The real rules**; can't be bypassed |
| Example | "Email is required", "start date can't be in the past" | same checks + age eligibility, duplicate policy, totals |
| If they disagree | The server wins | – |

All server errors have one JSON shape (Step 10). `api/errors.js` turns any Axios error into `{ status, message, fieldErrors }`:
- `message` → shown in a red `Alert`
- `fieldErrors.email` → shown under the email input
- no response at all → "Cannot reach the server. Make sure the backend is running on port 8080."

---

## Part 2 – What we built

### Folder map

```
frontend/
├── index.html                 ← the single HTML page; React mounts into <div id="root">
├── package.json               ← dependencies + scripts (dev, build, preview)
├── vite.config.js             ← React + Tailwind plugins, port 5173 (strict)
├── .env.example               ← VITE_API_BASE_URL (copy to .env.local to change)
├── public/favicon.svg
└── src/
    ├── main.jsx               ← starts React: <BrowserRouter><AuthProvider><App/></AuthProvider></BrowserRouter>
    ├── App.jsx                ← all routes (URL → page)
    ├── index.css              ← Tailwind import + design tokens (@theme)
    ├── api/                   ← ONLY place that knows URLs
    │   ├── client.js          ←   Axios instance + interceptors (JWT, 401)
    │   ├── errors.js          ←   any error → { status, message, fieldErrors }
    │   ├── authApi.js         ←   POST /api/auth/login
    │   ├── customerApi.js     ←   /api/customers/me, /api/dashboard, /api/products/available
    │   ├── policyApi.js       ←   GET /api/policies/preview, POST /api/policies
    │   └── recommendationApi.js ← POST /api/recommendations
    ├── auth/
    │   ├── sessionStore.js    ← read/write the session in sessionStorage (+ expiry check)
    │   ├── AuthContext.jsx    ← login(), logout(), customer; auto-logout on expiry/401
    │   └── ProtectedRoute.jsx ← redirect to /login when not logged in
    ├── hooks/useApiData.js    ← load data on mount / when inputs change; ignores stale answers
    ├── lib/format.js          ← ₹ money, dates (26 Nov 2026), terms (20 years), enum labels
    ├── components/
    │   ├── layout/            ← AppLayout (header, nav, footer), Logo
    │   └── ui/                ← Button, Card, Badge, Alert, Field, Table, EmptyState, StatCard, PageHeader, Spinner
    ├── pages/
    │   ├── LoginPage.jsx          ← FR-1A
    │   ├── DashboardPage.jsx      ← FR-1B + FR-1C + entry to FR-2
    │   ├── RecommendationPage.jsx ← FR-2
    │   ├── RegisterPolicyPage.jsx ← FR-3
    │   └── NotFoundPage.jsx
    └── optional/              ← NOT evaluated; delete to remove
        ├── fr4-payment/       ← PaymentPage + paymentApi (FR-4)
        └── fr5-search/        ← SearchPage + searchApi (FR-5)
```

**Rule of the layers (same idea as the backend):** pages never call Axios directly; they call `api/*.js` functions. Pages handle **screens**, `api/` handles **HTTP**.

```
Page (UI + state) ──► api/*.js (which URL, which body) ──► client.js (base URL, token, 401) ──► Spring Boot
```

### The pages

| URL | Page | FR | Shows | Calls |
|---|---|---|---|---|
| `/login` | LoginPage | 1A | Brand panel + login form; dev-only demo-account buttons | `POST /api/auth/login` |
| `/dashboard` | DashboardPage | 1B, 1C, 2 | Stats, AI banner, **My policies** table (10 README columns), **Available products** table with **Register** | `GET /api/dashboard` + `GET /api/products/available` (parallel) |
| `/recommendations` | RecommendationPage | 2 | Form (DOB, type, term, goals) → ranked table with reason, suitability, **Register** | `POST /api/recommendations` |
| `/register?productId=…[&recommendationItemId=…]` | RegisterPolicyPage | 3 | Pre-filled customer + product, start date, live maturity date, total premium, eligibility → **Confirm** → success with policy number | `GET /api/policies/preview` (on every date change), `POST /api/policies` |
| `/register` (no product) | RegisterPolicyPage → product picker | 3 | Cards of available products | `GET /api/products/available` |
| `/payments[/:policyNumber]` | *optional* PaymentPage | 4 | Lookup, totals, schedule with checkboxes, pay, history | 4 FR-4 endpoints |
| `/search` | *optional* SearchPage | 5 | 3 search fields, paged results | `GET /api/policies/search` |
| anything else | NotFoundPage | – | 404 page | – |

### Flow 1: Login (FR-1A)

```
LoginPage.handleSubmit
  ├─ validate(form)            → empty/invalid email? show field errors, stop
  ├─ AuthContext.login(email, password)
  │     └─ authApi.login → POST /api/auth/login
  │           ├─ 200 → sessionStore.set + setSession → isAuthenticated = true
  │           └─ 401 → Alert "Invalid email or password"
  └─ navigate(state.from ?? '/dashboard')   ← README: "redirect the customer to the dashboard"
```

### Flow 2: Dashboard → Register (FR-1B, 1C, 3)

```
DashboardPage mounts
  └─ useApiData(() => Promise.all([getDashboard(), getAvailableProducts()]))
        ├─ policies.length === 0 → EmptyState with the server's message
        └─ products table → [Register] = <Link to="/register?productId=203">
                                   │
RegisterPolicyPage reads ?productId=203
  └─ RegistrationForm
        ├─ startDate = today (FR-3A "default to current date")
        ├─ useApiData(() => previewRegistration(203, startDate), [productId, startDate])
        │     → customer, product, maturityDate, totalPremiumPayable, eligible, eligibilityMessage
        ├─ user changes date → preview runs again → numbers update
        └─ Confirm → registerPolicy({ productId, startDate, recommendationItemId? })
              ├─ 201 → RegistrationSuccess (policy number + copy button)
              └─ 409/400 → red Alert with the server message, preview reloaded
```

**Why the preview comes from the server:** the maturity date, total premium and eligibility are calculated by the **same Java code** that saves the policy (`PremiumScheduleCalculator`, `eligibilityProblem`). The UI never does insurance maths itself, so the numbers on screen can't disagree with the numbers in the database.

### Flow 3: AI recommendation → registration (FR-2 → FR-3)

```
RecommendationPage
  ├─ validate: at least one field, DOB not in the future
  ├─ getRecommendations(form)      (empty fields removed before sending)
  │     → { source: AI | FALLBACK, modelName, message, calculatedAge, recommendations[] }
  ├─ source badge: "AI powered · gemini-2.5-flash" or "Rule-based suggestions" + yellow notice
  └─ [Register] → /register?productId=202&recommendationItemId=7004
                       └─ on success the backend marks item 7004 as selected_for_registration = true
```

### Design decisions worth knowing

| Decision | Why |
|---|---|
| One Axios instance + interceptors | Token and 401 handling written once |
| `useApiData` ignores stale answers | If you change the start date twice quickly, an older (slower) response can't overwrite the newer one |
| Logout clears the session inside `startTransition` | React Router navigates in a "transition"; doing both together stops the guard from remembering the old page (otherwise the next user to log in landed on the previous user's page) |
| Demo-account buttons only in `npm run dev` | `import.meta.env.DEV` is `false` in a production build, so they're never shipped |
| Money formatted with `Intl.NumberFormat('en-IN')` | Indian grouping: ₹10,00,000 |
| Dates parsed from parts, not `new Date("2026-12-01")` | That form is read as UTC midnight and can show the previous day in some time zones |
| `VITE_API_BASE_URL` env var | Same build works locally and on AWS (Step 13); only variables starting with `VITE_` reach the browser, so never put secrets there |

### Optional FR-4 / FR-5 screens: how to remove them

Everything optional is tagged `OPTIONAL: FR-4` / `OPTIONAL: FR-5`. To delete:
1. Delete `src/optional/fr4-payment/` and/or `src/optional/fr5-search/`.
2. In `App.jsx`: delete the two imports and their `<Route>` lines.
3. In `components/layout/AppLayout.jsx`: delete the "Pay Premium" / "Search" nav entries.
4. FR-4 only: delete the "Action / Pay" column in `DashboardPage.jsx` and the "View payment schedule" button in `RegisterPolicyPage.jsx`.
5. Run `npm run build`: if it builds, nothing is left behind.

---

## Part 3 – Run and check

**One-time on each computer:** Node.js 20+ (`node -v`), then:

```powershell
cd frontend
npm install          # downloads node_modules (not committed to git)
```

**Every time** (two terminals):

```powershell
# terminal 1: backend
cd backend
.\mvnw spring-boot:run

# terminal 2: frontend
cd frontend
npm run dev          # → http://localhost:5173
```

**Production build:** `npm run build` → static files in `frontend/dist/` (served by any web server in Step 13). `npm run preview` serves that build locally.

### Click-through checklist

| # | Do | Expect |
|---|---|---|
| 1 | Open `http://localhost:5173/dashboard` without logging in | Redirect to `/login` |
| 2 | Click **Log in** with empty fields | Red "Email is required" / "Password is required" under the fields |
| 3 | Aarav + wrong password | Red alert "Invalid email or password" (401) |
| 4 | Log in as **Rohan** (demo button) | Dashboard, "No policies yet" with the server message, 3 products |
| 5 | **Get AI recommendations** → type *Health* → goals "Health cover for my parents" | Ranked table; without a Gemini key: "Rule-based suggestions" + yellow notice |
| 6 | Click **Register** on the first row | Form pre-filled, purple "From your AI recommendation" badge |
| 7 | Set start date to a past date | Field error + "Policy start date cannot be in the past", button disabled |
| 8 | Today's date → **Confirm registration** | Success screen with `HDP-2026-0003` |
| 9 | **Go to dashboard** | New policy in the table, product gone from "Available" |
| 10 | Wait 1 hour or delete the token in DevTools → click around | "Your session has expired. Please log in again." |

### Troubleshooting

| Symptom | Cause | Fix |
|---|---|---|
| "Cannot reach the server…" | Backend not running | Start terminal 1 |
| Browser console: *blocked by CORS policy* | Frontend not on 5173, or origin not allowed | Use port 5173 or add your origin to `CORS_ALLOWED_ORIGINS` |
| `Port 5173 is in use` | Another `npm run dev` is still running | Close it (strictPort refuses to switch port on purpose) |
| Logged out after restarting the backend | No `JWT_SECRET` set → random key per start → old tokens invalid | Log in again, or set `JWT_SECRET` (32+ chars) |
| Every request in DevTools appears twice (dev only) | React `StrictMode` runs effects twice in development to find bugs | Normal; not in the production build |

---

## Part 4 – Screenshots

See `docs/screenshot-checklist.md` → section **"Slide 5 – React UI (Step 12)"**. Take them after a fresh reset so the policy numbers match.

---

## Quick recap

- **React** draws the UI from **components**; **hooks** give them state and side effects.
- **React Router** maps URLs to pages; **ProtectedRoute** sends logged-out users to `/login`.
- **Axios** calls the API; one **interceptor** adds the JWT, another logs out on 401.
- **CORS** lets the page on :5173 read responses from :8080.
- **Tailwind** styles everything with small classes; design tokens live in `index.css`.
- The UI shows; **the backend decides** (eligibility, totals, dates, ownership).

**Next:** Step 13, Docker + AWS deployment.
