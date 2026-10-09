# CapstoneOne Backend

This backend provides the secure authentication, user-management, and audit APIs for the CapstoneOne platform. It is built with Java 17, Spring Boot, Spring Security, Spring Data JPA, PostgreSQL, Flyway, Resilience4j, and OpenAPI/Swagger support.

The service is designed for a production-style internal application flow with:

- public signup and login
- role-based authorization (`USER`, `ADMIN`, `AUDITOR`)
- JWT access tokens and refresh-token rotation
- server-side session tracking and logout/revocation support
- administrative user creation and deletion rules
- audit log exposure with least-privilege database read permissions
- rate limiting and database circuit-breaker protection

## Project goals

The backend enforces security at the application boundary instead of trusting UI state. It validates inputs, normalizes usernames/emails, stores only hashed passwords, persists sessions in PostgreSQL, rotates refresh tokens, and logs security-relevant events to a separate audit store.

## Technology stack

- Java 17
- Maven
- Spring Boot
- Spring Web MVC
- Spring Security
- Spring Data JPA / Hibernate
- PostgreSQL
- Flyway
- Resilience4j
- JJWT for JWT signing and validation
- BCrypt for password hashing
- Springdoc OpenAPI
- Spring Data JPA audit persistence with a bounded asynchronous writer queue

## Project structure

```text
backend/
├── src/main/java/com/hdfclife/capstoneone/
│   ├── auth/            # controller, dto, entity, exception, filter, repository, security, service
│   ├── user/            # controller, dto, entity, exception, repository, service
│   ├── audit/           # controller, dto, logging, model, repository, service
│   ├── common/          # cross-feature config, dto, exception, security, util, validation
│   ├── ratelimit/       # service and pool, with aspect/
│   ├── circuitbreaker/  # aspect/
│   └── logging/         # shared logging and request filter
├── src/main/resources/
│   ├── application.yaml
│   ├── logback-spring.xml
│   └── db/
│       ├── migration/   # Flyway SQL for app tables
│       ├── README.md    # audit database setup notes
│       ├── promote-initial-admin.sql
│       └── ...
├── .env.example
├── .env
├── pom.xml
├── mvnw
├── README.md
├── logs/
└── target/
```

## Runtime architecture

The application uses two separate data paths:

1. Main application datasource
   - stores users and session metadata
   - managed by the application role
   - contains `users` and `auth_sessions`

2. Audit datasource
   - stores audit events using separate reader and writer credentials
   - writes through a bounded in-process asynchronous queue and a Spring-managed JPA persistence unit
   - reads through a dedicated read-only JPA persistence unit
   - provisioned by DBA-managed scripts instead of the app login

## Security model

### Authentication and sessions

The backend issues signed JWT access and refresh tokens. Access tokens are short-lived and intended for API bearer authentication. Refresh tokens are stored as a server-side hash plus session metadata and are rotated during refresh to prevent replay and stale-token reuse. Refresh sessions have both an idle expiry and a fixed maximum lifetime, so rotation cannot keep a session alive indefinitely.

Authentication flow:

1. `POST /signup` creates a normal `USER` account.
2. `POST /login` verifies credentials and account state.
3. The server returns an access JWT and sets an HttpOnly refresh cookie.
4. `JwtAuthenticationFilter` validates each bearer token.
5. The session row is checked to ensure it is active and the token IDs match.
6. The current user role and enabled/locked state are loaded before authorization is applied; disabled or locked users cannot continue using existing tokens.
7. Refresh tokens rotate on use and require an allowed `Origin`. Refresh tokens are scoped to `/refresh`; access-token logout uses `/logout`, while refresh-cookie logout uses `/refresh/logout`.

Access and refresh JWTs carry distinct token types, audiences, issuer, and a fixed session-expiry claim. Tokens lacking these claims are rejected; users will need to sign in again after deployment.

### Authorization rules

- `USER` and `ADMIN` can access their own profile details.
- `ADMIN` can list users, inspect them, create users, and delete users.
- `AUDITOR` can read paginated audit logs only.
- Unmatched routes are denied by default.
- Password hashes and tokens are never returned in API payloads.

### Security hardening features

- BCrypt password hashing
- Jakarta Bean Validation on inbound DTOs
- duplicate username/email checks
- correlation IDs for logs and error tracing
- refresh-token rotation and revocation
- per-user and per-IP rate limits
- database circuit breakers
- asynchronous audit writes with bounded queue and explicit overflow/failure logging
- least-privilege audit storage access

Audit writes are best-effort and queued in process memory. A full queue, database outage, or forced process shutdown can lose queued events; failure and queue-overflow events are reported to console through `AUDIT_STORAGE_STATUS`. Use a durable broker or database outbox if audit delivery must survive process restarts.

## Environment configuration

The backend reads environment variables from `.env` via Spring config import.

Example `.env`:

```dotenv
DB_URL=jdbc:postgresql://localhost:5432/capstone_one
DB_USERNAME=capstone_app
DB_PASSWORD=your-local-db-password

AUDIT_DB_WRITER_USERNAME=capstone_audit_writer
AUDIT_DB_WRITER_PASSWORD=your-audit-writer-password
AUDIT_DB_READER_USERNAME=capstone_audit_reader
AUDIT_DB_READER_PASSWORD=your-audit-reader-password

JWT_SECRET=replace-with-openssl-rand-base64-32-output
JWT_ACTIVE_KEY_ID=primary
JWT_PREVIOUS_KEY_ID=
JWT_PREVIOUS_SECRET=
JWT_EXPIRATION=5m
JWT_REFRESH_EXPIRATION=16h40m
JWT_REFRESH_MAX_LIFETIME=7d
JWT_ISSUER=capstoneone
JWT_ACCESS_AUDIENCE=capstoneone-api
JWT_REFRESH_AUDIENCE=capstoneone-refresh

FRONTEND_ALLOWED_ORIGINS=http://localhost:3000
AUTH_REFRESH_COOKIE_SECURE=true
AUTH_REFRESH_COOKIE_SAME_SITE=Lax
```

Notes:

- Generate `JWT_SECRET` using a cryptographically random key (for example, `openssl rand -base64 32`) and store it in secret management. The backend requires at least 32 decoded bytes.
- JWT expiration defaults are 5 minutes for access tokens, 16 hours 40 minutes for refresh-token inactivity, and 7 days maximum per login session. Override them with duration values such as `15m` or `7d`; numeric values remain milliseconds.
- Keep JWT issuer and audiences stable across application instances; use different audiences for access and refresh tokens.
- `DB_URL` is usually PostgreSQL on `localhost:5432` for local work.
- `FRONTEND_ALLOWED_ORIGINS` must match the frontend origin used in the browser.
- `AUTH_REFRESH_COOKIE_SECURE` should stay `true` in production unless local HTTP testing is required.
- For cross-site frontend/API deployments, configure `AUTH_REFRESH_COOKIE_SAME_SITE=None` and keep the cookie secure; refresh and cookie-based logout also require an allowed `Origin`.
- To rotate JWT signing keys without immediately invalidating active sessions, set a new `JWT_SECRET` and `JWT_ACTIVE_KEY_ID`, and configure the old key and ID in `JWT_PREVIOUS_SECRET` and `JWT_PREVIOUS_KEY_ID`. Remove the previous key after the 7-day maximum session lifetime has passed.
- Application rate limits are local to each process. In multi-instance deployments, enforce shared limits at a trusted gateway; never trust forwarded client-IP headers unless the ingress proxy removes client-supplied values and sets its own.

## Database setup and Flyway

The application uses PostgreSQL with Flyway-managed schema updates.

### Main application database

The main database is initialized and maintained by a DBA or local admin. Example local setup:

```sql
CREATE ROLE capstone_app LOGIN;
\password capstone_app
CREATE DATABASE capstone_one OWNER capstone_app;
```

Then grant the required privileges on `public` and the application tables. The application login is not a superuser.

Flyway ensures the schema is checked and migrations are applied on startup. Hibernate is configured with:

```yaml
jpa:
  hibernate:
    ddl-auto: validate
```

This means the app validates schema compatibility instead of auto-generating or modifying database tables.

### Audit database

The audit database is provisioned separately with DBA scripts under `src/main/resources/db`. Those scripts create restricted writer and reader accounts, define the audit table, and configure retention policies.

A separate writer account inserts approved audit columns, while the reader account only selects a safe subset of data. This keeps the audit pipeline isolated from standard application privileges.

## Endpoints

| Method | Route | Access | Purpose |
| --- | --- | --- | --- |
| `POST` | `/signup` | Anonymous | Create a new `USER` account |
| `POST` | `/login` | Public | Validate credentials and issue tokens |
| `POST` | `/refresh` | Public with valid refresh cookie | Rotate and renew session |
| `POST` | `/logout` | Active access token | Revoke current session |
| `POST` | `/refresh/logout` | Refresh cookie + allowed Origin | Revoke current session |
| `GET` | `/auth` | `USER`, `ADMIN` | Validate identity |
| `GET` | `/users/me` | `USER`, `ADMIN` | View own safe account summary |
| `GET` | `/users?page=0&size=50` | `ADMIN` | List users (paginated; maximum page size 100) |
| `GET` | `/users/{id}` | `ADMIN` | Inspect a user |
| `POST` | `/users` | `ADMIN` | Create `USER`, `ADMIN`, or `AUDITOR` |
| `DELETE` | `/users/{id}` | `ADMIN` | Delete user and revoke sessions |
| `GET` | `/audit/logs` | `AUDITOR` | Read paginated audit records |

## Request examples

### Signup

```http
POST /signup
Content-Type: application/json

{
  "username": "jdoe",
  "email": "jdoe@example.com",
  "password": "Str0ngPass!"
}
```

### Login

```http
POST /login
Content-Type: application/json

{
  "username": "jdoe",
  "password": "Str0ngPass!"
}
```

### Refresh

```http
POST /refresh
Origin: http://localhost:3000
Cookie: refresh=...;
```

### Swagger docs

When the app is running, OpenAPI docs are available at:

- `/swagger-ui.html`
- `/v3/api-docs`

## Error handling and tracing

The API returns structured JSON errors with fields such as:

- timestamp
- status
- error
- message
- path
- correlationId

The application supports a `X-Correlation-ID` request header. If a UUID is supplied, it is preserved; otherwise, one is generated. This helps correlate logs and client-reported issues with server-side operations.

Common statuses:

- `400` invalid input or malformed request
- `401` missing or invalid auth
- `403` insufficient role or forbidden action
- `404` resource not found
- `409` duplicate user or rejected account operation
- `429` rate limit exceeded
- `503` database unavailable or breaker open
- `500` unexpected server error

## Rate limiting

The backend uses Resilience4j with local Caffeine-backed caches for in-memory rate limiting.
Central AOP aspects apply database circuit breakers to selected service operations, login
limits to authentication, and authenticated API limits to controller invocations. Limits
are configured under `app.rate-limit` in `application.yaml`; circuit-breaker policies are
configured under `resilience4j.circuitbreaker`.

Default limits:

- login by username: 5 per minute
- login by client IP: 30 per minute
- refresh/logout by client IP: 60 per minute
- authenticated requests by user: 120 per minute
- authenticated requests by IP: 300 per minute
- sensitive routes (`/users/**`, `/audit/**`): 30 per minute per user

These are instance-local limits and do not coordinate across multiple servers. In multi-instance deployments, enforce limits at a gateway or use a shared distributed rate-limit store.

## Circuit breakers

Two circuit breakers are configured:

- `authenticationDatabase`
- `auditLogDatabase`

They protect the app from repeated database failures and temporarily stop calling a dependency when it is unhealthy. The central circuit-breaker aspect applies these policies to the relevant service operations; shared policy values are defined once and each breaker selects the shared configuration.

## Running the backend locally

Prerequisites:

- Java 17+
- PostgreSQL running locally
- Maven wrapper (`mvnw`) is included

Setup:

```bash
cd CapstoneOne/backend
cp .env.example .env
# fill in .env values for DB, audit DB, and JWT secret
./mvnw spring-boot:run
```

The API runs on the default Spring Boot port:

- http://localhost:8080

## Testing

Run the backend test suite:

```bash
cd CapstoneOne/backend
./mvnw test
```

The project uses H2-backed test configurations where appropriate, but PostgreSQL-specific checks such as role grants, retention triggers, and audit privileges should still be verified against a disposable PostgreSQL environment before production deployment.

## Operational guidance

- Store secrets in environment variables or a real secret manager.
- Do not log raw passwords, JWTs, secret keys, or request bodies.
- Use HTTPS and a trusted proxy configuration in production.
- Keep at least one active administrator account at all times.
- Do not modify existing Flyway migration files after they are applied.
- Use new migrations for schema evolution in shared environments.

## Related docs

- [Database setup guide](src/main/resources/db/README.md)
- [Project overview](../../README.md)
