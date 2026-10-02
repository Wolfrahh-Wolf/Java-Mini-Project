-- =============================================================================
-- db/schema.sql
-- Vehicle Service Management System (VSMS)
-- Oracle Database Free 23c — FREEPDB1 — garage_user schema
--
-- This script is automatically executed by the gvenzl/oracle-free Docker image
-- on first container startup via /container-entrypoint-initdb.d/.
--
-- Execution order:
--   1. Sequences
--   2. Tables (parents before children)
--   3. Triggers (auto-increment via sequence)
--   4. Indexes
--   5. Seed data
-- =============================================================================

-- =============================================================================
-- SECTION 0 — SYSTEM & DB  CONFIGURATION
-- =============================================================================

SET DEFINE OFF;


-- =============================================================================
-- SECTION 1 — DROP OBJECTS (idempotent re-run safety)
-- =============================================================================

-- Drop triggers
BEGIN EXECUTE IMMEDIATE 'DROP TRIGGER INVOICES_TRG';       EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TRIGGER INVOICE_ITEMS_TRG';  EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TRIGGER JOB_CARDS_TRG';      EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TRIGGER SERVICES_TRG';       EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TRIGGER VEHICLES_TRG';       EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TRIGGER CUSTOMERS_TRG';      EXCEPTION WHEN OTHERS THEN NULL; END;
/

-- Drop tables (children first to respect FK constraints)
BEGIN EXECUTE IMMEDIATE 'DROP TABLE INVOICE_ITEMS CASCADE CONSTRAINTS'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TABLE INVOICES CASCADE CONSTRAINTS';      EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TABLE JOB_CARDS CASCADE CONSTRAINTS';     EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TABLE SERVICES CASCADE CONSTRAINTS';      EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TABLE VEHICLES CASCADE CONSTRAINTS';      EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TABLE CUSTOMERS CASCADE CONSTRAINTS';     EXCEPTION WHEN OTHERS THEN NULL; END;
/

-- Drop sequences
BEGIN EXECUTE IMMEDIATE 'DROP SEQUENCE CUSTOMERS_SEQ';     EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP SEQUENCE VEHICLES_SEQ';      EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP SEQUENCE SERVICES_SEQ';      EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP SEQUENCE JOB_CARDS_SEQ';     EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP SEQUENCE INVOICES_SEQ';      EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP SEQUENCE INVOICE_ITEMS_SEQ'; EXCEPTION WHEN OTHERS THEN NULL; END;
/


-- =============================================================================
-- SECTION 2 — SEQUENCES
-- =============================================================================

CREATE SEQUENCE CUSTOMERS_SEQ
    START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

CREATE SEQUENCE VEHICLES_SEQ
    START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

CREATE SEQUENCE SERVICES_SEQ
    START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

CREATE SEQUENCE JOB_CARDS_SEQ
    START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

CREATE SEQUENCE INVOICES_SEQ
    START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

CREATE SEQUENCE INVOICE_ITEMS_SEQ
    START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;


-- =============================================================================
-- SECTION 3 — TABLE DEFINITIONS
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 3.1 CUSTOMERS
--     Stores individual customer contact information.
--     Each customer is uniquely identified by their phone number (business key).
-- -----------------------------------------------------------------------------
CREATE TABLE CUSTOMERS (
    CUSTOMER_ID     NUMBER          NOT NULL,
    CUSTOMER_NAME   VARCHAR2(120)   NOT NULL,
    PHONE           VARCHAR2(15)    NOT NULL,
    EMAIL           VARCHAR2(150),
    ADDRESS         VARCHAR2(300),
    CREATED_AT      TIMESTAMP       DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT PK_CUSTOMERS          PRIMARY KEY (CUSTOMER_ID),
    CONSTRAINT UQ_CUSTOMERS_PHONE    UNIQUE (PHONE),
    CONSTRAINT CK_CUSTOMERS_PHONE    CHECK (REGEXP_LIKE(PHONE, '^\+?[0-9]{7,15}$')),
    CONSTRAINT CK_CUSTOMERS_EMAIL    CHECK (EMAIL IS NULL OR EMAIL LIKE '%@%.%')
);

-- -----------------------------------------------------------------------------
-- 3.2 VEHICLES
--     Each vehicle belongs to exactly one customer.
--     REGISTRATION_NO is the natural business key (number plate).
-- -----------------------------------------------------------------------------
CREATE TABLE VEHICLES (
    VEHICLE_ID       NUMBER          NOT NULL,
    CUSTOMER_ID      NUMBER          NOT NULL,
    REGISTRATION_NO  VARCHAR2(20)    NOT NULL,
    MAKE             VARCHAR2(60)    NOT NULL,   -- e.g., Toyota
    MODEL            VARCHAR2(60)    NOT NULL,   -- e.g., Corolla
    YEAR_OF_MFR      NUMBER(4)       NOT NULL,
    FUEL_TYPE        VARCHAR2(15)    NOT NULL,
    COLOR            VARCHAR2(40),
    ODOMETER_KM      NUMBER(10)      DEFAULT 0,
    CREATED_AT       TIMESTAMP       DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT PK_VEHICLES              PRIMARY KEY (VEHICLE_ID),
    CONSTRAINT FK_VEHICLES_CUSTOMER     FOREIGN KEY (CUSTOMER_ID)
                                            REFERENCES CUSTOMERS (CUSTOMER_ID)
                                            ON DELETE CASCADE,
    CONSTRAINT UQ_VEHICLES_REG          UNIQUE (REGISTRATION_NO),
    CONSTRAINT CK_VEHICLES_YEAR         CHECK (YEAR_OF_MFR BETWEEN 1900 AND 2100),
    CONSTRAINT CK_VEHICLES_ODOMETER     CHECK (ODOMETER_KM >= 0),
    CONSTRAINT CK_VEHICLES_FUEL         CHECK (FUEL_TYPE IN ('PETROL','DIESEL','ELECTRIC','HYBRID','CNG','LPG'))
);

-- -----------------------------------------------------------------------------
-- 3.3 SERVICES (Service Catalogue)
--     Master list of service types offered by the garage.
--     Labour rates are stored here; parts are added per job card via INVOICE_ITEMS.
-- -----------------------------------------------------------------------------
CREATE TABLE SERVICES (
    SERVICE_ID      NUMBER          NOT NULL,
    SERVICE_NAME    VARCHAR2(120)   NOT NULL,
    DESCRIPTION     VARCHAR2(500),
    LABOUR_RATE     NUMBER(10,2)    NOT NULL,   -- Rate per unit (per hour or fixed)
    RATE_TYPE       VARCHAR2(10)    NOT NULL,   -- 'FIXED' or 'HOURLY'
    CREATED_AT      TIMESTAMP       DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT PK_SERVICES              PRIMARY KEY (SERVICE_ID),
    CONSTRAINT UQ_SERVICES_NAME         UNIQUE (SERVICE_NAME),
    CONSTRAINT CK_SERVICES_RATE         CHECK (LABOUR_RATE >= 0),
    CONSTRAINT CK_SERVICES_RATE_TYPE    CHECK (RATE_TYPE IN ('FIXED','HOURLY'))
);

-- -----------------------------------------------------------------------------
-- 3.4 JOB_CARDS
--     Central operational record: one job card per vehicle visit.
--     Tracks status lifecycle, assigned technician, and service details.
-- -----------------------------------------------------------------------------
CREATE TABLE JOB_CARDS (
    JOB_CARD_ID     NUMBER          NOT NULL,
    VEHICLE_ID      NUMBER          NOT NULL,
    SERVICE_ID      NUMBER          NOT NULL,
    TECHNICIAN_NAME VARCHAR2(120),
    STATUS          VARCHAR2(15)    NOT NULL    DEFAULT 'BOOKED',
    APPOINTMENT_DT  TIMESTAMP       NOT NULL,
    START_DT        TIMESTAMP,
    COMPLETION_DT   TIMESTAMP,
    DELIVERY_DT     TIMESTAMP,
    LABOUR_HOURS    NUMBER(6,2)     DEFAULT 0,
    REMARKS         VARCHAR2(1000),
    CREATED_AT      TIMESTAMP       DEFAULT SYSTIMESTAMP NOT NULL,
    UPDATED_AT      TIMESTAMP       DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT PK_JOB_CARDS             PRIMARY KEY (JOB_CARD_ID),
    CONSTRAINT FK_JOB_CARDS_VEHICLE     FOREIGN KEY (VEHICLE_ID)
                                            REFERENCES VEHICLES (VEHICLE_ID)
                                            ON DELETE CASCADE,
    CONSTRAINT FK_JOB_CARDS_SERVICE     FOREIGN KEY (SERVICE_ID)
                                            REFERENCES SERVICES (SERVICE_ID),
    CONSTRAINT CK_JOB_CARDS_STATUS      CHECK (STATUS IN ('BOOKED','IN_PROGRESS','COMPLETED','DELIVERED')),
    CONSTRAINT CK_JOB_CARDS_HOURS       CHECK (LABOUR_HOURS >= 0)
);

-- -----------------------------------------------------------------------------
-- 3.5 INVOICES
--     One invoice per job card (1:1). Computed totals are stored after
--     all items are added (denormalised total for reporting performance —
--     acceptable and documented in DATABASE.md).
-- -----------------------------------------------------------------------------
CREATE TABLE INVOICES (
    INVOICE_ID      NUMBER          NOT NULL,
    JOB_CARD_ID     NUMBER          NOT NULL,
    INVOICE_DATE    TIMESTAMP       DEFAULT SYSTIMESTAMP NOT NULL,
    LABOUR_TOTAL    NUMBER(12,2)    DEFAULT 0   NOT NULL,
    PARTS_TOTAL     NUMBER(12,2)    DEFAULT 0   NOT NULL,
    TAX_PERCENT     NUMBER(5,2)     DEFAULT 18  NOT NULL,   -- GST default 18%
    TAX_AMOUNT      NUMBER(12,2)    DEFAULT 0   NOT NULL,
    GRAND_TOTAL     NUMBER(12,2)    DEFAULT 0   NOT NULL,
    PAYMENT_STATUS  VARCHAR2(15)    DEFAULT 'PENDING' NOT NULL,
    PAYMENT_MODE    VARCHAR2(20),
    NOTES           VARCHAR2(500),
    CONSTRAINT PK_INVOICES              PRIMARY KEY (INVOICE_ID),
    CONSTRAINT FK_INVOICES_JOB_CARD     FOREIGN KEY (JOB_CARD_ID)
                                            REFERENCES JOB_CARDS (JOB_CARD_ID)
                                            ON DELETE CASCADE,
    CONSTRAINT UQ_INVOICES_JOB_CARD     UNIQUE (JOB_CARD_ID),
    CONSTRAINT CK_INVOICES_PAY_STATUS   CHECK (PAYMENT_STATUS IN ('PENDING','PAID','WAIVED')),
    CONSTRAINT CK_INVOICES_PAY_MODE     CHECK (PAYMENT_MODE IS NULL OR
                                               PAYMENT_MODE IN ('CASH','CARD','UPI','ONLINE','CHEQUE')),
    CONSTRAINT CK_INVOICES_TAX          CHECK (TAX_PERCENT BETWEEN 0 AND 100)
);

-- -----------------------------------------------------------------------------
-- 3.6 INVOICE_ITEMS (formerly PARTS / SERVICE_ITEMS)
--     Line items on an invoice: both labour charges and spare parts.
--     ITEM_TYPE distinguishes between 'LABOUR' and 'PART'.
-- -----------------------------------------------------------------------------
CREATE TABLE INVOICE_ITEMS (
    ITEM_ID         NUMBER          NOT NULL,
    INVOICE_ID      NUMBER          NOT NULL,
    ITEM_TYPE       VARCHAR2(10)    NOT NULL,    -- 'LABOUR' or 'PART'
    DESCRIPTION     VARCHAR2(200)   NOT NULL,
    QUANTITY        NUMBER(8,2)     NOT NULL,
    UNIT_PRICE      NUMBER(12,2)    NOT NULL,
    LINE_TOTAL      NUMBER(12,2)    GENERATED ALWAYS AS (QUANTITY * UNIT_PRICE) VIRTUAL,
    CONSTRAINT PK_INVOICE_ITEMS         PRIMARY KEY (ITEM_ID),
    CONSTRAINT FK_INVOICE_ITEMS_INV     FOREIGN KEY (INVOICE_ID)
                                            REFERENCES INVOICES (INVOICE_ID)
                                            ON DELETE CASCADE,
    CONSTRAINT CK_INVOICE_ITEMS_TYPE    CHECK (ITEM_TYPE IN ('LABOUR','PART')),
    CONSTRAINT CK_INVOICE_ITEMS_QTY     CHECK (QUANTITY > 0),
    CONSTRAINT CK_INVOICE_ITEMS_PRICE   CHECK (UNIT_PRICE >= 0)
);


-- =============================================================================
-- SECTION 4 — AUTO-INCREMENT TRIGGERS (Sequence → PK)
-- =============================================================================

CREATE OR REPLACE TRIGGER CUSTOMERS_TRG
    BEFORE INSERT ON CUSTOMERS
    FOR EACH ROW
BEGIN
    IF :NEW.CUSTOMER_ID IS NULL THEN
        :NEW.CUSTOMER_ID := CUSTOMERS_SEQ.NEXTVAL;
    END IF;
END;
/

CREATE OR REPLACE TRIGGER VEHICLES_TRG
    BEFORE INSERT ON VEHICLES
    FOR EACH ROW
BEGIN
    IF :NEW.VEHICLE_ID IS NULL THEN
        :NEW.VEHICLE_ID := VEHICLES_SEQ.NEXTVAL;
    END IF;
END;
/

CREATE OR REPLACE TRIGGER SERVICES_TRG
    BEFORE INSERT ON SERVICES
    FOR EACH ROW
BEGIN
    IF :NEW.SERVICE_ID IS NULL THEN
        :NEW.SERVICE_ID := SERVICES_SEQ.NEXTVAL;
    END IF;
END;
/

CREATE OR REPLACE TRIGGER JOB_CARDS_TRG
    BEFORE INSERT ON JOB_CARDS
    FOR EACH ROW
BEGIN
    IF :NEW.JOB_CARD_ID IS NULL THEN
        :NEW.JOB_CARD_ID := JOB_CARDS_SEQ.NEXTVAL;
    END IF;
END;
/

CREATE OR REPLACE TRIGGER INVOICES_TRG
    BEFORE INSERT ON INVOICES
    FOR EACH ROW
BEGIN
    IF :NEW.INVOICE_ID IS NULL THEN
        :NEW.INVOICE_ID := INVOICES_SEQ.NEXTVAL;
    END IF;
END;
/

CREATE OR REPLACE TRIGGER INVOICE_ITEMS_TRG
    BEFORE INSERT ON INVOICE_ITEMS
    FOR EACH ROW
BEGIN
    IF :NEW.ITEM_ID IS NULL THEN
        :NEW.ITEM_ID := INVOICE_ITEMS_SEQ.NEXTVAL;
    END IF;
END;
/

-- Trigger: auto-update UPDATED_AT on JOB_CARDS row modification
CREATE OR REPLACE TRIGGER JOB_CARDS_AUDIT_TRG
    BEFORE UPDATE ON JOB_CARDS
    FOR EACH ROW
BEGIN
    :NEW.UPDATED_AT := SYSTIMESTAMP;
END;
/


-- =============================================================================
-- SECTION 5 — INDEXES (performance for common query patterns)
-- =============================================================================

-- Look up vehicles by customer
CREATE INDEX IDX_VEHICLES_CUSTOMER ON VEHICLES (CUSTOMER_ID);

-- Look up job cards by vehicle and status
CREATE INDEX IDX_JOB_CARDS_VEHICLE  ON JOB_CARDS (VEHICLE_ID);
CREATE INDEX IDX_JOB_CARDS_STATUS   ON JOB_CARDS (STATUS);
CREATE INDEX IDX_JOB_CARDS_APPT_DT  ON JOB_CARDS (APPOINTMENT_DT);

-- Invoice item lookup by invoice
CREATE INDEX IDX_INV_ITEMS_INVOICE  ON INVOICE_ITEMS (INVOICE_ID);

-- Customer lookup by phone
CREATE INDEX IDX_CUSTOMERS_PHONE    ON CUSTOMERS (PHONE);


-- =============================================================================
-- SECTION 6 — SEED DATA (realistic demo data)
-- =============================================================================

-- ── 6.1 Service Catalogue ────────────────────────────────────────────────────
INSERT INTO SERVICES (SERVICE_NAME, DESCRIPTION, LABOUR_RATE, RATE_TYPE)
VALUES ('Full Service', 'Comprehensive vehicle inspection, oil change, filter replacement, fluid top-up', 1200, 'FIXED');

INSERT INTO SERVICES (SERVICE_NAME, DESCRIPTION, LABOUR_RATE, RATE_TYPE)
VALUES ('Oil & Filter Change', 'Engine oil drain and refill with OEM filter replacement', 450, 'FIXED');

INSERT INTO SERVICES (SERVICE_NAME, DESCRIPTION, LABOUR_RATE, RATE_TYPE)
VALUES ('Brake Inspection & Replacement', 'Inspection of brake pads, discs, and calipers; replacement if worn', 350, 'HOURLY');

INSERT INTO SERVICES (SERVICE_NAME, DESCRIPTION, LABOUR_RATE, RATE_TYPE)
VALUES ('Tyre Rotation & Balancing', 'Rotate all four tyres and balance wheels', 600, 'FIXED');

INSERT INTO SERVICES (SERVICE_NAME, DESCRIPTION, LABOUR_RATE, RATE_TYPE)
VALUES ('AC Service & Recharge', 'Air conditioning system check, refrigerant recharge, filter cleaning', 900, 'FIXED');

INSERT INTO SERVICES (SERVICE_NAME, DESCRIPTION, LABOUR_RATE, RATE_TYPE)
VALUES ('Battery Replacement', 'Battery health check and replacement', 200, 'FIXED');

INSERT INTO SERVICES (SERVICE_NAME, DESCRIPTION, LABOUR_RATE, RATE_TYPE)
VALUES ('Engine Diagnostics', 'OBD-II scan, fault code reading, report generation', 500, 'FIXED');

INSERT INTO SERVICES (SERVICE_NAME, DESCRIPTION, LABOUR_RATE, RATE_TYPE)
VALUES ('Wheel Alignment', '4-wheel computerised alignment check and adjustment', 700, 'FIXED');

INSERT INTO SERVICES (SERVICE_NAME, DESCRIPTION, LABOUR_RATE, RATE_TYPE)
VALUES ('General Repair', 'Ad-hoc mechanical repair charged per technician-hour', 300, 'HOURLY');

INSERT INTO SERVICES (SERVICE_NAME, DESCRIPTION, LABOUR_RATE, RATE_TYPE)
VALUES ('Windshield & Glass', 'Chip repair, crack sealing, or full glass replacement', 400, 'HOURLY');

-- ── 6.2 Customers ────────────────────────────────────────────────────────────
INSERT INTO CUSTOMERS (CUSTOMER_NAME, PHONE, EMAIL, ADDRESS)
VALUES ('Rajan Mehta', '9876543210', 'rajan.mehta@email.com', '12, Lake View Road, Chennai - 600028');

INSERT INTO CUSTOMERS (CUSTOMER_NAME, PHONE, EMAIL, ADDRESS)
VALUES ('Priya Subramaniam', '9123456780', 'priya.subra@gmail.com', '45B, Anna Nagar East, Chennai - 600102');

INSERT INTO CUSTOMERS (CUSTOMER_NAME, PHONE, EMAIL, ADDRESS)
VALUES ('Anand Krishnamurthy', '9988776655', 'anand.k@outlook.com', '7, Nehru Street, Coimbatore - 641001');

INSERT INTO CUSTOMERS (CUSTOMER_NAME, PHONE, EMAIL, ADDRESS)
VALUES ('Divya Nair', '8877665544', 'divya.nair@yahoo.com', '3rd Floor, Sunrise Apts, Kochi - 682001');

INSERT INTO CUSTOMERS (CUSTOMER_NAME, PHONE, EMAIL, ADDRESS)
VALUES ('Suresh Patel', '7766554433', 'suresh.patel@gmail.com', '22, MG Road, Bengaluru - 560001');

INSERT INTO CUSTOMERS (CUSTOMER_NAME, PHONE, EMAIL, ADDRESS)
VALUES ('Meena Iyer', '6655443322', NULL, '8, Gandhi Nagar, Madurai - 625001');

-- ── 6.3 Vehicles ─────────────────────────────────────────────────────────────
-- Customer 1 — Rajan Mehta
INSERT INTO VEHICLES (CUSTOMER_ID, REGISTRATION_NO, MAKE, MODEL, YEAR_OF_MFR, FUEL_TYPE, COLOR, ODOMETER_KM)
VALUES (1, 'TN09AB1234', 'Maruti Suzuki', 'Swift Dzire', 2020, 'PETROL', 'Pearl White', 42500);

INSERT INTO VEHICLES (CUSTOMER_ID, REGISTRATION_NO, MAKE, MODEL, YEAR_OF_MFR, FUEL_TYPE, COLOR, ODOMETER_KM)
VALUES (1, 'TN09CD5678', 'Honda', 'City', 2018, 'PETROL', 'Lunar Silver', 78200);

-- Customer 2 — Priya Subramaniam
INSERT INTO VEHICLES (CUSTOMER_ID, REGISTRATION_NO, MAKE, MODEL, YEAR_OF_MFR, FUEL_TYPE, COLOR, ODOMETER_KM)
VALUES (2, 'TN22EF9012', 'Hyundai', 'Creta', 2022, 'DIESEL', 'Phantom Black', 18700);

-- Customer 3 — Anand Krishnamurthy
INSERT INTO VEHICLES (CUSTOMER_ID, REGISTRATION_NO, MAKE, MODEL, YEAR_OF_MFR, FUEL_TYPE, COLOR, ODOMETER_KM)
VALUES (3, 'TN37GH3456', 'Toyota', 'Fortuner', 2019, 'DIESEL', 'Platinum White Pearl', 95600);

-- Customer 4 — Divya Nair
INSERT INTO VEHICLES (CUSTOMER_ID, REGISTRATION_NO, MAKE, MODEL, YEAR_OF_MFR, FUEL_TYPE, COLOR, ODOMETER_KM)
VALUES (4, 'KL07IJ7890', 'Tata', 'Nexon EV', 2023, 'ELECTRIC', 'Flame Red', 11200);

-- Customer 5 — Suresh Patel
INSERT INTO VEHICLES (CUSTOMER_ID, REGISTRATION_NO, MAKE, MODEL, YEAR_OF_MFR, FUEL_TYPE, COLOR, ODOMETER_KM)
VALUES (5, 'KA05KL1122', 'Mahindra', 'Scorpio-N', 2021, 'DIESEL', 'Dazzling Silver', 56800);

-- Customer 6 — Meena Iyer
INSERT INTO VEHICLES (CUSTOMER_ID, REGISTRATION_NO, MAKE, MODEL, YEAR_OF_MFR, FUEL_TYPE, COLOR, ODOMETER_KM)
VALUES (6, 'TN58MN3344', 'Ford', 'EcoSport', 2017, 'PETROL', 'Deep Impact Blue', 103400);

-- ── 6.4 Job Cards ────────────────────────────────────────────────────────────
-- JC-1: Rajan's Swift — Full Service — DELIVERED
INSERT INTO JOB_CARDS (VEHICLE_ID, SERVICE_ID, TECHNICIAN_NAME, STATUS,
                        APPOINTMENT_DT, START_DT, COMPLETION_DT, DELIVERY_DT, LABOUR_HOURS, REMARKS)
VALUES (1, 1, 'Karthik R.', 'DELIVERED',
        TIMESTAMP '2026-09-01 09:00:00',
        TIMESTAMP '2026-09-01 09:30:00',
        TIMESTAMP '2026-09-01 13:00:00',
        TIMESTAMP '2026-09-01 14:00:00',
        3.5, 'Replaced air filter and cabin filter. Oil change done with 5W-30 synthetic.');

-- JC-2: Rajan's City — Brake Inspection — COMPLETED
INSERT INTO JOB_CARDS (VEHICLE_ID, SERVICE_ID, TECHNICIAN_NAME, STATUS,
                        APPOINTMENT_DT, START_DT, COMPLETION_DT, LABOUR_HOURS, REMARKS)
VALUES (2, 3, 'Murugan S.', 'COMPLETED',
        TIMESTAMP '2026-09-20 10:00:00',
        TIMESTAMP '2026-09-20 10:15:00',
        TIMESTAMP '2026-09-20 12:30:00',
        2.25, 'Front brake pads replaced. Rear discs within tolerance.');

-- JC-3: Priya's Creta — AC Service — IN_PROGRESS
INSERT INTO JOB_CARDS (VEHICLE_ID, SERVICE_ID, TECHNICIAN_NAME, STATUS,
                        APPOINTMENT_DT, START_DT, LABOUR_HOURS, REMARKS)
VALUES (3, 5, 'Vignesh T.', 'IN_PROGRESS',
        TIMESTAMP '2026-09-27 11:00:00',
        TIMESTAMP '2026-09-27 11:20:00',
        0, 'Refrigerant low; awaiting R134a recharge.');

-- JC-4: Anand's Fortuner — Engine Diagnostics — BOOKED
INSERT INTO JOB_CARDS (VEHICLE_ID, SERVICE_ID, TECHNICIAN_NAME, STATUS,
                        APPOINTMENT_DT, REMARKS)
VALUES (4, 7, NULL, 'BOOKED',
        TIMESTAMP '2026-09-28 09:00:00',
        'Customer reports check-engine light on since last 200 km.');

-- JC-5: Divya's Nexon EV — General Repair — BOOKED
INSERT INTO JOB_CARDS (VEHICLE_ID, SERVICE_ID, TECHNICIAN_NAME, STATUS,
                        APPOINTMENT_DT, REMARKS)
VALUES (5, 9, NULL, 'BOOKED',
        TIMESTAMP '2026-09-29 14:00:00',
        'Noise from front suspension on bumps.');

-- JC-6: Suresh's Scorpio-N — Wheel Alignment — DELIVERED
INSERT INTO JOB_CARDS (VEHICLE_ID, SERVICE_ID, TECHNICIAN_NAME, STATUS,
                        APPOINTMENT_DT, START_DT, COMPLETION_DT, DELIVERY_DT, LABOUR_HOURS, REMARKS)
VALUES (6, 8, 'Karthik R.', 'DELIVERED',
        TIMESTAMP '2026-09-15 08:30:00',
        TIMESTAMP '2026-09-15 09:00:00',
        TIMESTAMP '2026-09-15 10:30:00',
        TIMESTAMP '2026-09-15 11:00:00',
        1.5, 'Camber and toe adjusted to spec.');

-- JC-7: Meena's EcoSport — Oil & Filter Change — DELIVERED
INSERT INTO JOB_CARDS (VEHICLE_ID, SERVICE_ID, TECHNICIAN_NAME, STATUS,
                        APPOINTMENT_DT, START_DT, COMPLETION_DT, DELIVERY_DT, LABOUR_HOURS, REMARKS)
VALUES (7, 2, 'Murugan S.', 'DELIVERED',
        TIMESTAMP '2026-09-10 08:00:00',
        TIMESTAMP '2026-09-10 08:20:00',
        TIMESTAMP '2026-09-10 09:15:00',
        TIMESTAMP '2026-09-10 09:30:00',
        0.9, 'Used 10W-40 semi-synthetic. Next service at 108,400 km.');

-- ── 6.5 Invoices ─────────────────────────────────────────────────────────────
-- Invoice for JC-1 (Full Service — DELIVERED)
INSERT INTO INVOICES (JOB_CARD_ID, INVOICE_DATE, LABOUR_TOTAL, PARTS_TOTAL,
                       TAX_PERCENT, TAX_AMOUNT, GRAND_TOTAL, PAYMENT_STATUS, PAYMENT_MODE)
VALUES (1, TIMESTAMP '2026-09-01 14:00:00',
        1200, 980, 18, 393.20, 2573.20, 'PAID', 'UPI');

-- Invoice for JC-2 (Brake Inspection — COMPLETED, payment pending)
INSERT INTO INVOICES (JOB_CARD_ID, INVOICE_DATE, LABOUR_TOTAL, PARTS_TOTAL,
                       TAX_PERCENT, TAX_AMOUNT, GRAND_TOTAL, PAYMENT_STATUS)
VALUES (2, TIMESTAMP '2026-09-20 12:30:00',
        787.50, 2400, 18, 574.65, 3762.15, 'PENDING');

-- Invoice for JC-6 (Wheel Alignment — DELIVERED)
INSERT INTO INVOICES (JOB_CARD_ID, INVOICE_DATE, LABOUR_TOTAL, PARTS_TOTAL,
                       TAX_PERCENT, TAX_AMOUNT, GRAND_TOTAL, PAYMENT_STATUS, PAYMENT_MODE)
VALUES (6, TIMESTAMP '2026-09-15 11:00:00',
        700, 0, 18, 126, 826, 'PAID', 'CASH');

-- Invoice for JC-7 (Oil & Filter Change — DELIVERED)
INSERT INTO INVOICES (JOB_CARD_ID, INVOICE_DATE, LABOUR_TOTAL, PARTS_TOTAL,
                       TAX_PERCENT, TAX_AMOUNT, GRAND_TOTAL, PAYMENT_STATUS, PAYMENT_MODE)
VALUES (7, TIMESTAMP '2026-09-10 09:30:00',
        450, 1350, 18, 324, 2124, 'PAID', 'CASH');

-- ── 6.6 Invoice Items ────────────────────────────────────────────────────────
-- Items for Invoice 1 (JC-1 Full Service)
INSERT INTO INVOICE_ITEMS (INVOICE_ID, ITEM_TYPE, DESCRIPTION, QUANTITY, UNIT_PRICE)
VALUES (1, 'LABOUR', 'Full Service — Labour (3.5 hrs)', 3.5, 342.86);

INSERT INTO INVOICE_ITEMS (INVOICE_ID, ITEM_TYPE, DESCRIPTION, QUANTITY, UNIT_PRICE)
VALUES (1, 'PART', 'Engine Oil 5W-30 Synthetic (4L)', 1, 680);

INSERT INTO INVOICE_ITEMS (INVOICE_ID, ITEM_TYPE, DESCRIPTION, QUANTITY, UNIT_PRICE)
VALUES (1, 'PART', 'OEM Oil Filter — Maruti', 1, 180);

INSERT INTO INVOICE_ITEMS (INVOICE_ID, ITEM_TYPE, DESCRIPTION, QUANTITY, UNIT_PRICE)
VALUES (1, 'PART', 'Air Filter — Maruti Swift', 1, 120);

-- Items for Invoice 2 (JC-2 Brake Inspection)
INSERT INTO INVOICE_ITEMS (INVOICE_ID, ITEM_TYPE, DESCRIPTION, QUANTITY, UNIT_PRICE)
VALUES (2, 'LABOUR', 'Brake Inspection & Replacement — Labour (2.25 hrs)', 2.25, 350);

INSERT INTO INVOICE_ITEMS (INVOICE_ID, ITEM_TYPE, DESCRIPTION, QUANTITY, UNIT_PRICE)
VALUES (2, 'PART', 'Front Brake Pads Set — Honda City', 1, 1800);

INSERT INTO INVOICE_ITEMS (INVOICE_ID, ITEM_TYPE, DESCRIPTION, QUANTITY, UNIT_PRICE)
VALUES (2, 'PART', 'Brake Cleaning Spray', 2, 300);

-- Items for Invoice 3 (JC-6 Wheel Alignment)
INSERT INTO INVOICE_ITEMS (INVOICE_ID, ITEM_TYPE, DESCRIPTION, QUANTITY, UNIT_PRICE)
VALUES (3, 'LABOUR', 'Wheel Alignment — Fixed Labour', 1, 700);

-- Items for Invoice 4 (JC-7 Oil & Filter Change)
INSERT INTO INVOICE_ITEMS (INVOICE_ID, ITEM_TYPE, DESCRIPTION, QUANTITY, UNIT_PRICE)
VALUES (4, 'LABOUR', 'Oil & Filter Change — Labour', 1, 450);

INSERT INTO INVOICE_ITEMS (INVOICE_ID, ITEM_TYPE, DESCRIPTION, QUANTITY, UNIT_PRICE)
VALUES (4, 'PART', 'Engine Oil 10W-40 Semi-Synthetic (3.5L)', 1, 950);

INSERT INTO INVOICE_ITEMS (INVOICE_ID, ITEM_TYPE, DESCRIPTION, QUANTITY, UNIT_PRICE)
VALUES (4, 'PART', 'OEM Oil Filter — Ford EcoSport', 1, 250);

INSERT INTO INVOICE_ITEMS (INVOICE_ID, ITEM_TYPE, DESCRIPTION, QUANTITY, UNIT_PRICE)
VALUES (4, 'PART', 'Drain Plug Washer', 2, 75);

COMMIT;

-- =============================================================================
-- SECTION 7 — VERIFICATION QUERIES (run manually after initialization)
-- =============================================================================
--
-- SELECT TABLE_NAME, NUM_ROWS FROM USER_TABLES ORDER BY TABLE_NAME;
--
-- SELECT C.CUSTOMER_NAME, V.REGISTRATION_NO, V.MAKE, V.MODEL,
--        JC.STATUS, JC.APPOINTMENT_DT
-- FROM   CUSTOMERS C
-- JOIN   VEHICLES V  ON V.CUSTOMER_ID = C.CUSTOMER_ID
-- JOIN   JOB_CARDS JC ON JC.VEHICLE_ID = V.VEHICLE_ID
-- ORDER  BY JC.APPOINTMENT_DT;
--
-- SELECT I.INVOICE_ID, C.CUSTOMER_NAME, V.REGISTRATION_NO,
--        I.GRAND_TOTAL, I.PAYMENT_STATUS
-- FROM   INVOICES I
-- JOIN   JOB_CARDS JC ON JC.JOB_CARD_ID = I.JOB_CARD_ID
-- JOIN   VEHICLES V   ON V.VEHICLE_ID   = JC.VEHICLE_ID
-- JOIN   CUSTOMERS C  ON C.CUSTOMER_ID  = V.CUSTOMER_ID;
--
-- =============================================================================
