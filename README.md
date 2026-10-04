# Pit Stop — Vehicle Service Management System

> Standalone Java desktop application for garage service operations.

---

## Overview

VSMS is a Swing-based desktop application that allows garage staff to:

- Register customers and their vehicles.
- Book service appointments and allocate technicians.
- Track job cards through their full lifecycle (`BOOKED → IN_PROGRESS → COMPLETED → DELIVERED`).
- Generate itemised invoices covering labour and spare parts.
- View complete vehicle service history.

All data persistence is handled via **JDBC** (`PreparedStatement`) connected to an **Oracle Database Free** instance running in Docker.

---

## Prerequisites

| Requirement | Minimum Version |
|---|---|
| JDK | 17 (LTS) |
| Apache Maven | 3.8+ |
| Docker Desktop | 4.x |
| Docker Compose | v2 (bundled with Docker Desktop) |
| RAM (for Oracle container) | 2 GB free |

---

## Quick Start

### Step 1 — Clone and configure credentials

```bash
git clone <repository-url>
cd Java-Mini-Project
cp .env.example .env
# Edit .env with your preferred passwords (or leave defaults for local dev)
```

### Step 2 — Start the Oracle Database container

```bash
docker compose up -d
```

Wait for the database to be ready (first boot takes ~2 minutes):

```bash
docker compose logs -f oracle-db
# Watch for the line: "DATABASE IS READY TO USE!"
```

The `db/schema.sql` script is automatically executed on first startup, creating all tables and seed data inside the `FREEPDB1` pluggable database under the `garage_user` schema.

### Step 3 — Compile and run the application

```bash
# Compile
.\mvnw.cmd compile

# Run via exec plugin (reads .env automatically)
.\mvnw.cmd exec:java

# Or build a runnable jar
.\mvnw.cmd package
java -jar target/vehicle-service-mgmt.jar
```

---

## Project Structure

```
.
├── .env.example          ← Copy to .env; fill in DB credentials
├── .gitignore
├── AGENTS.md             ← Coding-agent operating protocol
├── docker-compose.yml    ← Oracle DB container definition
├── pom.xml               ← Maven build descriptor
├── db/
│   ├── 01_init.sh        ← Shell script executed by Docker to run schema.sql
│   └── schema.sql        ← Oracle DDL + seed data (auto-run on container init)
├── docs/
│   ├── ARCHITECTURE.md   ← MVC/DAO layered design & data-flow diagrams
│   ├── DATABASE.md       ← Schema reference + 1NF/2NF/3NF normalization
│   ├── TECH_STACK.md     ← Hardware/software requirement tables
│   └── WORK_SPLIT.md     ← Phased execution plan with acceptance criteria
└── src/
    └── main/java/com/garage/
        ├── model/        ← Plain Java POJOs
        ├── dao/          ← Pure JDBC data access objects
        ├── util/         ← DBConnection singleton
        ├── service/      ← Business logic + transaction management
        └── ui/           ← Swing frames, panels, dialogs, Fluent UI 2 theme
```

---

## Documentation

| Document | Purpose |
|---|---|
| [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) | System architecture, MVC/DAO breakdown, data-flow |
| [`docs/DATABASE.md`](docs/DATABASE.md) | Relational schema, ER relationships, normalization proof |
| [`docs/TECH_STACK.md`](docs/TECH_STACK.md) | Technology choices, hardware/software requirements |
| [`docs/WORK_SPLIT.md`](docs/WORK_SPLIT.md) | Phased implementation plan with contracts per phase |
| [`AGENTS.md`](AGENTS.md) | Rules for coding agents working on subsequent phases |

---

## Connection Details (local dev defaults)

| Property | Value |
|---|---|
| JDBC URL | `jdbc:oracle:thin:@localhost:1521/FREEPDB1` |
| Username | `garage_user` |
| Password | (see `.env`) |
| Container name | `vsms_oracle` |

---


