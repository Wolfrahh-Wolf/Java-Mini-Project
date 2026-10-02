/**
 * Invoice — POJO representing a billing invoice for a completed service job.
 * Maps directly to the INVOICES table in Oracle.
 *
 * Course  : UIT3361 OOP Java / UIT3311 Database Technology Lab
 * Project : Vehicle Service Management System
 */
package com.garage.model;

import java.time.LocalDateTime;

/**
 * Plain Java object representing an invoice record.
 * One invoice maps to exactly one job card (1:1 relationship enforced by UNIQUE constraint).
 *
 * <p>GRAND_TOTAL = LABOUR_TOTAL + PARTS_TOTAL + TAX_AMOUNT.
 * TAX_AMOUNT = (LABOUR_TOTAL + PARTS_TOTAL) × TAX_PERCENT / 100.
 * These are stored (denormalised) for legal auditability — see DATABASE.md.
 */
public class Invoice {

    private int           invoiceId;
    private int           jobCardId;
    private LocalDateTime invoiceDate;
    private double        labourTotal;
    private double        partsTotal;
    private double        taxPercent;    // default 18 (GST)
    private double        taxAmount;
    private double        grandTotal;
    private String        paymentStatus; // PENDING | PAID | WAIVED
    private String        paymentMode;   // CASH | CARD | UPI | ONLINE | CHEQUE | null
    private String        notes;

    // ── Constructors ──────────────────────────────────────────────────────────

    /** No-arg constructor required for DAO result-set mapping. */
    public Invoice() {}

    /**
     * Constructor for creating a new invoice at generation time (before DB insert).
     *
     * @param jobCardId   the job card this invoice belongs to
     * @param labourTotal initial labour charge
     * @param partsTotal  initial parts total (usually 0 at generation)
     * @param taxPercent  applicable tax rate (e.g., 18.0 for 18% GST)
     * @param taxAmount   computed tax amount
     * @param grandTotal  total including tax
     */
    public Invoice(int jobCardId, double labourTotal, double partsTotal,
                   double taxPercent, double taxAmount, double grandTotal) {
        this.jobCardId     = jobCardId;
        this.labourTotal   = labourTotal;
        this.partsTotal    = partsTotal;
        this.taxPercent    = taxPercent;
        this.taxAmount     = taxAmount;
        this.grandTotal    = grandTotal;
        this.paymentStatus = "PENDING";
    }

    /**
     * Full constructor including all DB-assigned fields (used when mapping from ResultSet).
     *
     * @param invoiceId     surrogate PK
     * @param jobCardId     FK to JOB_CARDS
     * @param invoiceDate   invoice generation timestamp
     * @param labourTotal   total labour charges
     * @param partsTotal    total spare parts charges
     * @param taxPercent    applicable tax percentage
     * @param taxAmount     computed tax amount
     * @param grandTotal    grand total (labour + parts + tax)
     * @param paymentStatus PENDING, PAID, or WAIVED
     * @param paymentMode   payment method (may be null)
     * @param notes         optional invoice notes (may be null)
     */
    public Invoice(int invoiceId, int jobCardId, LocalDateTime invoiceDate,
                   double labourTotal, double partsTotal, double taxPercent,
                   double taxAmount, double grandTotal,
                   String paymentStatus, String paymentMode, String notes) {
        this.invoiceId     = invoiceId;
        this.jobCardId     = jobCardId;
        this.invoiceDate   = invoiceDate;
        this.labourTotal   = labourTotal;
        this.partsTotal    = partsTotal;
        this.taxPercent    = taxPercent;
        this.taxAmount     = taxAmount;
        this.grandTotal    = grandTotal;
        this.paymentStatus = paymentStatus;
        this.paymentMode   = paymentMode;
        this.notes         = notes;
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    /** @return surrogate primary key */
    public int getInvoiceId()         { return invoiceId; }

    /** @return FK referencing the associated job card */
    public int getJobCardId()         { return jobCardId; }

    /** @return invoice generation timestamp */
    public LocalDateTime getInvoiceDate() { return invoiceDate; }

    /** @return total labour charges */
    public double getLabourTotal()    { return labourTotal; }

    /** @return total spare parts charges */
    public double getPartsTotal()     { return partsTotal; }

    /** @return applicable tax percentage (e.g., 18.0) */
    public double getTaxPercent()     { return taxPercent; }

    /** @return computed tax amount */
    public double getTaxAmount()      { return taxAmount; }

    /** @return grand total including all charges and tax */
    public double getGrandTotal()     { return grandTotal; }

    /** @return payment status: PENDING, PAID, or WAIVED */
    public String getPaymentStatus()  { return paymentStatus; }

    /** @return payment mode (may be null if not yet paid) */
    public String getPaymentMode()    { return paymentMode; }

    /** @return optional invoice notes (may be null) */
    public String getNotes()          { return notes; }

    // ── Setters ───────────────────────────────────────────────────────────────

    /** @param invoiceId surrogate PK (set by DAO after insert) */
    public void setInvoiceId(int invoiceId)           { this.invoiceId = invoiceId; }

    /** @param jobCardId FK to JOB_CARDS */
    public void setJobCardId(int jobCardId)           { this.jobCardId = jobCardId; }

    /** @param invoiceDate invoice generation timestamp */
    public void setInvoiceDate(LocalDateTime invoiceDate) { this.invoiceDate = invoiceDate; }

    /** @param labourTotal total labour charges */
    public void setLabourTotal(double labourTotal)    { this.labourTotal = labourTotal; }

    /** @param partsTotal total spare parts charges */
    public void setPartsTotal(double partsTotal)      { this.partsTotal = partsTotal; }

    /** @param taxPercent tax rate percentage */
    public void setTaxPercent(double taxPercent)      { this.taxPercent = taxPercent; }

    /** @param taxAmount computed tax amount */
    public void setTaxAmount(double taxAmount)        { this.taxAmount = taxAmount; }

    /** @param grandTotal grand total */
    public void setGrandTotal(double grandTotal)      { this.grandTotal = grandTotal; }

    /** @param paymentStatus PENDING, PAID, or WAIVED */
    public void setPaymentStatus(String paymentStatus){ this.paymentStatus = paymentStatus; }

    /** @param paymentMode payment method */
    public void setPaymentMode(String paymentMode)    { this.paymentMode = paymentMode; }

    /** @param notes optional invoice notes */
    public void setNotes(String notes)                { this.notes = notes; }

    // ── Utility ───────────────────────────────────────────────────────────────

    @Override
    public String toString() {
        return "Invoice{id=" + invoiceId +
               ", jobCardId=" + jobCardId +
               ", grandTotal=" + grandTotal +
               ", status='" + paymentStatus + '\'' + '}';
    }
}
