# TASK: Phase 0 — Foundation Setup, Documentation Blueprints & Database Provisioning

## 1. ACADEMIC COURSE CONTEXT & EXACT REQUIREMENTS
You are scaffolding a repository for an academic lab project combining two university laboratory papers:
- **UIT3361**: Object-Oriented Programming Using Java
- **UIT3311**: Database Technology Laboratory
- **Assignment**: Exercise 10 — Standalone Java Application with Database Connectivity
- **Reference Syllabus Topics**:
  - Java Application Development Unit: AWT, Swing, Console Applications, GUI Applications (AWT, Swing), Database Applications (JDBC).
- **Final Evaluation Deliverables**:
  - Live application demonstration before the Model Examination.
  - Comprehensive project report covering:
    1. System Requirements (hardware and software specifications)
    2. System Design (front-end and back-end architecture)
    3. Core Modules Implemented
    4. Code Snippets highlighting key functionality
    5. Screenshots of the working application
    6. GitHub Repository Link for source code

---

## 2. PROBLEM STATEMENT
**Vehicle Service Management System**:

Develop a standalone Java application for garages where:
- Customers/Service Advisors can register vehicles and book servicing appointments.
- Technicians/Advisors can track job card status across operational stages (Booked, In-Progress, Completed, Delivered).
- Billing computes labor and spare parts costs, generating an itemized invoice.
- Java Swing serves as the desktop booking, monitoring, and reporting interface.
- JDBC manages vehicle records, transactions, and service entries via `PreparedStatement`.
- Oracle Database (running in a local Docker container) stores relational service records and vehicle history.
- **Mandatory Academic Requirement**: Perform formal relational database normalization (1NF, 2NF, 3NF) and document clear academic justifications.

---

## 3. CORE ARCHITECTURAL CONSTRAINTS & CODING PHILOSOPHY
- **No Over-Engineering**: Do NOT use Spring, Spring Boot, Hibernate, JPA, or complex reflection/bytecode frameworks. The evaluation is on foundational OOP concepts and core JDBC.
- **Back-End Pattern**: Standard Desktop MVC/DAO structure:
  - `model/`: Plain Java POJOs (getters, setters, constructors).
  - `dao/`: Pure JDBC using `java.sql.*` interfaces (`Connection`, `PreparedStatement`, `ResultSet`). Explicit resource management using `try-with-resources`.
  - `util/`: Singleton `DBConnection.java` reading connection properties securely from `.env` or a local config.
  - `controller/` or `service/`: Lightweight business logic and transaction demarcation (`commit`/`rollback`).
  - `ui/`: Swing views (`JFrame`, `JDialog`, `JPanel`, `JTable`).
- **Front-End & UI Standards**:
  - **No Bizarre "AI UI"**: Avoid messy custom-painted gradients or absolute/null layouts (`setBounds`).
  - Use native **System Look and Feel** (`UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())`).
  - Use standard, robust Swing layout managers (`BorderLayout`, `GridBagLayout`, `FlowLayout`, nested `JPanel` with consistent padding/borders via `BorderFactory.createEmptyBorder(...)`).
  - Clean, readable table rendering (`JTable` inside `JScrollPane`) and standard dialogs (`JOptionPane`).
  - Strict UI Threading: Always launch and mutate UI components on the Event Dispatch Thread using `SwingUtilities.invokeLater`.
- **Build System**: Maven (`pom.xml`) managing:
  - JDK 17 or 21 baseline.
  - Oracle JDBC Driver: `com.oracle.database.jdbc:ojdbc11`.
  - `dotenv-java` (for clean `.env` loading, keeping credentials out of code).
- My Exact Syllabus for reference:
  - GUIs AWET and Applets, Comnsole Aplications, GUI Ap;lications (AWT, Swing, JavaFX), Database Applications (JDBC).
---

## 4. REPOSITORY STRUCTURE RULES
Keep root clean and place architectural blueprints in `docs/`:

.
├── .env.example
├── .gitignore
├── AGENTS.md
├── README.md
├── docker-compose.yml
├── pom.xml
├── db/
│   └── schema.sql
├── docs/
│   ├── ARCHITECTURE.md
│   ├── DATABASE.md
│   ├── TECH_STACK.md
│   └── WORK_SPLIT.md
└── src/
 

---

## 5. REQUIRED DELIVERABLES TO GENERATE (PHASE 0)

### 1. `docker-compose.yml`
- Containerized Oracle Database using `gvenzl/oracle-free:slim` (lightweight Oracle XE/Free image).
- Map standard ports (e.g., `1521:1521`).
- Configure persistent volume mapping, database name (`FREE` or `FREEPDB1`), and default credentials.
- Automate execution of `db/schema.sql` on container startup via entrypoint initialization scripts (`/container-entrypoint-initdb.d`).

### 2. `db/schema.sql`
- Complete, runnable DDL for Oracle Database:
  - Tables: `CUSTOMERS`, `VEHICLES`, `SERVICES`, `JOB_CARDS`, `SERVICE_ITEMS` / `PARTS`, `INVOICES`.
  - Primary keys (using `IDENTITY` columns or Oracle sequences/triggers).
  - Foreign keys with referential integrity.
  - Appropriate check constraints (e.g., job statuses: `'BOOKED'`, `'IN_PROGRESS'`, `'COMPLETED'`, `'DELIVERED'`).
- Realistic seed data for immediate testing and live demo readiness.

### 3. `.env.example` & `.gitignore`
- `.env.example`: `DB_URL=jdbc:oracle:thin:@localhost:1521/FREEPDB1`, `DB_USER=...`, `DB_PASSWORD=...`.
- `.gitignore`: Standard Java, Maven, IDE (`.idea`, `.vscode`), OS files, and `.env`.

### 4. `pom.xml`
- Minimal, clean Maven configuration with `ojdbc11`, `dotenv-java`, compiler plugin targeting Java 17+, and `exec-maven-plugin` configured to run the main class.

### 5. `AGENTS.md` (Root)
- Context and operating protocol for coding agents working on subsequent phases.
- Rules on JDBC code style, connection safety, Swing threading rules, zero over-engineering enforcement, and branch/testing workflows.

### 6. `README.md` (Root)
- Brief project overview, prerequisites, and quick-start instructions:
  - Starting the Oracle Docker container.
  - Compiling and executing the application via Maven.

### 7. Documentation in `docs/`
- **`docs/TECH_STACK.md`**:
  - Languages, tools, drivers, and runtime specifications.
  - Formatted hardware and software requirement tables aligning with academic report submission standards.
- **`docs/ARCHITECTURE.md`**:
  - MVC/DAO layered architecture breakdown with clean ASCII/textual data-flow diagrams.
  - Package structure and component responsibilities.
- **`docs/DATABASE.md`**:
  - Relational schema descriptions and ER relationship mapping.
  - **Formal Normalization Justification**: Step-by-step breakdown from Unnormalized Form (UNF) through 1NF, 2NF, and 3NF/BCNF, with explicit academic justifications for why each table is in 3NF.
- **`docs/WORK_SPLIT.md`**:
  - **Standardized Agent Trigger Contract**: Include a short section at the top detailing the exact prompt template to trigger each phase (e.g., `"Read AGENTS.md, docs/ARCHITECTURE.md, and execute Phase [X] from docs/WORK_SPLIT.md completely."`).
  - **SRS Feature Catalog (Baseline Requirements)**:
    - Module 1: Customer & Vehicle Registration / Lookup.
    - Module 2: Appointment Booking & Service Slot Scheduling.
    - Module 3: Job Card Tracking (Lifecycle: `BOOKED` -> `IN_PROGRESS` -> `COMPLETED` -> `DELIVERED`).
    - Module 4: Billing & Invoicing (Labor + Parts breakdown, total calculation, payment status).
    - Module 5: Service History & Vehicle Past Records Lookup.
  - **Balanced Phased Execution Plan (Phase 1 to Phase N)**:
    - Structure each phase so it can be completed cleanly in a single session without token limits or partial state.
    - **Mandatory Anatomy of Every Phase**:
      1. **Objective & Scope**: What is built in this phase.
      2. **Files to Create / Modify**: Explicit package paths (e.g., `src/main/java/com/garage/dao/CustomerDAO.java`).
      3. **Step-by-Step Implementation Flow**: Step 1 (Model) -> Step 2 (DAO with PreparedStatement) -> Step 3 (Controller/Service) -> Step 4 (UI View/Panel).
      4. **Verification & Acceptance Criteria**: Specific terminal command or smoke test harness to run (e.g., console test inserting a record and querying it back) plus manual Swing UI check steps.
      5. **Definition of Done**: Clear checkpoint before the agent marks the phase complete.
---

## 6. EXECUTION INSTRUCTION
Generate all listed files and directories with complete, syntactically correct, and immediately usable contents. Do not output placeholders, omitted sections, or deferred TODOs.
