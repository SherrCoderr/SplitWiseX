# SplitWiseX

Real-time group expense splitting app — Spring Boot + React/TypeScript + PostgreSQL + WebSocket/STOMP.

> **Status: Stage 2 complete — authentication.** Registration, login, JWT
> issuance/validation, protected routes, and a working logged-in dashboard
> shell all work end-to-end against the real backend and PostgreSQL
> database. Groups, expenses, balances, settlements, and real-time updates
> are not implemented yet; those come in Stages 3–8.

## What exists right now

```
splitwisex/
├── docker-compose.yml          # PostgreSQL for local dev
├── backend/                    # Spring Boot (Java 17, Maven)
│   └── src/main/java/com/splitwisex/
│       ├── SplitwisexApplication.java
│       ├── config/
│       │   ├── WebConfig.java              # CORS for the Vite dev server
│       │   └── SecurityConfig.java         # JWT-based, stateless security
│       ├── controller/
│       │   ├── HealthController.java       # GET /api/health
│       │   └── AuthController.java         # POST /api/auth/register, /login
│       ├── service/
│       │   ├── UserService.java            # user lookups
│       │   └── AuthService.java            # register/login business logic
│       ├── repository/
│       │   └── UserRepository.java
│       ├── entity/
│       │   └── User.java
│       ├── dto/auth/
│       │   ├── RegisterRequest.java        # validated: name, email, password
│       │   ├── LoginRequest.java           # validated: email, password
│       │   ├── AuthResponse.java           # token + user
│       │   └── UserSummaryDto.java         # safe-to-expose user fields
│       ├── mapper/
│       │   └── UserMapper.java
│       ├── security/
│       │   ├── JwtService.java             # generate/parse/validate JWTs
│       │   ├── JwtAuthenticationFilter.java
│       │   ├── CustomAuthenticationEntryPoint.java  # JSON 401s
│       │   └── CustomAccessDeniedHandler.java       # JSON 403s
│       └── exception/
│           ├── GlobalExceptionHandler.java  # validation, auth, generic errors
│           ├── DuplicateEmailException.java
│           └── InvalidCredentialsException.java
│   └── src/main/resources/
│       ├── application.yml
│       └── db/migration/
│           └── V1__create_users_table.sql
│   └── src/test/java/com/splitwisex/
│       ├── security/JwtServiceTest.java     # token generation/validation/expiry
│       └── service/AuthServiceTest.java     # register/login logic, mocked deps
└── frontend/                   # React 18 + TypeScript + Tailwind + Vite
    └── src/
        ├── App.tsx             # routes, now with ProtectedRoute / GuestRoute
        ├── context/
        │   └── AuthContext.tsx # holds auth state, exposes login/register/logout
        ├── api/
        │   ├── client.ts       # Axios instance: attaches JWT, handles 401s
        │   └── auth.ts         # typed register/login calls, error extraction
        ├── lib/
        │   ├── authStorage.ts  # localStorage persistence for token + user
        │   └── jwt.ts          # client-side expiry check (no signature check)
        ├── components/
        │   ├── ProtectedRoute.tsx  # redirects unauthenticated users to /login
        │   ├── GuestRoute.tsx      # redirects logged-in users away from /login, /register
        │   └── FormField.tsx       # shared labeled input with inline error
        └── pages/
            ├── Login.tsx        # fully functional
            ├── Register.tsx     # fully functional
            └── Dashboard.tsx    # welcome + logout (full dashboard in Stage 5)
```

### Why these architectural choices

- **Vite** instead of Create React App: CRA is deprecated/unmaintained; Vite is the
  current standard for React+TS projects and is what you'd be expected to know in an interview.
- **Flyway** for schema migrations instead of `ddl-auto: update`: lets the database
  schema be version-controlled and reviewable, which is what real backend teams do.
  `ddl-auto` is set to `validate` — Hibernate checks the schema matches the entities
  but never silently mutates it.
- **JWT via Authorization header, not cookies**: the frontend stores the token in
  `localStorage` and an Axios interceptor attaches it as `Authorization: Bearer <token>`
  on every request. This keeps the backend fully stateless (no session store) and
  avoids CSRF entirely, at the cost of the token being readable by any JS on the page —
  an httpOnly cookie would close that gap but needs CSRF protection in return. That
  trade-off is documented inline in `lib/authStorage.ts`.
- **No `UserDetailsService`**: since there's exactly one way to authenticate (JWT) and
  no roles/authorities yet, `JwtAuthenticationFilter` loads the `User` directly from
  `UserRepository` and puts it straight into the `SecurityContext`. This avoids a layer
  of indirection that would only pay off once role-based authorization exists.
- **Deliberately generic login errors**: `InvalidCredentialsException` never says
  whether the email or the password was wrong, so the login endpoint can't be used to
  enumerate which emails have accounts.
- **CORS preflight is explicitly `permitAll`ed at the Security layer**
  (`HttpMethod.OPTIONS, "/**"`), separately from the actual CORS headers in
  `WebConfig`. Spring Security's filter chain runs before Spring MVC's own CORS
  handling, so without this, an `OPTIONS` preflight to any endpoint that requires
  authentication (starting in Stage 3) would be rejected before ever reaching the code
  that adds the CORS headers — a classic, easy-to-miss gotcha.

## Prerequisites

- Java 17+
- Maven 3.9+ (or use your IDE's bundled Maven)
- Node.js 18+ and npm
- Docker (for PostgreSQL) — or a local PostgreSQL 16 instance if you prefer not to use Docker

## How to run it

**1. Start PostgreSQL**

```bash
cd splitwisex
docker compose up -d
docker compose ps          # wait for "healthy"
```

**2. Start the backend**

```bash
cd backend
mvn spring-boot:run
```

Flyway will run `V1__create_users_table.sql` automatically on startup. Confirm it's up:

```bash
curl http://localhost:8080/api/health
```

**3. Start the frontend**

```bash
cd frontend
npm install
npm run dev
```

Open `http://localhost:5173`.

## Verifying Stage 2 end-to-end

1. Go to `http://localhost:5173/register`, create an account (e.g. name
   "Sameer Kumar", a real-looking email, and a password with 8+ characters
   including a letter and a number). You should land on `/dashboard` showing
   your name and email.
2. Confirm the row landed in Postgres:
   ```bash
   docker exec -it splitwisex-postgres psql -U splitwisex -d splitwisex -c "SELECT id, name, email FROM users;"
   ```
3. Click **Log out** — you should be sent back to the landing page, and
   navigating to `/dashboard` directly should bounce you to `/login`.
4. Log back in with the same email/password — you should land on `/dashboard` again.
5. Try registering the same email twice — you should see "An account with
   this email already exists" under the email field.
6. Try logging in with a wrong password — you should see "Invalid email or
   password".
7. Try `curl http://localhost:8080/api/auth/register` with a missing field,
   e.g.:
   ```bash
   curl -i -X POST http://localhost:8080/api/auth/register \
     -H "Content-Type: application/json" \
     -d '{"name":"","email":"not-an-email","password":"short"}'
   ```
   You should get a `400` with a `fieldErrors` object naming all three problems.
8. Try hitting a token-protected scenario manually — since there's no
   protected business endpoint yet (that's Stage 3), you can instead confirm
   `JwtAuthenticationFilter` is wired correctly by checking that a request
   with a garbage token is rejected:
   ```bash
   curl -i http://localhost:8080/api/health   # 200, public
   ```

## Running backend tests

```bash
cd backend
mvn test
```

This runs `JwtServiceTest` (token generation, tampering, expiry) and
`AuthServiceTest` (register/login logic with mocked dependencies — no
database required). Both are unit tests; full request/response integration
testing is planned for Stage 8.

> **Note on this repository's own verification:** the frontend build
> (`npm run build`, i.e. `tsc -b && vite build`) was run and passes with zero
> TypeScript errors. The backend, however, could **not** be compiled or
> tested in the sandbox this was built in, because that environment's
> network policy blocks Maven Central. The code was written carefully and
> reviewed by hand (including catching and fixing a real bug — a multi-catch
> block in `JwtService` that mixed a superclass and subclass exception type,
> which is a Java compile error). Please run `mvn compile` and `mvn test`
> yourself as the first step, per the "How to run it" section above, and
> report back anything that doesn't build cleanly.

## Environment variables (backend)

All have working local defaults baked into `application.yml`, so you don't need to
set anything to run locally with the provided `docker-compose.yml`. Override them if
your setup differs:

| Variable            | Default                                      | Purpose                          |
|---------------------|-----------------------------------------------|-----------------------------------|
| `DB_NAME`            | `splitwisex`                                  | Postgres database name           |
| `DB_USER`            | `splitwisex`                                  | Postgres username                |
| `DB_PASSWORD`        | `splitwisex`                                  | Postgres password                |
| `JWT_SECRET`         | dev placeholder                               | HMAC signing key for JWTs. **Change this for anything beyond local dev.** |
| `JWT_EXPIRATION_MS`  | `86400000` (24h)                              | JWT lifetime                     |

## API reference (Stage 2)

### `POST /api/auth/register`
```json
// request
{ "name": "Sameer Kumar", "email": "sameer@example.com", "password": "password123" }

// 201 response
{
  "token": "eyJ...",
  "tokenType": "Bearer",
  "expiresInMs": 86400000,
  "user": { "id": 1, "name": "Sameer Kumar", "email": "sameer@example.com" }
}
```
`400` on validation failure (with `fieldErrors`), `409` on a duplicate email.

### `POST /api/auth/login`
```json
// request
{ "email": "sameer@example.com", "password": "password123" }

// 200 response — same shape as register
```
`401` on a wrong email or password (same generic message either way).

## Roadmap

- [x] Stage 1 — Project skeleton (backend, frontend, database, run instructions)
- [x] Stage 2 — Registration, login, JWT, Spring Security
- [ ] Stage 3 — Groups, members, expenses, PostgreSQL relationships
- [ ] Stage 4 — Balance calculation, settlement algorithm
- [ ] Stage 5 — React dashboard, group pages, expense UI, balance UI
- [ ] Stage 6 — WebSockets, real-time group updates
- [ ] Stage 7 — Analytics, error handling, loading states, validation, UI polish
- [ ] Stage 8 — Testing and final README

