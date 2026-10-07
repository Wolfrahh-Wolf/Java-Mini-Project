# Architecture — Vehicle Service Management System (VSMS)

> Layered MVC/DAO Desktop Architecture | Java Swing + JDBC + Oracle Database

---

## 1. Architectural Pattern

VSMS uses a classic **three-tier desktop MVC/DAO** pattern adapted for a standalone Swing application:

```
┌─────────────────────────────────────────────────────────────────┐
│                        PRESENTATION TIER                        │
│                     Java Swing (ui/ package)                    │
│   JFrame • JPanel • JTable • JDialog • JOptionPane              │
└───────────────────────────┬─────────────────────────────────────┘
                            │  calls (on EDT via SwingWorker)
┌───────────────────────────▼─────────────────────────────────────┐
│                        BUSINESS LOGIC TIER                      │
│                   Service Layer (service/ package)              │
│   Validation • Transaction demarcation • Business rules         │
└───────────────────────────┬─────────────────────────────────────┘
                            │  delegates
┌───────────────────────────▼─────────────────────────────────────┐
│                        DATA ACCESS TIER                         │
│                     DAO Layer (dao/ package)                    │
│        Pure JDBC • PreparedStatement • ResultSet mapping        │
└───────────────────────────┬─────────────────────────────────────┘
                            │  connection from
┌───────────────────────────▼─────────────────────────────────────┐
│                        INFRASTRUCTURE                           │
│           DBConnection (util/) + Oracle DB (Docker)             │
└─────────────────────────────────────────────────────────────────┘
```

---

## 2. Package Structure & Responsibilities

```
db/
├── schema.sql                      ← Oracle DDL: sequences, tables, triggers, indexes, seed data
├── 01_init.sh                      ← Shell script executed by Docker init to run schema.sql
│
src/main/java/com/garage/
│
├── model/                          ← PLAIN JAVA POJOs
│   ├── Customer.java               → Fields: customerId, name, phone, email, address, createdAt
│   ├── Vehicle.java                → Fields: vehicleId, customerId, registrationNo, make, model, yearOfMfr, fuelType, color, odometerKm
│   ├── Service.java                → Fields: serviceId, serviceName, description, labourRate, rateType
│   ├── JobCard.java                → Fields: jobCardId, vehicleId, serviceId, technicianName, status, appointmentDt, startDt, completionDt, deliveryDt, labourHours, remarks
│   ├── Invoice.java                → Fields: invoiceId, jobCardId, invoiceDate, labourTotal, partsTotal, taxPercent, taxAmount, grandTotal, paymentStatus, paymentMode
│   └── InvoiceItem.java            → Fields: itemId, invoiceId, itemType, description, quantity, unitPrice, lineTotal
│
├── dao/                            ← PURE JDBC DATA ACCESS OBJECTS
│   ├── CustomerDAO.java            → CRUD + search by phone/name
│   ├── VehicleDAO.java             → CRUD + findByCustomer + findByRegistration + findById
│   ├── ServiceDAO.java             → findAll + findById
│   ├── JobCardDAO.java             → CRUD + findByStatus + findByVehicle + updateStatus + updateTechnician + recordHours
│   ├── InvoiceDAO.java             → create + findByJobCard + findById + updateTotals + updatePaymentStatus
│   └── InvoiceItemDAO.java         → insert + findByInvoice
│
├── util/                           ← SHARED UTILITIES
│   └── DBConnection.java           → Thread-safe singleton; loads .env via dotenv-java; returns Connection
│
├── service/                        ← BUSINESS LOGIC + TRANSACTION MANAGEMENT
│   ├── CustomerService.java        → registerCustomer, findByPhone, findAll, findById
│   ├── VehicleService.java         → registerVehicle, getVehiclesForCustomer, getVehicleForCustomer
│   ├── BookingService.java         → bookAppointment (vehicle + job card atomically)
│   ├── JobCardService.java         → getAllJobCards, getByStatus, advanceStatus, assignTechnician, recordLabourHours
│   ├── BillingService.java         → generateInvoice, addPart, recomputeTotals, markAsPaid
│   └── HistoryService.java         → getServiceHistory (multi-table JOIN), getVehicleSummary
│
└── ui/                             ← SWING VIEWS
    ├── MainApp.java                → Entry point; sets FlatDarkLaf + FluentTheme; invokesLater MainFrame
    ├── MainFrame.java              → JFrame with JTabbedPane housing all panels
    ├── FluentTheme.java            → Fluent UI 2 Dark design tokens; global FlatLaf defaults; helper factories
    ├── CustomerPanel.java          → Register + search customers
    ├── VehiclePanel.java           → Register + list vehicles per customer
    ├── BookingPanel.java           → Appointment booking form
    ├── JobCardPanel.java           → Job card status board with update controls
    ├── BillingPanel.java           → Invoice view + add items + mark paid
    └── ServiceHistoryPanel.java    → Vehicle history search and display
```

---

## 3. Data Flow Diagrams

### 3.1 Application Bootstrap

```
main() thread
    │
    ▼
UIManager.setLookAndFeel(systemLAF)
    │
    ▼
SwingUtilities.invokeLater(...)
    │  (jumps to EDT)
    ▼
MainFrame constructor
    ├── new CustomerPanel()
    ├── new VehiclePanel()
    ├── new BookingPanel()
    ├── new JobCardPanel()
    ├── new BillingPanel()
    └── new ServiceHistoryPanel()
```

### 3.2 Booking Appointment Flow

```
BookingPanel (EDT)
    │ User fills form + clicks "Book"
    ▼
SwingWorker.doInBackground()        ← leaves EDT
    │
    ▼
BookingService.bookAppointment()
    ├── validate inputs
    ├── conn = DBConnection.getConnection()
    ├── conn.setAutoCommit(false)
    ├── VehicleDAO.findByRegistration()  → SELECT
    ├── JobCardDAO.insert()              → INSERT
    ├── conn.commit()
    └── return JobCard
    │
    ▼
SwingWorker.done() (back on EDT)
    └── JOptionPane.showMessage("Booking confirmed")
        + refresh table model
```

### 3.3 Invoice Generation Flow

```
BillingPanel (EDT)
    │ Technician clicks "Generate Invoice"
    ▼
SwingWorker.doInBackground()
    │
    ▼
BillingService.generateInvoice(jobCardId)
    ├── JobCardDAO.findById()             → fetch job card
    ├── ServiceDAO.findById()             → fetch labour rate
    ├── compute labourTotal = rate × hours (or fixed)
    ├── InvoiceDAO.insert(invoice)        → INSERT
    └── return Invoice
    │
    ▼
BillingPanel.addItem() loop
    └── InvoiceItemDAO.insert(item)       → INSERT per spare part

BillingService.computeAndSaveTotals(invoiceId)
    ├── SELECT SUM(LINE_TOTAL) WHERE ITEM_TYPE = 'LABOUR'
    ├── SELECT SUM(LINE_TOTAL) WHERE ITEM_TYPE = 'PART'
    ├── compute taxAmount = (labour + parts) × taxPercent / 100
    ├── compute grandTotal
    └── InvoiceDAO.updateTotals()         → UPDATE
```

---

## 4. DBConnection Singleton

```java
// Pattern: Initialise-on-class-load (thread-safe without synchronized)
public class DBConnection {
    private static final Connection INSTANCE;

    static {
        Dotenv env = Dotenv.load();
        try {
            INSTANCE = DriverManager.getConnection(
                env.get("DB_URL"),
                env.get("DB_USER"),
                env.get("DB_PASSWORD")
            );
        } catch (SQLException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public static Connection getConnection() { return INSTANCE; }
    private DBConnection() {}
}
```

> **Note**: For production use, replace with a connection pool (`HikariCP`) and JNDI lookup.

---

## 5. Threading Model

| Thread | Responsibility |
|---|---|
| Event Dispatch Thread (EDT) | All Swing component creation, reading, and mutation |
| `SwingWorker.doInBackground()` | All JDBC calls (blocking I/O must not block EDT) |
| `SwingWorker.done()` | Updating UI after DB call completes (runs on EDT) |
| Main thread | Only `SwingUtilities.invokeLater(...)` bootstrap |

---

## 6. Error Handling Strategy

```
UI Layer        → catch Exception from SwingWorker.get(); show JOptionPane.ERROR_MESSAGE
Service Layer   → rollback transaction on SQLException; re-throw as RuntimeException with context
DAO Layer       → throw SQLException upward; never swallow
DBConnection    → fatal if connection fails; error displayed before frame shows
```

---

## 7. Key Design Decisions

| Decision | Rationale |
|---|---|
| Single `Connection` singleton | Desktop application; single-user per instance |
| `JTabbedPane` as main container | Simplest multi-view navigation without routing complexity |
| `AbstractTableModel` subclasses | Clean separation of table data from Swing rendering |
| Service layer owns transactions | DAO methods accept a `Connection` parameter for testability |
| Virtual `LINE_TOTAL` column in DB | Ensures computed value is always consistent; no risk of stale cached total |
