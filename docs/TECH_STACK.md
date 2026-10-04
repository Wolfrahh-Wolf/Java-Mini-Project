# Technology Stack — Pit Stop (Vehicle Service Management System)

---

## 1. Language & Runtime

| Component | Technology | Version | Justification |
|---|---|---|---|
| Programming Language | Java (SE) | 17 LTS | LTS release ensures stability; modern language features (records, sealed classes available but not required) |
| JDK Distribution | Oracle JDK / Eclipse Temurin | 17.x | Free and production-grade |
| UI Toolkit | Java Swing + FlatLaf | Bundled + 3.5.1 | Swing for structure; FlatLaf for modern theming and Fluent UI 2 Dark support |
| Build Tool | Apache Maven | 3.8+ | Standard dependency management; exec plugin for run-without-IDE workflow |
| JDBC Driver | Oracle JDBC (ojdbc11) | 23.4.0.24.05 | Thin driver for Oracle 23c; available on Maven Central |
| `.env` loader | dotenv-java (cdimascio) | 3.0.0 | Secure credential loading; keeps secrets out of source code |

---

## 2. Database

| Component | Technology | Version | Justification |
|---|---|---|---|
| RDBMS | Oracle Database Free | 23c (23.4) | Syllabus-mandated Oracle; Free edition is production-identical without licence cost |
| Container Image | `gvenzl/oracle-free:slim` | Latest slim | Smallest verified Oracle XE/Free image; auto-initialises from mounted SQL scripts |
| Container Runtime | Docker + Docker Compose | Docker ≥ 24, Compose v2 | Reproducible local environment; eliminates manual Oracle install |
| Persistence | Named Docker Volume (`oracle_data`) | — | Survives container restarts; data not lost on `docker compose down` |

---

## 3. Development Tools

| Tool | Purpose | Version |
|---|---|---|
| IntelliJ IDEA Community / NetBeans | Primary IDE | Latest |
| Git | Version control | 2.x |
| GitHub | Remote repository + submission link | — |
| DBeaver / SQL*Plus | Database inspection and query testing | Any |
| Docker Desktop | Container management UI | 4.x |

---

## 4. Hardware Requirements (Minimum)

| Resource | Minimum Specification | Recommended |
|---|---|---|
| CPU | Dual-core 2.0 GHz (x86-64) | Quad-core 2.5 GHz |
| RAM | 4 GB total (2 GB free for Oracle container) | 8 GB |
| Disk Space | 5 GB free (Oracle image ~2 GB + data) | 10 GB SSD |
| OS | Windows 10 / Ubuntu 20.04 / macOS 12 | Windows 11 / Ubuntu 22.04 |
| Display | 1280 × 768 | 1920 × 1080 |
| Network | Required for initial Docker image pull | Broadband |

---

## 5. Software Requirements

| Software | Version | Source |
|---|---|---|
| JDK 17 (Temurin) | 17.x | [adoptium.net](https://adoptium.net) |
| Apache Maven | 3.8+ | [maven.apache.org](https://maven.apache.org) |
| Docker Desktop | 4.x | [docker.com](https://www.docker.com) |
| Git | 2.x | [git-scm.com](https://git-scm.com) |
| IntelliJ IDEA Community | 2023+ | [jetbrains.com](https://www.jetbrains.com) |

---

## 6. Dependency Versions (pom.xml)

| Artifact ID | Group ID | Version | Scope |
|---|---|---|---|
| `ojdbc11` | `com.oracle.database.jdbc` | `23.4.0.24.05` | `compile` |
| `dotenv-java` | `io.github.cdimascio` | `3.0.0` | `compile` |

---

## 7. Architecture Decision Record (ADR)

### ADR-001: Swing over JavaFX
- **Decision**: Use Java Swing with FlatLaf.
- **Rationale**: Mature, built-in to JDK; FlatLaf (specifically FlatDarkLaf) provides modern dark theme styling and Fluent UI 2 design tokens without the overhead of JavaFX.

### ADR-002: No ORM
- **Decision**: Raw JDBC with `PreparedStatement`.
- **Rationale**: Provides full control over SQL execution, query plans, and transaction management; avoids ORM boilerplate and hidden N+1 queries.

### ADR-003: Oracle Docker over Local Install
- **Decision**: Use `gvenzl/oracle-free:slim` Docker image.
- **Rationale**: Eliminates 30-minute manual Oracle installation; reproducible across team machines; automatically seeds schema on first boot.

### ADR-004: dotenv-java over `Properties` file
- **Decision**: Use `.env` + `dotenv-java`.
- **Rationale**: Industry-standard 12-factor app approach; `.env` excluded from Git preventing credential leaks; simpler than encrypted `Properties` files.
