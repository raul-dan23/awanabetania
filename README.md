# Awana Betania – Branch Management Web Application

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
| Backend | Java 17 · Spring Boot 3.x · JPA/Hibernate · REST API |
| Frontend | React · Vite · Responsive UI |
| Database | MySQL |
| NFC Bridge | Java (javax.smartcardio) · WebSocket · PC/SC |
| Server | Ubuntu Linux (self-hosted) |
| Deployment | Git hooks · Shell scripts · Auto-deploy pipeline |

---

## Features

### Member & Score Management
- Registration and role assignment (children, leaders, directors, coordinators)
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
- **Scoring:** 1st = 1000 pts · 2nd = 500 pts · 3rd = 300 pts · 4th = 100 pts
- **Session codes** (e.g. `OLM26`) allow arbiters to join without a full account
- **Comparison view:** totals per team per arbiter, differences highlighted, final ranking with medals
- Fully isolated from club data (no interaction with Child, Score, etc.)

### Role-Based Access
| Role | Access |
|---|---|
| Child | Personal score view |
| Leader | Scoring, shop receipts, Olimpiada scoring |
| Director / Coordinator | + Product management, session management, admin PIN features |
| User ID=1 | + Control Center (NFC card association) |
| Guest Arbiter | Olimpiada scoring only (via session code + name) |

---

## Architecture

```
Client (React + Vite)
        │
        │  HTTP REST
        ▼
Spring Boot (Java 17)
  ├── Controller/   REST endpoints
  ├── Model/        JPA entities (Child, Score, Meeting, Bon, Product, OlimpiadaSession, ...)
  └── Repository/   Spring Data JPA
        │
        │  JDBC
        ▼
MySQL Database
        │
Hosted on Ubuntu Linux (self-hosted)
Auto-deployed via Git hooks
```

For NFC features, a lightweight bridge JAR runs locally on the accountant's machine and communicates with the browser via WebSocket:

```
nfc-bridge.jar (local)  ──WebSocket──►  Browser  ──REST──►  Server
```

---

## NFC Bridge

The NFC bridge is a separate Maven project under `nfc-bridge/`.

```bash
# Build
cd nfc-bridge && mvn package

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

## Deployment

The frontend is bundled into the Spring Boot JAR for single-artifact deployment:

```bash
cd Frontend && npm run build
cp -r dist/* ../src/main/resources/static/
cd .. && mvn package -DskipTests
# Upload target/*.jar to server and restart Spring Boot
```

The server runs an automated pipeline:
1. Push to main branch
2. Git hook triggers a shell script on the server
3. Script pulls, rebuilds, and restarts the service
4. Zero manual intervention required

---

## Status

🟢 **Live in production** — actively maintained and used by 100+ users.

---

## Author

**Raul Macovei**  
CS Student · West University of Timișoara  
[LinkedIn](https://www.linkedin.com/in/raul-macovei-b56a38332) · [GitHub](https://github.com/raul-dan23)
