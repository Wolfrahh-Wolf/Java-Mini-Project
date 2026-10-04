/**
 * InvoiceItem — POJO representing a single line item on an invoice.
 * Maps directly to the INVOICE_ITEMS table in Oracle.
 */
package com.garage.model;

/**
 * Plain Java object representing an invoice line item.
 * ITEM_TYPE is either "LABOUR" (labour charge) or "PART" (spare part).
 *
 * <p>NOTE: LINE_TOTAL is a VIRTUAL (computed) column in Oracle:
 * {@code QUANTITY * UNIT_PRICE}. It must NEVER be included in INSERT
 * statements but will be present in ResultSet reads.
 */
public class InvoiceItem {

    private int    itemId;
    private int    invoiceId;
    private String itemType;     // "LABOUR" or "PART"
    private String description;
    private double quantity;
    private double unitPrice;
    private double lineTotal;    // VIRTUAL column — read-only, computed by Oracle

    // ── Constructors ──────────────────────────────────────────────────────────

    /** No-arg constructor required for DAO result-set mapping. */
    public InvoiceItem() {}

    /**
     * Constructor for creating a new invoice item (before DB insert).
     * LINE_TOTAL is omitted — Oracle computes it automatically.
     *
     * @param invoiceId   the invoice this item belongs to
     * @param itemType    "LABOUR" or "PART"
     * @param description human-readable description of the item
     * @param quantity    quantity (hours for labour, units for parts)
     * @param unitPrice   price per unit in INR
     */
    public InvoiceItem(int invoiceId, String itemType, String description,
                       double quantity, double unitPrice) {
        this.invoiceId   = invoiceId;
        this.itemType    = itemType;
        this.description = description;
        this.quantity    = quantity;
        this.unitPrice   = unitPrice;
        // lineTotal is computed by Oracle; set locally for convenience after insert
        this.lineTotal   = quantity * unitPrice;
    }

    /**
     * Full constructor including all DB-read fields (used when mapping from ResultSet).
     *
     * @param itemId      surrogate PK assigned by Oracle sequence
     * @param invoiceId   FK to INVOICES
     * @param itemType    "LABOUR" or "PART"
     * @param description item description
     * @param quantity    quantity
     * @param unitPrice   unit price in INR
     * @param lineTotal   Oracle-computed line total (QUANTITY × UNIT_PRICE)
     */
    public InvoiceItem(int itemId, int invoiceId, String itemType, String description,
                       double quantity, double unitPrice, double lineTotal) {
        this.itemId      = itemId;
        this.invoiceId   = invoiceId;
        this.itemType    = itemType;
        this.description = description;
        this.quantity    = quantity;
        this.unitPrice   = unitPrice;
        this.lineTotal   = lineTotal;
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    /** @return surrogate primary key */
    public int    getItemId()      { return itemId; }

    /** @return FK referencing the owning invoice */
    public int    getInvoiceId()   { return invoiceId; }

    /** @return "LABOUR" or "PART" */
    public String getItemType()    { return itemType; }

    /** @return human-readable description */
    public String getDescription() { return description; }

    /** @return quantity (hours or units) */
    public double getQuantity()    { return quantity; }

    /** @return unit price in INR */
    public double getUnitPrice()   { return unitPrice; }

    /**
     * Returns the line total.
     * This mirrors Oracle's virtual column ({@code QUANTITY * UNIT_PRICE}).
     *
     * @return line total in INR
     */
    public double getLineTotal()   { return lineTotal; }

    // ── Setters ───────────────────────────────────────────────────────────────

    /** @param itemId surrogate PK (set by DAO after insert) */
    public void setItemId(int itemId)           { this.itemId = itemId; }

    /** @param invoiceId FK to INVOICES */
    public void setInvoiceId(int invoiceId)     { this.invoiceId = invoiceId; }

    /** @param itemType "LABOUR" or "PART" */
    public void setItemType(String itemType)    { this.itemType = itemType; }

    /** @param description item description */
    public void setDescription(String description){ this.description = description; }

    /** @param quantity quantity */
    public void setQuantity(double quantity)    { this.quantity = quantity; }

    /** @param unitPrice unit price */
    public void setUnitPrice(double unitPrice)  { this.unitPrice = unitPrice; }

    /** @param lineTotal Oracle-computed line total */
    public void setLineTotal(double lineTotal)  { this.lineTotal = lineTotal; }

    // ── Utility ───────────────────────────────────────────────────────────────

    @Override
    public String toString() {
        return "InvoiceItem{id=" + itemId +
               ", type='" + itemType + '\'' +
               ", desc='" + description + '\'' +
               ", qty=" + quantity +
               ", unitPrice=" + unitPrice +
               ", lineTotal=" + lineTotal + '}';
    }
}
