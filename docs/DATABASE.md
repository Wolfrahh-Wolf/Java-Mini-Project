# Database Design — Pit Stop (Vehicle Service Management System)

> Oracle Database Free 23c | Schema: `garage_user` | PDB: `FREEPDB1`

---

## 1. Entity–Relationship Overview

```
CUSTOMERS ──< VEHICLES ──< JOB_CARDS >── SERVICES
                                │
                                ▼
                            INVOICES ──< INVOICE_ITEMS
```

| Relationship | Cardinality | Description |
|---|---|---|
| CUSTOMERS → VEHICLES | 1 : N | One customer may own many vehicles |
| VEHICLES → JOB_CARDS | 1 : N | One vehicle may have many service visits |
| SERVICES → JOB_CARDS | 1 : N | One service type may be used in many job cards |
| JOB_CARDS → INVOICES | 1 : 1 | Each job card has exactly one invoice |
| INVOICES → INVOICE_ITEMS | 1 : N | One invoice contains many line items |

---

## 2. Table Descriptions

### 2.1 CUSTOMERS

| Column | Data Type | Constraints | Description |
|---|---|---|---|
| `CUSTOMER_ID` | `NUMBER` | PK, auto via sequence | Surrogate primary key |
| `CUSTOMER_NAME` | `VARCHAR2(120)` | NOT NULL | Full name |
| `PHONE` | `VARCHAR2(15)` | NOT NULL, UNIQUE | Mobile number (business key) |
| `EMAIL` | `VARCHAR2(150)` | NULL OK, format check | Email address |
| `ADDRESS` | `VARCHAR2(300)` | NULL OK | Street / city address |
| `CREATED_AT` | `TIMESTAMP` | DEFAULT SYSTIMESTAMP | Record creation timestamp |

### 2.2 VEHICLES

| Column | Data Type | Constraints | Description |
|---|---|---|---|
| `VEHICLE_ID` | `NUMBER` | PK | Surrogate PK |
| `CUSTOMER_ID` | `NUMBER` | FK → CUSTOMERS | Owner reference |
| `REGISTRATION_NO` | `VARCHAR2(20)` | NOT NULL, UNIQUE | Number plate (business key) |
| `MAKE` | `VARCHAR2(60)` | NOT NULL | Manufacturer (e.g., Toyota) |
| `MODEL` | `VARCHAR2(60)` | NOT NULL | Model name (e.g., Corolla) |
| `YEAR_OF_MFR` | `NUMBER(4)` | CHECK 1900–2100 | Manufacturing year |
| `FUEL_TYPE` | `VARCHAR2(15)` | CHECK enum | PETROL/DIESEL/ELECTRIC/HYBRID/CNG/LPG |
| `COLOR` | `VARCHAR2(40)` | NULL OK | Body colour |
| `ODOMETER_KM` | `NUMBER(10)` | DEFAULT 0, ≥ 0 | Current odometer reading |
| `CREATED_AT` | `TIMESTAMP` | DEFAULT SYSTIMESTAMP | Record creation timestamp |

### 2.3 SERVICES (Catalogue)

| Column | Data Type | Constraints | Description |
|---|---|---|---|
| `SERVICE_ID` | `NUMBER` | PK | Surrogate PK |
| `SERVICE_NAME` | `VARCHAR2(120)` | NOT NULL, UNIQUE | Display name |
| `DESCRIPTION` | `VARCHAR2(500)` | NULL OK | Detailed description |
| `LABOUR_RATE` | `NUMBER(10,2)` | ≥ 0 | Rate in INR (fixed or per hour) |
| `RATE_TYPE` | `VARCHAR2(10)` | CHECK: FIXED/HOURLY | Determines how labour is billed |
| `CREATED_AT` | `TIMESTAMP` | DEFAULT SYSTIMESTAMP | Record creation timestamp |

### 2.4 JOB_CARDS

| Column | Data Type | Constraints | Description |
|---|---|---|---|
| `JOB_CARD_ID` | `NUMBER` | PK | Surrogate PK |
| `VEHICLE_ID` | `NUMBER` | FK → VEHICLES | Which vehicle is being serviced |
| `SERVICE_ID` | `NUMBER` | FK → SERVICES | Type of service being performed |
| `TECHNICIAN_NAME` | `VARCHAR2(120)` | NULL OK | Assigned technician (NULL when booked, not yet assigned) |
| `STATUS` | `VARCHAR2(15)` | CHECK enum, DEFAULT 'BOOKED' | Lifecycle status |
| `APPOINTMENT_DT` | `TIMESTAMP` | NOT NULL | Scheduled appointment date/time |
| `START_DT` | `TIMESTAMP` | NULL OK | When work actually began |
| `COMPLETION_DT` | `TIMESTAMP` | NULL OK | When work was completed |
| `DELIVERY_DT` | `TIMESTAMP` | NULL OK | When vehicle was handed back |
| `LABOUR_HOURS` | `NUMBER(6,2)` | DEFAULT 0 | Actual labour hours recorded |
| `REMARKS` | `VARCHAR2(1000)` | NULL OK | Technician notes |
| `CREATED_AT` | `TIMESTAMP` | DEFAULT SYSTIMESTAMP | Record creation |
| `UPDATED_AT` | `TIMESTAMP` | DEFAULT SYSTIMESTAMP, auto-updated by trigger | Last modification |

**Status Lifecycle Constraint:**
```
BOOKED → IN_PROGRESS → COMPLETED → DELIVERED
```
Enforced by `CK_JOB_CARDS_STATUS CHECK (STATUS IN ('BOOKED','IN_PROGRESS','COMPLETED','DELIVERED'))`.

### 2.5 INVOICES

| Column | Data Type | Constraints | Description |
|---|---|---|---|
| `INVOICE_ID` | `NUMBER` | PK | Surrogate PK |
| `JOB_CARD_ID` | `NUMBER` | FK → JOB_CARDS, UNIQUE | 1:1 with job card |
| `INVOICE_DATE` | `TIMESTAMP` | DEFAULT SYSTIMESTAMP | When invoice was generated |
| `LABOUR_TOTAL` | `NUMBER(12,2)` | NOT NULL | Sum of all LABOUR line items |
| `PARTS_TOTAL` | `NUMBER(12,2)` | NOT NULL | Sum of all PART line items |
| `TAX_PERCENT` | `NUMBER(5,2)` | CHECK 0–100 | GST/tax rate applied |
| `TAX_AMOUNT` | `NUMBER(12,2)` | NOT NULL | Computed tax (stored for immutability) |
| `GRAND_TOTAL` | `NUMBER(12,2)` | NOT NULL | Labour + Parts + Tax |
| `PAYMENT_STATUS` | `VARCHAR2(15)` | CHECK enum | PENDING / PAID / WAIVED |
| `PAYMENT_MODE` | `VARCHAR2(20)` | CHECK enum, NULL OK | CASH/CARD/UPI/ONLINE/CHEQUE |
| `NOTES` | `VARCHAR2(500)` | NULL OK | Any billing notes |

### 2.6 INVOICE_ITEMS

| Column | Data Type | Constraints | Description |
|---|---|---|---|
| `ITEM_ID` | `NUMBER` | PK | Surrogate PK |
| `INVOICE_ID` | `NUMBER` | FK → INVOICES | Parent invoice |
| `ITEM_TYPE` | `VARCHAR2(10)` | CHECK: LABOUR/PART | Distinguishes labour from parts |
| `DESCRIPTION` | `VARCHAR2(200)` | NOT NULL | Line item description |
| `QUANTITY` | `NUMBER(8,2)` | > 0 | Quantity or hours |
| `UNIT_PRICE` | `NUMBER(12,2)` | ≥ 0 | Price per unit |
| `LINE_TOTAL` | `NUMBER(12,2)` | VIRTUAL (QUANTITY × UNIT_PRICE) | Computed, always consistent |

---

## 3. Formal Normalization Justification

### 3.0 Unnormalized Form (UNF) — The Starting Point

Consider a single flat worksheet a garage might use:

```
[ JOB_SHEET ]
JobNo | CustomerName | CustomerPhone | CustomerAddress |
VehicleRegNo | VehicleMake | VehicleModel | VehicleYear | FuelType |
ServiceName | ServiceRate | TechnicianName | Status | AppointmentDate |
LabourHours | Part1Name | Part1Qty | Part1Price | Part2Name | Part2Qty | Part2Price |
InvoiceTotal | PaymentStatus
```

**UNF Problems identified:**
1. **Repeating groups**: `Part1Name`, `Part2Name`, `Part3Name`… are multi-valued groups — not atomic.
2. **Multiple facts per row**: Customer info, vehicle info, service info, parts, and billing all in one row.
3. **No single identifiable key** for the entire row.

---

### 3.1 First Normal Form (1NF)

**Rule**: Every attribute must be atomic (indivisible); no repeating groups; every row must be uniquely identifiable.

**Transformation**: Eliminate the repeating parts groups. Each part becomes its own row.

```
FLAT_SERVICE_RECORD (
  JobNo, CustomerName, CustomerPhone, CustomerAddress,
  VehicleRegNo, VehicleMake, VehicleModel, VehicleYear, FuelType,
  ServiceName, ServiceRate, TechnicianName, Status, AppointmentDate,
  LabourHours, LineItemDescription, Quantity, UnitPrice, ItemType,
  InvoiceTotal, PaymentStatus
)
Primary Key: (JobNo, LineItemDescription)
```

**1NF is satisfied because:**
- All attribute values are atomic (no sets or arrays).
- Each row represents a single line item in the context of a job.
- A composite key `(JobNo, LineItemDescription)` uniquely identifies each row.

**Remaining problems (for 2NF):**
- `CustomerName`, `CustomerPhone`, `CustomerAddress` depend only on part of the key → `JobNo` (not the full composite key).
- `VehicleRegNo`, `VehicleMake`, `VehicleModel`, `VehicleYear`, `FuelType` depend only on `JobNo`.
- `ServiceName`, `ServiceRate` depend only on `JobNo`.
- `InvoiceTotal` and `PaymentStatus` depend only on `JobNo`.

---

### 3.2 Second Normal Form (2NF)

**Rule**: Must be in 1NF AND every non-key attribute must be fully functionally dependent on the **entire** primary key. No partial dependencies on a subset of a composite key are allowed.

**Transformation**: Decompose by removing every partial dependency.

```
CUSTOMERS (CustomerID → CustomerName, CustomerPhone, CustomerAddress)
    PK: CustomerID

VEHICLES (VehicleID → VehicleRegNo, Make, Model, Year, FuelType, CustomerID)
    PK: VehicleID,  FK: CustomerID → CUSTOMERS

SERVICES (ServiceID → ServiceName, ServiceRate, RateType)
    PK: ServiceID

JOB_CARDS (JobCardID → VehicleID, ServiceID, TechnicianName, Status,
                        AppointmentDate, LabourHours, ...)
    PK: JobCardID,  FK: VehicleID → VEHICLES, ServiceID → SERVICES

INVOICES (InvoiceID → JobCardID, LabourTotal, PartsTotal, Tax, GrandTotal, PaymentStatus)
    PK: InvoiceID,  FK: JobCardID → JOB_CARDS

INVOICE_ITEMS (ItemID → InvoiceID, ItemType, Description, Quantity, UnitPrice)
    PK: ItemID,  FK: InvoiceID → INVOICES
```

**2NF is satisfied because:**
- Each relation now has a single-column surrogate primary key (no composite keys remain in the base tables).
- Every non-key attribute is fully functionally dependent on its own table's PK — there are no partial dependencies.

**Remaining problems (for 3NF):**
- Are there any transitive dependencies? (non-key attribute → non-key attribute → PK)

---

### 3.3 Third Normal Form (3NF)

**Rule**: Must be in 2NF AND no non-key attribute may be transitively dependent on the primary key. That is: for every functional dependency `X → Y` where `Y` is a non-key attribute, `X` must be a candidate key (superkey) of the relation.

**Analysis of each table:**

#### CUSTOMERS
```
CustomerID → CustomerName, CustomerPhone, CustomerAddress, Email, CreatedAt
```
- No non-key attribute depends on another non-key attribute.
- `CustomerPhone` is a business key (unique), not a determinant of other attributes in this table.
- **✅ In 3NF.**

#### VEHICLES
```
VehicleID → CustomerID, RegistrationNo, Make, Model, YearOfMfr, FuelType, Color, OdometerKm, CreatedAt
```
- `Make` does not determine `Model` in the schema (make = brand, model = product; a brand has many models, so no FD here).
- `RegistrationNo` is a candidate key (unique constraint), but all other attributes depend on `VehicleID`, not on each other.
- **✅ In 3NF.** (If a full BCNF analysis were required: both `VehicleID` and `RegistrationNo` are superkeys; no non-superkey FD exists.)

#### SERVICES
```
ServiceID → ServiceName, Description, LabourRate, RateType, CreatedAt
```
- `ServiceName` is a candidate key (unique constraint) — it could be a determinant. All attributes depend on the identity of the service concept, whether identified by `ServiceID` or `ServiceName`.
- `RateType` does not determine `LabourRate` (same rate type can have many different rates).
- **✅ In 3NF.**

#### JOB_CARDS
```
JobCardID → VehicleID, ServiceID, TechnicianName, Status,
            AppointmentDt, StartDt, CompletionDt, DeliveryDt,
            LabourHours, Remarks, CreatedAt, UpdatedAt
```
- `VehicleID` and `ServiceID` are FKs (not attributes that determine other attributes in this table).
- `TechnicianName` is informational; no other attribute depends on it.
- `Status` does not determine any timestamps; the application updates each timestamp independently.
- **✅ In 3NF.**

> **Note on `TechnicianName`**: If technician management were in scope (e.g., a `TECHNICIANS` table with employee ID, certifications, hourly rate), `TechnicianName` would be moved to a separate table. In the current scope, technicians are informational strings, not managed entities.

#### INVOICES
```
InvoiceID → JobCardID, InvoiceDate, LabourTotal, PartsTotal,
             TaxPercent, TaxAmount, GrandTotal, PaymentStatus, PaymentMode, Notes
```
- `TaxAmount = (LabourTotal + PartsTotal) × TaxPercent / 100` — this appears to be a transitive computation. 

**Justification for storing computed totals (controlled denormalization):**
> In pure 3NF, `TaxAmount` and `GrandTotal` would be derived and not stored. However, invoices are **legal financial documents** where the printed total must be immutable and match what the customer signed. If `INVOICE_ITEMS` rows were subsequently corrected, recomputing the total would alter the historical invoice figure. Storing the computed totals is therefore an intentional, documented departure from strict 3NF in favour of **data integrity and auditability** — a standard practice in billing systems.

- Excluding the intentional denormalization of totals, **INVOICES is in 3NF.**

#### INVOICE_ITEMS
```
ItemID → InvoiceID, ItemType, Description, Quantity, UnitPrice, LineTotal (VIRTUAL)
```
- `LineTotal` is a **virtual/computed column** (`QUANTITY × UNIT_PRICE`) — it is not stored physically; Oracle computes it on read.
- No non-key attribute determines any other non-key attribute.
- **✅ In 3NF.**

---

### 3.4 Summary — Normalization Outcome

| Table | 1NF | 2NF | 3NF | Notes |
|---|---|---|---|---|
| CUSTOMERS | ✅ | ✅ | ✅ | Fully normalized |
| VEHICLES | ✅ | ✅ | ✅ | Fully normalized |
| SERVICES | ✅ | ✅ | ✅ | Fully normalized |
| JOB_CARDS | ✅ | ✅ | ✅ | Technician is informational string in scope |
| INVOICES | ✅ | ✅ | ✅* | Intentional stored totals for auditability |
| INVOICE_ITEMS | ✅ | ✅ | ✅ | Virtual LINE_TOTAL is not stored |

> **Conclusion**: All six tables in the VSMS schema satisfy the requirements of Third Normal Form (3NF). The single intentional departure from strict 3NF in INVOICES (stored computed totals) is justified by financial document immutability and is explicitly documented per normalization best practice.

---

## 4. Indexes

| Index Name | Table | Column(s) | Purpose |
|---|---|---|---|
| `IDX_VEHICLES_CUSTOMER` | VEHICLES | CUSTOMER_ID | Join performance on customer→vehicle lookup |
| `IDX_JOB_CARDS_VEHICLE` | JOB_CARDS | VEHICLE_ID | Service history queries per vehicle |
| `IDX_JOB_CARDS_STATUS` | JOB_CARDS | STATUS | Dashboard filter by job card status |
| `IDX_JOB_CARDS_APPT_DT` | JOB_CARDS | APPOINTMENT_DT | Appointment calendar queries |
| `IDX_INV_ITEMS_INVOICE` | INVOICE_ITEMS | INVOICE_ID | Line item fetch per invoice |
| `IDX_CUSTOMERS_PHONE` | CUSTOMERS | PHONE | Customer lookup by phone (common entry point) |

---

## 5. Seed Data Summary

| Table | Rows | Description |
|---|---|---|
| SERVICES | 10 | Full service catalogue (oil change, brakes, AC, etc.) |
| CUSTOMERS | 6 | Realistic Indian customer profiles with phone/email |
| VEHICLES | 7 | Mix of petrol, diesel, and electric vehicles |
| JOB_CARDS | 7 | Covers all 4 status states for full demo coverage |
| INVOICES | 4 | 3 paid + 1 pending, with GST computed |
| INVOICE_ITEMS | 13 | Labour + parts breakdowns for completed jobs |
