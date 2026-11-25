# Step 1 – Project Skeleton

> **In one line:** we created an empty Spring Boot app that starts, connects to PostgreSQL, and reports "I'm healthy".

---

## Part 1 – The theory

### 1.1 What Spring Boot does for us

Plain Spring needs lots of setup code. **Spring Boot** uses **auto-configuration**: it looks at which libraries are in `pom.xml` and sets them up for you.

| You add this dependency | Spring Boot automatically gives you |
|---|---|
| `spring-boot-starter-web` | A web server (Tomcat) on port 8080 + JSON support |
| `spring-boot-starter-data-jpa` + `postgresql` | Database connection, Hibernate, repositories |
| `flyway-core` | Runs our SQL migration files on startup (Step 2) |
| `spring-boot-starter-actuator` | A ready-made `/actuator/health` URL |
| `spring-boot-starter-validation` | `@NotNull`, `@Positive` etc. on request data |

`@SpringBootApplication` on the main class switches this on. It also scans every class under `com.healthdekho` to find your controllers, services and repositories.

### 1.2 Configuration files and profiles

Settings live in `src/main/resources/`. We use **profiles**, which are different settings for different environments:

```
application.yml        ← shared by everyone (always loaded)
application-dev.yml    ← your laptop (loaded when profile = dev)
application-prod.yml   ← AWS later  (loaded when profile = prod)
```

Spring loads `application.yml` first, then puts the profile file on top of it. Our default profile is `dev`.

### 1.3 Keeping secrets out of the code

The database password must **never** go into Git. So the config file only has a placeholder:

```yaml
password: ${DB_PASSWORD}
```

`${DB_PASSWORD}` means: *"read this value from the computer's environment variable called DB_PASSWORD"*. You saved that variable once in Windows, so the password lives only on your machine.

### 1.4 Maven Wrapper (`mvnw`)

`mvnw` is a small script that downloads the exact Maven version the project needs. Use `.\mvnw` instead of `mvn`, and both your computers build exactly the same way.

---

## Part 2 – What we built

```
backend/
├── pom.xml                        ← dependencies (Spring Boot 3.5.7, Java 21)
├── mvnw, mvnw.cmd, .mvn/          ← Maven Wrapper
└── src/main/
    ├── java/com/healthdekho/
    │   ├── HealthDekhoApplication.java   ← main class, starts everything
    │   ├── controller/   ← receives HTTP requests
    │   ├── service/      ← business rules
    │   ├── repository/   ← talks to the database
    │   ├── entity/       ← Java classes mapped to tables
    │   ├── dto/          ← request/response shapes (records)
    │   ├── ai/           ← Spring AI (Step 9)
    │   ├── config/       ← configuration classes
    │   └── exception/    ← error handling
    └── resources/
        ├── application.yml, application-dev.yml, application-prod.yml
        └── db/migration/   ← SQL files (Step 2)
```

The folders follow the **layered architecture** the README asks for (§2.3). A request will travel like this:

```
HTTP request → Controller → Service → Repository → PostgreSQL
```

**Two important settings in `application.yml`:**
- `ddl-auto: validate`: Hibernate never creates tables; Flyway does (Step 2).
- `open-in-view: false`: the database connection closes once the service is done, so no hidden queries run later.

**Not added yet (on purpose):**
- **Spring Security** comes in Step 4. Added now, it would lock every URL behind a login.
- **Spring AI** comes in Step 9. It needs the Gemini key to start.

---

## Part 3 – Run and check

```powershell
cd backend
.\mvnw test                # starts the app once in a test, checks it boots
.\mvnw spring-boot:run     # runs the app
```

Open **http://localhost:8080/actuator/health**. You should see:

```json
{ "status": "UP", "components": { "db": { "status": "UP" } } }
```

`db: UP` proves the app reached PostgreSQL.

**Verified on 2026-11-25:** test passed ✅, health UP ✅, db UP (PostgreSQL 18.6) ✅

**Harmless warning:** *"Flyway upgrade recommended: PostgreSQL 18.6 is newer…"*. Flyway officially lists up to PG 17, but it works fine with 18. Ignore it.

**If something fails:** a wrong `DB_PASSWORD`, or Postgres not running, gives a `password authentication failed` or `Connection refused` error on startup.

---

## Quick recap

- **Spring Boot** auto-configures whatever is in `pom.xml`.
- **Profiles** (`dev`, `prod`) = different settings per environment.
- **`${VAR}`** reads secrets from environment variables, never from Git.
- **`/actuator/health`** is a free endpoint to check the app and DB are up.

**Next, Step 2:** create the 7 database tables with Flyway.
