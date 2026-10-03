# Awana Betania – Branch Management Web Application

[![CI](https://github.com/raul-dan23/awanabetania/actions/workflows/ci.yml/badge.svg)](https://github.com/raul-dan23/awanabetania/actions/workflows/ci.yml)

> A full-stack web application built for [Awana Romania](https://awanabetania.eu), a non-profit Christian youth organization operating multiple branches across Romania.  
> **Live at:** [awanabetania.eu](https://awanabetania.eu)

---

## Overview

This application was designed and built from scratch to support the day-to-day operations of the Timișoara branch. It is actively used by **100+ users** and runs in a self-hosted production environment.

The system handles member management, scoring, internal coordination, an NFC-powered season-end fair, and an Olimpiada arbitration system — replacing manual processes with a centralized, reliable platform.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Java 17 · Spring Boot 3.5 · Spring Security (JWT) · JPA/Hibernate · REST API |
| Frontend | React 19 · Vite · Responsive UI |
| Database | MySQL 8 · Flyway migrations |
| NFC Bridge | Java (javax.smartcardio) · WebSocket · PC/SC |
| Server | Ubuntu Linux (self-hosted) · systemd |
| Delivery | GitHub Actions CI · scripted deploy with pre-flight check, health check and automatic rollback |

---

## Features

### Member & Score Management
- Children sign up with a username and password; leaders are added by the director and sign in with Google
- Forgotten passwords are reset by the director (a one-time temporary password the user must change)
- Per-meeting scoring: attendance, Bible, lesson, extra points
- Season-wide point tracking with daily and cumulative totals
- Department and group management

### NFC Fair System (Târg Final de Sezon)
End-of-season marketplace where children spend their accumulated season points using physical NFC cards.

```
[Physical NFC Card]
        │
[USB NFC Reader - PC/SC]
        │  javax.smartcardio
[nfc-bridge.jar — runs locally]
        │  WebSocket  ws://localhost:7000
[React Frontend — browser]
        │  HTTP REST
[Spring Boot — Ubuntu Server]
        │
[MySQL — deducts points from Child.seasonPoints]
```

- Each card stores only the hardware UID — no data written to cards
- **Admin flow:** associates card UID to child in Control Center
- **Seller flow** (phone): creates purchase receipts (Bons) for children
- **Accountant flow** (laptop + NFC reader): child taps card → pending receipt appears → approves → points deducted automatically
- Products managed via admin panel with categories and point prices

### Olimpiada Awana
Independent scoring system for the annual club competition. Two arbiters score four fixed teams across multiple rounds; results are compared at the end.

- **4 fixed teams:** ROSU, GALBEN, ALBASTRU, VERDE
- **Scoring:** 1st = 1500 pts · 2nd = 1000 pts · 3rd = 500 pts · 4th = 300 pts (rounds can count double)
- **Session codes** (e.g. `OLM26`) allow arbiters to join without a full account
- **Comparison view:** totals per team per arbiter, differences highlighted, final ranking with medals
- Fully isolated from club data (no interaction with Child, Score, etc.)

### Role-Based Access
Enforced on the server: every request carries a signed JWT, and the role comes from the token, never from the client.
Leaders sign in with Google (invite-only: an account the director has not added is refused); children use passwords.

| Role | Access |
|---|---|
| Child | Own profile and progress only |
| Leader | Scoring, shop receipts, Olimpiada scoring |
| Director / Coordinator | + Product and Olimpiada session management (admin PIN); the main admin account also gets the Control Center |
| Guest Arbiter | Olimpiada scoring only (via session code + name) |

---

## Architecture

```
Client (React + Vite)
        │
        │  HTTPS · REST · Authorization: Bearer <JWT>
        ▼
Spring Boot (Java 17)
  ├── Security/     JWT filter, access rules per role, admin PIN
  ├── Controller/   HTTP only: validated request DTOs in, response DTOs out
  ├── Service/      business rules and transactions
  ├── Exception/    errors as RFC 7807 problem responses
  ├── Model/        JPA entities (Child, Score, Meeting, Bon, Product, OlimpiadaSession, ...)
  └── Repository/   Spring Data JPA
        │
        │  JDBC
        ▼
MySQL 8 — schema versioned with Flyway (src/main/resources/db/migration)
        │
Hosted on Ubuntu Linux, run by systemd
```

For NFC features, a lightweight bridge JAR runs locally on the accountant's machine and communicates with the browser via WebSocket:

```
nfc-bridge.jar (local)  ──WebSocket──►  Browser  ──REST──►  Server
```

---

## Running Locally

Requirements: Java 17+, Node 22 (see `Frontend/.nvmrc`), MySQL 8.

```bash
# 1. Configuration: copy the template and fill in your local values
cp .env.example .env

# 2. Backend on http://localhost:8080 — Flyway creates the schema on an empty database
./mvnw spring-boot:run

# 3. Frontend on http://localhost:5173 — /api is proxied to the backend
cd Frontend
npm ci
npm run dev
```

`.env.example` lists every setting with an explanation. The frontend reads its API address from
`VITE_API_URL` (`Frontend/.env.development`, `Frontend/.env.production`).

### Tests

```bash
./mvnw verify                      # backend: unit, security and migration tests
cd Frontend && npm run lint        # frontend
```

`DatabaseMigrationTest` starts MySQL 8 in Docker (Testcontainers), runs every migration and checks
the result against the JPA entities; it is skipped when Docker is not available. CI runs everything
on every push.

### Changing the database schema

Add a new file `src/main/resources/db/migration/V<n>__what_it_does.sql`. Never edit a migration
that has already run: Flyway keeps a checksum of each one. Hibernate only validates the schema
(`ddl-auto=validate`) and refuses to start if it does not match the entities.

---

## Deployment & Operations

Production runs as the systemd service `awanabetania`. A deploy is one command on the server:

```bash
cd /var/www/html && ./deploy.sh
```

It pulls `main`, backs up the database, builds the frontend and the backend, and starts the new
version once without the web server to apply migrations and validate the schema while the site is
still running the old version. Only then does it restart the service, wait for
`/actuator/health`, and confirm through `/actuator/info` that the commit it just built is the one
answering. If the new version does not come up, it restarts the previous one automatically.

Backups, restores, rollbacks and the server checklist are described in
[docs/OPERATIONS.md](docs/OPERATIONS.md) (Romanian).

---

## NFC Bridge

The NFC bridge is a separate Maven project under `nfc-bridge/`.

```bash
# Build
./mvnw -f nfc-bridge/pom.xml package

# Run with physical reader
java -jar nfc-bridge/target/nfc-bridge.jar

# Run in test mode (manual UID input, no hardware required)
java -jar nfc-bridge/target/nfc-bridge.jar --test
```

- Polls PC/SC reader every 300ms
- Reads card UID via APDU `FF CA 00 00 00`
- Broadcasts `{"uid":"A1B2C3D4"}` over WebSocket to all connected browsers
- Browser auto-fills the active UID field

---

## Status

🟢 **Live in production** — actively maintained and used by 100+ users.

---

## Author

**Raul Macovei**  
CS Student · West University of Timișoara  
[LinkedIn](https://www.linkedin.com/in/raul-macovei-b56a38332) · [GitHub](https://github.com/raul-dan23)
