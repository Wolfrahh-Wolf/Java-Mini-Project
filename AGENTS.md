# AGENTS.md — Coding Agent Operating Protocol
## Vehicle Service Management System (VSMS)

> **Read this file completely before writing a single line of code.**
> These rules are absolute and override any "best practice" the model believes it knows.

---

## 1. Project Identity

| Field | Value |
|---|---|
| Application | Vehicle Service Management System (VSMS) |
| Course | UIT3361 OOP Java + UIT3311 Database Technology Lab |
| Assignment | Exercise 10 — Standalone Java App with DB Connectivity |
| Build system | Maven 3.x |
| JDK target | Java 17 (LTS) |
| Database | Oracle Database Free (23c) via Docker |
| UI toolkit | Java Swing |
| JDBC driver | `ojdbc11` (thin) |

---

## 2. Architecture — Non-Negotiable Package Layout

```
src/main/java/com/garage/
├── model/          # Plain POJOs only — fields, constructors, getters, setters
├── dao/            # All SQL via PreparedStatement + ResultSet; one class per entity
├── util/           # DBConnection.java (singleton), any shared helpers
├── service/        # Business logic + transaction demarcation (commit/rollback)
└── ui/             # Swing views: JFrame, JDialog, JPanel subclasses
```

**Do NOT** create packages or classes outside this structure without explicit user instruction.

---

## 3. JDBC Coding Rules (Strict)

### 3.1 Always use `try-with-resources`
```java
// CORRECT
try (Connection conn = DBConnection.getConnection();
     PreparedStatement ps = conn.prepareStatement(SQL)) {
    ps.setString(1, value);
    try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) { ... }
    }
} catch (SQLException e) {
    throw new RuntimeException("DB error: " + e.getMessage(), e);
}

// WRONG — never do this
Connection conn = DBConnection.getConnection();
PreparedStatement ps = conn.prepareStatement(SQL);
```

### 3.2 Always use `PreparedStatement` — never `Statement` with concatenation
```java
// CORRECT
ps.setString(1, customer.getName());

// WRONG — SQL injection risk
stmt.executeQuery("SELECT * FROM CUSTOMERS WHERE NAME = '" + name + "'");
```

### 3.3 Transaction demarcation belongs in the `service/` layer
```java
// service method
conn.setAutoCommit(false);
try {
    customerDao.insert(conn, customer);
    vehicleDao.insert(conn, vehicle);
    conn.commit();
} catch (SQLException e) {
    conn.rollback();
    throw e;
}
```

### 3.4 DBConnection singleton — must read from `.env`
- Use `io.github.cdimascio.dotenv.Dotenv` to load `.env`.
- Never hard-code credentials in source files.
- The singleton must be thread-safe (use `synchronized` or initialise in static block).

### 3.5 ResultSet column references
- Always reference columns by **name**, not ordinal index: `rs.getString("CUSTOMER_NAME")`.

---

## 4. Swing / UI Threading Rules (Strict)

### 4.1 Always launch on the Event Dispatch Thread (EDT)
```java
// CORRECT — in main()
SwingUtilities.invokeLater(() -> new MainFrame().setVisible(true));

// WRONG
new MainFrame().setVisible(true); // called from main thread
```

### 4.2 Long-running DB calls must use `SwingWorker`
```java
new SwingWorker<List<Customer>, Void>() {
    @Override protected List<Customer> doInBackground() throws Exception {
        return customerDao.findAll();
    }
    @Override protected void done() {
        try { tableModel.setData(get()); }
        catch (Exception e) { JOptionPane.showMessageDialog(...); }
    }
}.execute();
```

### 4.3 Layout managers — absolutely required
- Use `BorderLayout`, `GridBagLayout`, `FlowLayout`, and nested `JPanel` compositions.
- **Never** use `null` layout or `setBounds(...)` for positioning.
- Apply consistent padding via `BorderFactory.createEmptyBorder(10, 10, 10, 10)`.

### 4.4 Look and Feel — system native only
```java
UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
```
Called once, before any Swing component is instantiated.

### 4.5 Dialogs
- Validation errors → `JOptionPane.showMessageDialog(parent, msg, "Validation Error", JOptionPane.ERROR_MESSAGE)`.
- Confirmations → `JOptionPane.showConfirmDialog(...)`.
- Never create custom modal windows for simple messages.

---

## 5. Zero Over-Engineering Mandate

The following are **banned** from this codebase:

| Banned Technology | Reason |
|---|---|
| Spring / Spring Boot | Not in academic scope |
| Hibernate / JPA / any ORM | Violates foundational JDBC requirement |
| Lombok | Not introduced in syllabus |
| Reflection / bytecode manipulation | Out of scope |
| JavaFX (unless specifically requested) | Swing is the required UI toolkit |
| Any build tool other than Maven | Scope constraint |

---

## 6. SQL & Oracle-Specific Rules

- All DDL uses **Oracle SQL syntax** (not ANSI generic or MySQL syntax).
- Sequences named `<TABLE>_SEQ`; triggers named `<TABLE>_TRG` for auto-increment PKs.
- All table/column names in **UPPERCASE** in SQL (Oracle convention).
- Use `VARCHAR2` (not `VARCHAR`), `NUMBER` (not `INT`), `DATE` or `TIMESTAMP`.
- Constraint names follow pattern: `PK_<TABLE>`, `FK_<TABLE>_<REF>`, `CK_<TABLE>_<COLUMN>`, `UQ_<TABLE>_<COLUMN>`.

---

## 7. Phase Execution Protocol

When a phase prompt is given:

1. **Read** `AGENTS.md` (this file) fully.
2. **Read** `docs/ARCHITECTURE.md` for the layered design.
3. **Read** the specific phase entry in `docs/WORK_SPLIT.md`.
4. **Implement** every file listed in "Files to Create / Modify" for that phase.
5. **Verify** by running the acceptance criteria command listed in the phase.
6. **Do NOT** start the next phase unless the current phase's Definition of Done is met.

### Standard Phase Trigger Prompt
```
Read AGENTS.md, docs/ARCHITECTURE.md, and execute Phase [X] from docs/WORK_SPLIT.md completely.
```

---

## 8. Code Style & Quality

- **Naming**: `camelCase` for variables/methods, `PascalCase` for classes, `UPPER_SNAKE_CASE` for constants.
- **Javadoc**: Every public class and public method must have a brief Javadoc comment.
- **No raw types**: Always parameterise generics (`List<Customer>`, not `List`).
- **Error messages**: Must be descriptive — include the operation and entity involved.
- **No `System.exit()`** inside DAO or service layers — throw exceptions upward.
- **No `printStackTrace()`** in production paths — log or re-throw with context.

---

## 9. File Header Template

Every new `.java` source file must begin with:
```java
/**
 * [ClassName] — [one-line description]
 *
 * Course  : UIT3361 OOP Java / UIT3311 Database Technology Lab
 * Project : Vehicle Service Management System
 */
package com.garage.[layer];
```

---

## 10. Branch & Version Control Guidelines

- `main` branch: stable, demo-ready code only.
- Feature work: `feature/<phase-N>-<short-description>` branches.
- Commit message format: `[Phase N] Short imperative description`.
- Never commit `.env` (it is in `.gitignore`).
- Never commit `target/` output.
