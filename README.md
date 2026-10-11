# SecApp

SecApp is a full-stack application for secure user authentication, account administration, and audit visibility. It combines a React/Next.js frontend with a Spring Boot backend and PostgreSQL storage. The platform supports public signup, login, refresh-token rotation, role-based user management, and audit log access.

## Overview

This repository contains both the client and backend APIs for the application:

- `CapstoneOne/frontend` — Next.js user portal and dashboard
- `CapstoneOne/backend` — Spring Boot API, JWT auth, and data services

The frontend provides the user experience for login, signup, session refresh, and dashboard interactions. The backend is the real security boundary and enforces user validation, role checks, session state, and database access controls.

## Architecture

```text
Browser
  └─ Next.js / React app (localhost:3000)
       ├─ Redux auth state
       ├─ Axios client with refresh retry handling
       └─ JSON + bearer token flow
              ↓ CORS
       Spring Boot REST API (localhost:8080)
       ├─ Spring Security filter chain
       ├─ JWT validation and role authorization
       ├─ service and repository layers
       ├─ PostgreSQL application database
       └─ restricted PostgreSQL audit writer/reader roles
```

## Features

### Authentication and sessions

- Public signup for a normal `USER` account
- Login with password validation and BCrypt hashing
- JWT access tokens and refresh-token cookie rotation
- Session revocation on logout and account deletion
- User-friendly refresh support in the frontend
- Replay protection for stale or reused refresh tokens

### Role-based access

Three roles are implemented:

- `USER` — self-service profile access
- `ADMIN` — user management and privileged administration
- `AUDITOR` — read-only access to audit records

### Security and resilience

- Server-side validation of DTOs
- Duplicate username/email checks
- Structured JSON error responses with correlation IDs
- Rate limiting for login and authenticated traffic
- Database circuit breakers for database dependency failures
- Audit log write and read separation at the database level

### Operational capabilities

- PostgreSQL-backed account/session persistence
- Flyway migrations for schema evolution
- OpenAPI/Swagger-generated API documentation
- Best-effort audit logging with restricted database permissions

## Technology stack

### Frontend

- Next.js
- React
- TypeScript
- Redux Toolkit
- Axios
- Tailwind CSS
- Zod validation

### Backend

- Java 17
- Spring Boot
- Spring Security
- Spring Data JPA / Hibernate
- PostgreSQL
- Flyway
- Resilience4j
- JJWT
- BCrypt
- Springdoc OpenAPI

## Repository layout

```text
icc-get-team-Quantum/
├── README.md
├── .gitignore
├── CapstoneOne/
│   ├── backend/
│   │   ├── src/
│   │   ├── .env.example
│   │   ├── pom.xml
│   │   ├── mvnw
│   │   ├── README.md
│   │   └── ...
│   └── frontend/
│       ├── src/
│       ├── package.json
│       ├── README.md
│       └── ...
└── ...
```

## Prerequisites

Before running the project locally, install:

- Java 17 or newer
- PostgreSQL
- Node.js and npm
- Git

## Local setup

### 1) Backend setup

```bash
cd CapstoneOne/backend
cp .env.example .env
# fill in DB credentials, JWT secret, and frontend origin
./mvnw spring-boot:run
```

Review the backend README for environment variables, Flyway details, audit DB setup, and security configuration.

### 2) Frontend setup

```bash
cd CapstoneOne/frontend
npm install
npm run dev
```

Open the frontend in the browser:

- http://localhost:3000

The backend default API port is:

- http://localhost:8080

## API overview

The backend exposes the following key API areas:

| Method | Endpoint | Access | Purpose |
| --- | --- | --- | --- |
| `POST` | `/signup` | Anonymous | Register a normal user |
| `POST` | `/login` | Public | Authenticate and issue tokens |
| `POST` | `/refresh` | Public with valid refresh cookie | Rotate session |
| `POST` | `/logout` | Active access token | Revoke current session |
| `POST` | `/refresh/logout` | Refresh cookie + allowed Origin | Revoke current session |
| `GET` | `/auth` | `USER`, `ADMIN` | Check current identity |
| `GET` | `/users/me` | `USER`, `ADMIN` | Read own safe profile |
| `GET` | `/users` | `ADMIN` | List users |
| `GET` | `/users/{id}` | `ADMIN` | Inspect a user |
| `POST` | `/users` | `ADMIN` | Create a user with a selected role |
| `DELETE` | `/users/{id}` | `ADMIN` | Delete a user and revoke sessions |
| `GET` | `/audit/logs` | `AUDITOR` | Read audit records |

## Documentation

- Backend docs: `CapstoneOne/backend/README.md`
- Database setup notes: `CapstoneOne/backend/src/main/resources/db/README.md`
- Swagger UI at: `http://localhost:8080/swagger-ui.html`
- Swagger JSON: `http://localhost:8080/v3/api-docs`

## Testing

### Backend

```bash
cd CapstoneOne/backend
./mvnw test
```

### Frontend

```bash
cd CapstoneOne/frontend
npm test
npm run lint
npm run build
```

## Security and operational notes

- Keep secrets in environment variables or a secret manager.
- Never log raw passwords, tokens, or JWT signing keys.
- Use HTTPS in deployment and keep cookie settings secure.
- Keep at least one active administrator account.
- Do not modify applied Flyway migrations.
- For production, validate PostgreSQL access grants and audit DB permissions in a disposable environment before rollout.

## Next steps

This project is already structured as a secure starter application for real-world authentication and admin workflows. Typical next improvements include:

- stronger cookie-based token storage and CSRF safeguards
- distributed or shared rate limiting for multi-instance deployments
- expanded admin and auditor UIs
- deeper logging, alerting, and observability
- complete lifecycle features such as password reset, email verification, and account lockouts

## Related documentation

- [Backend README](CapstoneOne/backend/README.md)
- [Frontend README](CapstoneOne/frontend/README.md)
