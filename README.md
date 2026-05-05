# Awana Betania – Branch Management Web Application

> A full-stack web application built for [Awana Romania](https://awanabetania.eu), a non-profit Christian youth organization operating multiple branches across Romania.  
> **Live at:** [awanabetania.eu](https://awanabetania.eu)

---

## Overview

This application was designed and built from scratch to support the day-to-day operations of the Timișoara branch. It is actively used by **100+ users** and runs in a self-hosted production environment.

The system handles member management, operational workflows, and internal coordination — replacing manual processes with a centralized, reliable platform.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Java · Spring Boot · REST API |
| Frontend | React · Responsive UI |
| Database | MySQL |
| Server | Ubuntu Linux (self-hosted) |
| Deployment | Git hooks · Shell scripts · Auto-deploy pipeline |

---

## Features

- **Member management** – registration, roles, and assignment tracking
- **RESTful API** – clean client-server communication between React frontend and Spring Boot backend
- **Responsive UI** – accessible on both desktop and mobile for non-technical users
- **Auto-deployment** – Git-integrated deployment scripts; pushing to main triggers an automatic server update
- **Production reliability** – continuous availability with ongoing maintenance and user support

---

## Architecture

```
Client (React)
     │
     │  HTTP / REST
     ▼
Spring Boot (Java)
     │
     │  JDBC
     ▼
MySQL Database
     │
Hosted on Ubuntu Linux Server
Auto-deployed via Git hooks
```

---

## Deployment

The application is self-hosted on a Linux server with an automated deployment pipeline:

1. Code is pushed to the main branch
2. A Git hook triggers a shell script on the server
3. The script pulls the latest changes, rebuilds, and restarts the service
4. Zero manual intervention required for routine deployments

---

## Status

🟢 **Live in production** — actively maintained and used by 100+ users.

---

## Author

**Raul Macovei**  
CS Student · West University of Timișoara  
[LinkedIn](https://www.linkedin.com/in/raul-macovei-b56a38332) · [GitHub](https://github.com/raul-dan23)
