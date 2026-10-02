# Work Split & Phased Execution Plan
## Vehicle Service Management System (VSMS)

---

## PART A — Agent Trigger Protocol

### Standard Phase Trigger Prompt

To execute any phase, send this exact prompt to the coding agent:

```
Read AGENTS.md, docs/ARCHITECTURE.md, and execute Phase [X] from docs/WORK_SPLIT.md completely.
```

Replace `[X]` with the phase number (1, 2, 3, 4, or 5).

### Prerequisites Checklist (Before Any Phase)

- [ ] Oracle container is running: `docker compose up -d`
- [ ] `DATABASE IS READY TO USE!` confirmed in container logs
- [ ] `.env` file exists and has valid credentials
- [ ] `mvn compile` exits with `BUILD SUCCESS`
- [ ] Previous phase's Definition of Done is confirmed

---

## PART B — SRS Feature Catalog (Baseline Requirements)

| Module | Feature | Priority |
|---|---|---|
| **M1** Customer & Vehicle | Register new customer | P0 |
| **M1** Customer & Vehicle | Search customer by phone | P0 |
| **M1** Customer & Vehicle | Register vehicle under customer | P0 |
| **M1** Customer & Vehicle | View all vehicles for a customer | P0 |
| **M2** Appointment Booking | Book service appointment | P0 |
| **M2** Appointment Booking | Select service type from catalogue | P0 |
| **M2** Appointment Booking | View upcoming appointments (BOOKED status) | P1 |
| **M3** Job Card Tracking | View all job cards by status | P0 |
| **M3** Job Card Tracking | Advance job card status (BOOKED→IN_PROGRESS→…) | P0 |
| **M3** Job Card Tracking | Assign technician to job card | P1 |
| **M3** Job Card Tracking | Record labour hours and remarks | P1 |
| **M4** Billing & Invoicing | Auto-generate invoice from completed job card | P0 |
| **M4** Billing & Invoicing | Add spare parts as invoice line items | P0 |
| **M4** Billing & Invoicing | View itemised invoice with labour + parts breakdown | P0 |
| **M4** Billing & Invoicing | Mark invoice as paid (with payment mode) | P0 |
| **M5** Service History | Search vehicle service history by registration | P0 |
| **M5** Service History | View all past job cards and invoices for a vehicle | P0 |

**Priority:** P0 = Must-have for demo. P1 = Should-have; implement if time allows.

---

## PART C — Phased Execution Plan

---

## Phase 1 — Project Infrastructure + Customer & Vehicle Module

### Objective & Scope
Establish the foundational Java infrastructure (DBConnection, base POJOs, first DAO pair) and implement the first complete end-to-end vertical slice: registering a customer and their vehicle, and searching for them. By the end of this phase, the application can connect to Oracle, persist data, and display it in a basic Swing frame.

### Files to Create / Modify

```
src/main/java/com/garage/
├── util/
│   └── DBConnection.java
├── model/
│   ├── Customer.java
│   └── Vehicle.java
├── dao/
│   ├── CustomerDAO.java
│   └── VehicleDAO.java
├── service/
│   ├── CustomerService.java
│   └── VehicleService.java
└── ui/
    ├── MainApp.java
    ├── MainFrame.java
    ├── CustomerPanel.java
    └── VehiclePanel.java
```

### Step-by-Step Implementation Flow

**Step 1 — `util/DBConnection.java`**
- Load `.env` using `Dotenv.load()`.
- Create a `private static final Connection INSTANCE` in a `static {}` block.
- Expose `public static Connection getConnection()`.
- Throw `ExceptionInInitializerError` if connection fails (forces early failure).

**Step 2 — `model/Customer.java` and `model/Vehicle.java`**
- Plain POJO fields matching schema columns exactly.
- `int`/`long` for IDs, `String` for text, `double` for money, `java.time.LocalDateTime` for timestamps.
- Full constructor, no-arg constructor, getters, setters, `toString()`.

**Step 3 — `dao/CustomerDAO.java`**
- `insert(Connection conn, Customer c) : int` — INSERT with PreparedStatement, return generated key via `RETURN_GENERATED_KEYS`.
- `findById(Connection conn, int id) : Optional<Customer>`.
- `findByPhone(Connection conn, String phone) : Optional<Customer>`.
- `findAll(Connection conn) : List<Customer>`.

**Step 4 — `dao/VehicleDAO.java`**
- `insert(Connection conn, Vehicle v) : int`.
- `findByCustomerId(Connection conn, int customerId) : List<Vehicle>`.
- `findByRegistrationNo(Connection conn, String regNo) : Optional<Vehicle>`.

**Step 5 — `service/CustomerService.java` and `service/VehicleService.java`**
- `CustomerService.registerCustomer(Customer c)` — validates phone not blank, calls `CustomerDAO.insert()`.
- `CustomerService.findByPhone(String phone)` — delegates to DAO.
- `VehicleService.registerVehicle(Vehicle v)` — validates required fields, calls `VehicleDAO.insert()`.
- `VehicleService.getVehiclesForCustomer(int customerId)` — delegates to DAO.

**Step 6 — `ui/MainApp.java`**
- `main()`: Set system L&F, then `SwingUtilities.invokeLater(() -> new MainFrame().setVisible(true))`.

**Step 7 — `ui/MainFrame.java`**
- `JFrame` subclass with `JTabbedPane`.
- Add tabs: "Customers", "Vehicles" (others added in later phases).

**Step 8 — `ui/CustomerPanel.java`**
- `JPanel` with `GridBagLayout` registration form: Name, Phone, Email, Address fields + "Register" button.
- `JTable` (inside `JScrollPane`) showing all customers, refreshed after registration.
- Search field + "Search by Phone" button, highlights matching row.
- DB calls wrapped in `SwingWorker`.

**Step 9 — `ui/VehiclePanel.java`**
- Customer-lookup section (search by phone) to select owner.
- Vehicle form: Registration No, Make, Model, Year, Fuel Type (JComboBox), Color, Odometer.
- "Register Vehicle" button → `VehicleService.registerVehicle()`.
- Table showing vehicles for selected customer.

### Verification & Acceptance Criteria

```bash
# 1. Compile
mvn compile
# Expected: BUILD SUCCESS with zero errors

# 2. Run the application
mvn exec:java
# Expected: Swing window appears with "Customers" and "Vehicles" tabs

# Manual checks:
# A. In Customers tab: enter Name="Test User", Phone="9000000001" → click Register
#    → Row appears in the table below.
# B. Search by Phone "9000000001" → row is highlighted.
# C. In Vehicles tab: search for customer by phone → select them
#    → Enter RegNo="TN01ZZ9999", Make="Test", Model="Car", Year=2023, Fuel=PETROL
#    → Click Register → vehicle appears in table.
# D. Restart app; verify data persisted (visible in table on load from DB).
```

### Definition of Done
- [ ] `mvn compile` passes with zero errors.
- [ ] Application window launches with no exceptions in console.
- [ ] A new customer can be registered and immediately appears in the table.
- [ ] Customer can be found by phone number.
- [ ] A vehicle can be registered under that customer.
- [ ] All data survives application restart (confirmed from DB).

---

## Phase 2 — Service Catalogue + Appointment Booking Module

### Objective & Scope
Implement the Service Catalogue display and the full Appointment Booking workflow. A user selects a vehicle, chooses a service type, picks a date/time, and the job card is created in `BOOKED` status. This phase introduces the `Service` and `JobCard` models, their DAOs, the `BookingService`, and the `BookingPanel` UI.

### Files to Create / Modify

```
src/main/java/com/garage/
├── model/
│   ├── Service.java
│   └── JobCard.java
├── dao/
│   ├── ServiceDAO.java
│   └── JobCardDAO.java
├── service/
│   └── BookingService.java
└── ui/
    ├── BookingPanel.java
    └── MainFrame.java         ← ADD "Booking" tab
```

### Step-by-Step Implementation Flow

**Step 1 — `model/Service.java` and `model/JobCard.java`**
- `Service`: serviceId, serviceName, description, labourRate, rateType.
- `JobCard`: jobCardId, vehicleId, serviceId, technicianName, status, appointmentDt, startDt, completionDt, deliveryDt, labourHours, remarks, createdAt, updatedAt.

**Step 2 — `dao/ServiceDAO.java`**
- `findAll(Connection conn) : List<Service>` — loads catalogue on startup.
- `findById(Connection conn, int serviceId) : Optional<Service>`.

**Step 3 — `dao/JobCardDAO.java`**
- `insert(Connection conn, JobCard jc) : int`.
- `findById(Connection conn, int jobCardId) : Optional<JobCard>`.
- `findByVehicleId(Connection conn, int vehicleId) : List<JobCard>`.
- `findByStatus(Connection conn, String status) : List<JobCard>`.
- `updateStatus(Connection conn, int jobCardId, String newStatus) : void` — also sets the appropriate timestamp column (START_DT, COMPLETION_DT, DELIVERY_DT).

**Step 4 — `service/BookingService.java`**
- `bookAppointment(int vehicleId, int serviceId, LocalDateTime appointmentDt, String remarks) : JobCard`
  - Validates vehicleId exists (call `VehicleDAO.findById`).
  - Validates `appointmentDt` is in the future.
  - Inserts `JobCard` with status `BOOKED`.
  - Returns the created `JobCard`.

**Step 5 — `ui/BookingPanel.java`**
- Vehicle lookup by registration number (text field + "Find" button).
- Displays vehicle details (make, model, owner name) once found.
- `JComboBox<Service>` populated from `ServiceDAO.findAll()` at panel load time.
- `JSpinner` with `SpinnerDateModel` for appointment date/time selection.
- Remarks `JTextArea`.
- "Book Appointment" button → `BookingService.bookAppointment()` in `SwingWorker`.
- On success: `JOptionPane` with confirmation message including Job Card ID.

**Step 6 — `MainFrame.java`**
- Add "Booking" tab pointing to `BookingPanel`.

### Verification & Acceptance Criteria

```bash
mvn exec:java
# Manual checks:
# A. Open Booking tab. Enter a vehicle registration from seed data (e.g., TN09AB1234).
#    Click Find → vehicle details appear.
# B. Select "Full Service" from service dropdown.
#    Set appointment date/time to tomorrow.
#    Click "Book Appointment".
#    → Dialog: "Appointment booked! Job Card #8" (or next sequence value).
# C. Verify via SQL*Plus or DBeaver:
#    SELECT * FROM JOB_CARDS WHERE STATUS = 'BOOKED';
#    → New row appears.
```

### Definition of Done
- [ ] Service dropdown populates correctly from DB on panel load.
- [ ] Vehicle search finds vehicle and displays owner details.
- [ ] Booking creates a `JOB_CARDS` row with `STATUS = 'BOOKED'`.
- [ ] Booking for a non-existent vehicle shows an error dialog.
- [ ] Booking with a past date shows a validation error.

---

## Phase 3 — Job Card Status Tracking Module

### Objective & Scope
Build the operational dashboard for garage staff to view all job cards grouped by status, advance their lifecycle stage, assign technicians, and record labour hours. This phase is entirely around `JobCardPanel` and the `JobCardService`.

### Files to Create / Modify

```
src/main/java/com/garage/
├── service/
│   └── JobCardService.java
└── ui/
    ├── JobCardPanel.java
    └── MainFrame.java         ← ADD "Job Cards" tab
```

### Step-by-Step Implementation Flow

**Step 1 — `service/JobCardService.java`**
- `getAllJobCards() : List<JobCard>` — fetch all, for full dashboard view.
- `getByStatus(String status) : List<JobCard>`.
- `advanceStatus(int jobCardId) : JobCard` — transitions: BOOKED→IN_PROGRESS→COMPLETED→DELIVERED. Validates legal transition. Sets appropriate timestamp.
- `assignTechnician(int jobCardId, String technicianName) : void`.
- `recordLabourHours(int jobCardId, double hours) : void`.

**Step 2 — `ui/JobCardPanel.java`**
- Status filter row: `JRadioButton` group for ALL / BOOKED / IN_PROGRESS / COMPLETED / DELIVERED.
- Main `JTable` (inside `JScrollPane`) with columns: Job Card ID, Vehicle Reg, Customer Name, Service Name, Technician, Status, Appointment Date, Labour Hours.
  - Custom `AbstractTableModel` backed by `List<JobCardDisplayRow>` (a flat DTO joining vehicle + customer + service info).
- Selection-driven detail panel below (or right panel) showing full remarks.
- Action buttons (enabled only when a row is selected):
  - **"Advance Status"**: calls `JobCardService.advanceStatus()`, refreshes table.
  - **"Assign Technician"**: shows `JOptionPane.showInputDialog()`, calls `JobCardService.assignTechnician()`.
  - **"Record Hours"**: shows `JOptionPane.showInputDialog()` with numeric validation.
- "Refresh" button to reload from DB.

**Step 3 — `MainFrame.java`**
- Add "Job Cards" tab pointing to `JobCardPanel`.

### Verification & Acceptance Criteria

```bash
mvn exec:java
# Manual checks:
# A. Open "Job Cards" tab. All 7 seed job cards are visible.
# B. Filter by "BOOKED" → only JC-4 and JC-5 appear.
# C. Select JC-4 (Anand's Fortuner). Click "Assign Technician" → enter "Vignesh T."
#    → Table refreshes; technician column updates.
# D. Click "Advance Status" → status changes from BOOKED to IN_PROGRESS.
# E. Verify in DB: SELECT JOB_CARD_ID, STATUS, START_DT FROM JOB_CARDS WHERE JOB_CARD_ID = 4;
#    → STATUS = 'IN_PROGRESS', START_DT is populated.
# F. Advance again → COMPLETED; COMPLETION_DT populated.
# G. Advance again → DELIVERED; DELIVERY_DT populated.
# H. Attempt to advance a DELIVERED job → error dialog shown.
```

### Definition of Done
- [ ] All job cards load into table on tab open.
- [ ] Status filter buttons correctly filter the table.
- [ ] Status advancement follows the correct lifecycle and updates timestamps.
- [ ] Cannot advance past `DELIVERED` (error dialog shown).
- [ ] Technician assignment persists after app restart.
- [ ] Labour hours recorded with positive-number validation.

---

## Phase 4 — Billing & Invoicing Module

### Objective & Scope
Implement the full billing workflow: auto-generating an invoice from a `COMPLETED` job card, adding spare parts as invoice line items, computing and displaying the itemised total with GST, and marking the invoice as paid. This phase introduces `Invoice`, `InvoiceItem` models, their DAOs, `BillingService`, and `BillingPanel`.

### Files to Create / Modify

```
src/main/java/com/garage/
├── model/
│   ├── Invoice.java
│   └── InvoiceItem.java
├── dao/
│   ├── InvoiceDAO.java
│   └── InvoiceItemDAO.java
├── service/
│   └── BillingService.java
└── ui/
    ├── BillingPanel.java
    └── MainFrame.java         ← ADD "Billing" tab
```

### Step-by-Step Implementation Flow

**Step 1 — `model/Invoice.java` and `model/InvoiceItem.java`**
- Mirror all schema columns as described in `docs/DATABASE.md`.

**Step 2 — `dao/InvoiceDAO.java`**
- `insert(Connection conn, Invoice inv) : int`.
- `findByJobCardId(Connection conn, int jobCardId) : Optional<Invoice>`.
- `findById(Connection conn, int invoiceId) : Optional<Invoice>`.
- `updateTotals(Connection conn, int invoiceId, double labourTotal, double partsTotal, double taxAmount, double grandTotal) : void`.
- `updatePaymentStatus(Connection conn, int invoiceId, String status, String mode) : void`.

**Step 3 — `dao/InvoiceItemDAO.java`**
- `insert(Connection conn, InvoiceItem item) : int`.
- `findByInvoiceId(Connection conn, int invoiceId) : List<InvoiceItem>`.

**Step 4 — `service/BillingService.java`**
- `generateInvoice(int jobCardId) : Invoice`
  - Checks no invoice already exists for this jobCardId (`findByJobCardId`).
  - Fetches `JobCard` and `Service` to compute initial labour amount.
  - If service `RATE_TYPE = 'HOURLY'`: `labourTotal = labourRate × labourHours`.
  - If `RATE_TYPE = 'FIXED'`: `labourTotal = labourRate`.
  - Inserts `Invoice` row with `labourTotal`, `partsTotal = 0`, `taxPercent = 18`.
  - Inserts `InvoiceItem` of type `LABOUR` for the computed labour charge.
  - Returns created `Invoice`.
- `addPart(int invoiceId, String description, double qty, double unitPrice) : InvoiceItem`
  - Inserts `InvoiceItem` of type `PART`.
  - Calls `recomputeTotals(invoiceId)`.
- `recomputeTotals(int invoiceId) : void`
  - `SELECT SUM(LINE_TOTAL) ... WHERE ITEM_TYPE='LABOUR'` → labourTotal.
  - `SELECT SUM(LINE_TOTAL) ... WHERE ITEM_TYPE='PART'` → partsTotal.
  - `taxAmount = (labourTotal + partsTotal) × taxPercent / 100`.
  - `grandTotal = labourTotal + partsTotal + taxAmount`.
  - Calls `InvoiceDAO.updateTotals(...)`.
- `markAsPaid(int invoiceId, String paymentMode) : void`.

**Step 5 — `ui/BillingPanel.java`**
- Job card search: lookup by Job Card ID or select from list of COMPLETED/DELIVERED jobs.
- "Generate Invoice" button → `BillingService.generateInvoice()`.
- Invoice header display: Invoice ID, Date, Job Card, Vehicle, Customer.
- `JTable` showing `INVOICE_ITEMS` with columns: Type, Description, Qty, Unit Price, Line Total.
- "Add Part" button: opens simple input dialog (Description, Qty, Unit Price) → `BillingService.addPart()`, refreshes items table.
- Summary panel (right or bottom): Labour Total, Parts Total, Tax (18% GST), **Grand Total** — all in bold.
- "Mark as Paid" button with `JComboBox` for payment mode (CASH/CARD/UPI/ONLINE/CHEQUE).
- Payment status badge (PENDING in red, PAID in green) using `JLabel` with colored foreground.

**Step 6 — `MainFrame.java`**
- Add "Billing" tab pointing to `BillingPanel`.

### Verification & Acceptance Criteria

```bash
mvn exec:java
# Manual checks:
# A. Open Billing tab. Enter Job Card ID = 1 (Rajan's Full Service — already has seed invoice).
#    → Invoice loads with existing line items and PAID status shown in green.
# B. Enter Job Card ID = 2 (Brake Inspection — COMPLETED, seed invoice exists but PENDING).
#    → Invoice loads, Grand Total = ₹3,762.15. Click "Mark as Paid" → select "CARD".
#    → Status badge changes to PAID (green). Verify DB:
#    SELECT PAYMENT_STATUS, PAYMENT_MODE FROM INVOICES WHERE JOB_CARD_ID = 2;
# C. For a freshly COMPLETED job card (created in Phase 3 tests):
#    Click "Generate Invoice" → invoice created, labour item appears.
#    Click "Add Part" → enter "Brake Fluid 500ml", Qty=1, Price=250.
#    → Parts total updates; Grand Total recalculates with 18% GST.
```

### Definition of Done
- [ ] Invoice generated correctly for FIXED and HOURLY rate services.
- [ ] Adding a part updates line items table and recomputes grand total.
- [ ] GST (18%) computed and displayed correctly.
- [ ] "Mark as Paid" persists `PAYMENT_STATUS = 'PAID'` and payment mode.
- [ ] Attempting to generate invoice for a job card that already has one shows error.
- [ ] All amounts display with 2 decimal places and ₹ prefix.

---

## Phase 5 — Service History & Final Polish

### Objective & Scope
Implement the Service History lookup (full vehicle service record across all visits), apply final UI polish across all panels (consistent borders, fonts, table renderers, status colour coding), handle all edge cases, and prepare the application for live demonstration. This is the completion phase.

### Files to Create / Modify

```
src/main/java/com/garage/
├── service/
│   └── HistoryService.java
└── ui/
    ├── ServiceHistoryPanel.java
    ├── MainFrame.java             ← ADD "History" tab + final polish
    ├── CustomerPanel.java         ← Polish if needed
    ├── VehiclePanel.java          ← Polish if needed
    ├── BookingPanel.java          ← Polish if needed
    ├── JobCardPanel.java          ← Polish if needed
    └── BillingPanel.java          ← Polish if needed
```

### Step-by-Step Implementation Flow

**Step 1 — `service/HistoryService.java`**
- `getServiceHistory(String registrationNo) : List<ServiceHistoryRecord>`
  - Executes a multi-table JOIN:
    ```sql
    SELECT JC.JOB_CARD_ID, JC.APPOINTMENT_DT, JC.STATUS,
           S.SERVICE_NAME, JC.TECHNICIAN_NAME, JC.LABOUR_HOURS,
           I.GRAND_TOTAL, I.PAYMENT_STATUS
    FROM   JOB_CARDS JC
    JOIN   VEHICLES  V  ON V.VEHICLE_ID  = JC.VEHICLE_ID
    JOIN   SERVICES  S  ON S.SERVICE_ID  = JC.SERVICE_ID
    LEFT   JOIN INVOICES I ON I.JOB_CARD_ID = JC.JOB_CARD_ID
    WHERE  V.REGISTRATION_NO = ?
    ORDER  BY JC.APPOINTMENT_DT DESC
    ```
  - Returns a flat `ServiceHistoryRecord` DTO (not a raw model object).

**Step 2 — `ui/ServiceHistoryPanel.java`**
- Search by Registration Number (text field + "Search" button).
- Vehicle summary header: Make, Model, Year, Owner Name, Total Services.
- `JTable` showing history: Date, Service, Technician, Status, Hours, Invoice Total, Payment.
- Status cells colour-coded using a custom `TableCellRenderer`: BOOKED=blue, IN_PROGRESS=orange, COMPLETED=purple, DELIVERED=green.
- "Export History" button (P1): prints to `System.out` as formatted text, or shows a `JTextArea` in a dialog.

**Step 3 — Application-wide UI Polish**
- Consistent `BorderFactory.createTitledBorder(...)` on every form section.
- Consistent `BorderFactory.createEmptyBorder(10, 10, 10, 10)` padding on all panels.
- All `JTable` instances: `setRowHeight(24)`, `setFillsViewportHeight(true)`, `getTableHeader().setReorderingAllowed(false)`.
- Button alignment: standardise on `FlowLayout.RIGHT` in button rows.
- Error messages: confirm all use `JOptionPane.showMessageDialog(..., JOptionPane.ERROR_MESSAGE)`.
- Input validation: all form fields trim whitespace before use.
- `JLabel` for app title in `MainFrame` top area with bold font.

**Step 4 — Edge Case Hardening**
- Empty table state: if no rows returned, display a centred informational label instead of an empty table.
- DB connection failure at startup: show `JOptionPane.showMessageDialog(null, "Cannot connect to database...", "Fatal Error", JOptionPane.ERROR_MESSAGE)` then `System.exit(1)`.
- Number input fields: use `JFormattedTextField` with `NumberFormat` to prevent non-numeric entry.

**Step 5 — `MainFrame.java`**
- Add "Service History" as the final tab.
- Set `JFrame` minimum size: `setMinimumSize(new Dimension(900, 650))`.
- Set window title: `"Vehicle Service Management System — Garage Pro"`.
- Centre on screen: `setLocationRelativeTo(null)`.

### Verification & Acceptance Criteria

```bash
mvn exec:java
# Manual checks (full integration run):
# A. Search history for "TN09AB1234" → history shows at minimum the seed Full Service job.
# B. History for a new registration (from Phase 2 test) → shows all booked/completed visits.
# C. All 5 tabs are present and switch cleanly.
# D. Resize window to minimum (900×650) → no layout breaks, all components visible.
# E. Disconnect Docker container mid-session (docker compose stop):
#    → Click any "Refresh" or "Search" button
#    → Error dialog appears; application does not crash.
# F. Enter letters in a numeric field (Year, Odometer, Hours) → no crash, validation error shown.

# Full smoke test — new end-to-end flow:
# 1. Register new customer "Demo User" phone 9001234567
# 2. Register vehicle TN00ZZ0001 under Demo User, Fuel=PETROL
# 3. Book Full Service appointment (tomorrow)
# 4. On Job Cards tab: advance to IN_PROGRESS, assign technician, record 2.5 hours
# 5. Advance to COMPLETED
# 6. On Billing tab: generate invoice → ₹1,200 (fixed) + 18% = ₹1,416. Add one part.
# 7. Mark as Paid (UPI)
# 8. On History tab: search TN00ZZ0001 → full record visible
```

### Definition of Done
- [ ] Service History panel returns correct multi-join results.
- [ ] Status cells are colour-coded in the history table.
- [ ] All 5 application tabs are fully functional.
- [ ] Full end-to-end smoke test (steps 1–8 above) passes without errors.
- [ ] No `NullPointerException` or uncaught exceptions during normal operation.
- [ ] Application window is professional-looking and demo-ready.
- [ ] Application handles DB connection loss gracefully.

---

## PART D — Phase Summary Table

| Phase | Module | New Files | Key Deliverable |
|---|---|---|---|
| **Phase 0** | Foundation & Docs | All root + docs + db | Blueprint ready; DB initialised |
| **Phase 1** | Customer & Vehicle | DBConnection, Customer/Vehicle Model+DAO+Service+UI | First end-to-end CRUD slice |
| **Phase 2** | Booking | Service/JobCard Model+DAO, BookingService, BookingPanel | Appointments booked to DB |
| **Phase 3** | Job Card Tracking | JobCardService, JobCardPanel | Full status lifecycle management |
| **Phase 4** | Billing | Invoice/Item Model+DAO, BillingService, BillingPanel | Itemised invoices with GST |
| **Phase 5** | History + Polish | HistoryService, ServiceHistoryPanel, UI hardening | Demo-ready application |
