/**
 * BillingService — Business logic for invoice generation, part addition,
 * total recomputation, and payment marking.
 */
package com.garage.service;

import com.garage.dao.InvoiceDAO;
import com.garage.dao.InvoiceItemDAO;
import com.garage.dao.JobCardDAO;
import com.garage.dao.ServiceDAO;
import com.garage.model.Invoice;
import com.garage.model.InvoiceItem;
import com.garage.model.JobCard;
import com.garage.model.Service;
import com.garage.util.DBConnection;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Implements the billing workflow:
 * <ol>
 *   <li>Generate an invoice from a completed job card.</li>
 *   <li>Add spare part line items to an existing invoice.</li>
 *   <li>Recompute totals (labour + parts + 18% GST = grand total).</li>
 *   <li>Mark an invoice as paid with a given payment mode.</li>
 * </ol>
 *
 * <p>SQLExceptions are wrapped in RuntimeExceptions so Swing callers
 * handle only unchecked exceptions inside {@code SwingWorker.done()}.
 */
public class BillingService {

    /** GST percentage applied to all invoices. */
    private static final double DEFAULT_TAX_PERCENT = 18.0;

    private final InvoiceDAO     invoiceDao     = new InvoiceDAO();
    private final InvoiceItemDAO invoiceItemDao = new InvoiceItemDAO();
    private final JobCardDAO     jobCardDao     = new JobCardDAO();
    private final ServiceDAO     serviceDao     = new ServiceDAO();

    // ── Public Methods ────────────────────────────────────────────────────────

    /**
     * Generates a new invoice for the given job card.
     *
     * <p>Business rules:
     * <ul>
     *   <li>Only one invoice may exist per job card (enforced by DB UNIQUE constraint
     *       and pre-checked here for a user-friendly error message).</li>
     *   <li>Labour total is computed from the service's RATE_TYPE and LABOUR_HOURS.</li>
     *   <li>Initial PARTS_TOTAL = 0.</li>
     *   <li>TAX_PERCENT = 18 (GST).</li>
     * </ul>
     *
     * @param jobCardId the JOB_CARD_ID for which to generate an invoice
     * @return the created Invoice with its generated ID populated
     * @throws IllegalArgumentException if the job card does not exist,
     *                                  or an invoice already exists for it
     * @throws RuntimeException         wrapping any SQLException
     */
    public Invoice generateInvoice(int jobCardId) {
        Connection conn = DBConnection.getConnection();

        // ── Guard: check for duplicate invoice ───────────────────────────────
        try {
            Optional<Invoice> existing = invoiceDao.findByJobCardId(conn, jobCardId);
            if (existing.isPresent()) {
                throw new IllegalArgumentException(
                    "BillingService.generateInvoice: An invoice already exists for Job Card #" +
                    jobCardId + " (Invoice #" + existing.get().getInvoiceId() + "). " +
                    "Cannot generate a duplicate.");
            }
        } catch (SQLException e) {
            throw new RuntimeException(
                "BillingService.generateInvoice: Failed to check for existing invoice. " +
                "Cause: " + e.getMessage(), e);
        }

        // ── Fetch job card ────────────────────────────────────────────────────
        JobCard jobCard;
        try {
            jobCard = jobCardDao.findById(conn, jobCardId)
                .orElseThrow(() -> new IllegalArgumentException(
                    "BillingService.generateInvoice: No job card found with id=" + jobCardId));
        } catch (SQLException e) {
            throw new RuntimeException(
                "BillingService.generateInvoice: Failed to load job card id=" + jobCardId +
                ". Cause: " + e.getMessage(), e);
        }

        // ── Fetch service catalogue entry ─────────────────────────────────────
        Service service;
        try {
            service = serviceDao.findById(conn, jobCard.getServiceId())
                .orElseThrow(() -> new IllegalArgumentException(
                    "BillingService.generateInvoice: Service id=" + jobCard.getServiceId() +
                    " not found in catalogue."));
        } catch (SQLException e) {
            throw new RuntimeException(
                "BillingService.generateInvoice: Failed to load service id=" +
                jobCard.getServiceId() + ". Cause: " + e.getMessage(), e);
        }

        // ── Compute initial labour total ──────────────────────────────────────
        double labourTotal;
        double labourQty;
        double labourUnitPrice;
        String labourDescription;

        if ("HOURLY".equals(service.getRateType())) {
            double hours = (jobCard.getLabourHours() > 0) ? jobCard.getLabourHours() : 1.0;
            labourTotal      = service.getLabourRate() * hours;
            labourQty        = hours;
            labourUnitPrice  = service.getLabourRate();
            labourDescription = service.getServiceName() +
                                " — Labour (" + hours + " hr × ₹" +
                                String.format("%.0f", service.getLabourRate()) + "/hr)";
        } else {
            // FIXED rate
            labourTotal      = service.getLabourRate();
            labourQty        = 1.0;
            labourUnitPrice  = service.getLabourRate();
            labourDescription = service.getServiceName() + " — Fixed Service Charge";
        }

        double partsTotal = 0.0;
        double taxAmount  = (labourTotal + partsTotal) * DEFAULT_TAX_PERCENT / 100.0;
        double grandTotal = labourTotal + partsTotal + taxAmount;

        // ── Insert invoice row ────────────────────────────────────────────────
        Invoice invoice = new Invoice(jobCardId, labourTotal, partsTotal,
                                      DEFAULT_TAX_PERCENT, taxAmount, grandTotal);
        try {
            invoiceDao.insert(conn, invoice);
        } catch (SQLException e) {
            throw new RuntimeException(
                "BillingService.generateInvoice: Failed to insert invoice for job card #" +
                jobCardId + ". Cause: " + e.getMessage(), e);
        }

        // ── Insert initial LABOUR line item ───────────────────────────────────
        InvoiceItem labourItem = new InvoiceItem(
            invoice.getInvoiceId(), "LABOUR",
            labourDescription, labourQty, labourUnitPrice);
        try {
            invoiceItemDao.insert(conn, labourItem);
        } catch (SQLException e) {
            throw new RuntimeException(
                "BillingService.generateInvoice: Failed to insert labour line item for " +
                "invoice #" + invoice.getInvoiceId() + ". Cause: " + e.getMessage(), e);
        }

        return invoice;
    }

    /**
     * Adds a spare part line item to an existing invoice and recomputes totals.
     *
     * @param invoiceId   the INVOICE_ID to add the part to
     * @param description part description (must not be blank)
     * @param qty         quantity (must be > 0)
     * @param unitPrice   unit price in INR (must be >= 0)
     * @return the inserted {@link InvoiceItem} with its generated ID
     * @throws IllegalArgumentException if any validation fails
     * @throws RuntimeException         wrapping any SQLException
     */
    public InvoiceItem addPart(int invoiceId, String description,
                               double qty, double unitPrice) {
        if (description == null || description.trim().isEmpty()) {
            throw new IllegalArgumentException(
                "BillingService.addPart: Part description must not be blank.");
        }
        if (qty <= 0) {
            throw new IllegalArgumentException(
                "BillingService.addPart: Quantity must be greater than zero. Got: " + qty);
        }
        if (unitPrice < 0) {
            throw new IllegalArgumentException(
                "BillingService.addPart: Unit price must be zero or positive. Got: " + unitPrice);
        }

        Connection conn = DBConnection.getConnection();

        InvoiceItem part = new InvoiceItem(invoiceId, "PART",
                                           description.trim(), qty, unitPrice);
        try {
            invoiceItemDao.insert(conn, part);
        } catch (SQLException e) {
            throw new RuntimeException(
                "BillingService.addPart: Failed to insert part '" + description +
                "' for invoice #" + invoiceId + ". Cause: " + e.getMessage(), e);
        }

        recomputeTotals(invoiceId);
        return part;
    }

    /**
     * Recomputes and persists the invoice totals by summing all line items.
     *
     * <p>Formula:
     * <pre>
     *   labourTotal = SUM(LINE_TOTAL) WHERE ITEM_TYPE = 'LABOUR'
     *   partsTotal  = SUM(LINE_TOTAL) WHERE ITEM_TYPE = 'PART'
     *   taxAmount   = (labourTotal + partsTotal) × TAX_PERCENT / 100
     *   grandTotal  = labourTotal + partsTotal + taxAmount
     * </pre>
     * TAX_PERCENT is re-read from the existing invoice row (honours original rate).
     *
     * @param invoiceId the INVOICE_ID to recompute
     * @throws RuntimeException wrapping any SQLException
     */
    public void recomputeTotals(int invoiceId) {
        Connection conn = DBConnection.getConnection();
        try {
            // Get existing invoice to read its TAX_PERCENT
            Invoice existing = invoiceDao.findById(conn, invoiceId)
                .orElseThrow(() -> new IllegalArgumentException(
                    "BillingService.recomputeTotals: No invoice found with id=" + invoiceId));

            double labourTotal = invoiceItemDao.sumByType(conn, invoiceId, "LABOUR");
            double partsTotal  = invoiceItemDao.sumByType(conn, invoiceId, "PART");
            double taxAmount   = (labourTotal + partsTotal) *
                                  existing.getTaxPercent() / 100.0;
            double grandTotal  = labourTotal + partsTotal + taxAmount;

            invoiceDao.updateTotals(conn, invoiceId,
                                    labourTotal, partsTotal, taxAmount, grandTotal);
        } catch (SQLException e) {
            throw new RuntimeException(
                "BillingService.recomputeTotals: Failed to recompute totals for invoice #" +
                invoiceId + ". Cause: " + e.getMessage(), e);
        }
    }

    /**
     * Marks an invoice as paid with the given payment mode.
     *
     * @param invoiceId   the INVOICE_ID to mark paid
     * @param paymentMode the payment method: CASH, CARD, UPI, ONLINE, or CHEQUE
     * @throws IllegalArgumentException if paymentMode is blank or invalid
     * @throws RuntimeException         wrapping any SQLException
     */
    public void markAsPaid(int invoiceId, String paymentMode) {
        if (paymentMode == null || paymentMode.trim().isEmpty()) {
            throw new IllegalArgumentException(
                "BillingService.markAsPaid: Payment mode must not be blank.");
        }
        String mode = paymentMode.trim().toUpperCase();
        List<String> validModes = List.of("CASH", "CARD", "UPI", "ONLINE", "CHEQUE");
        if (!validModes.contains(mode)) {
            throw new IllegalArgumentException(
                "BillingService.markAsPaid: Invalid payment mode '" + paymentMode +
                "'. Must be one of: " + validModes);
        }

        Connection conn = DBConnection.getConnection();
        try {
            invoiceDao.updatePaymentStatus(conn, invoiceId, "PAID", mode);
        } catch (SQLException e) {
            throw new RuntimeException(
                "BillingService.markAsPaid: Failed to mark invoice #" + invoiceId +
                " as paid. Cause: " + e.getMessage(), e);
        }
    }

    /**
     * Finds an invoice by its associated job card ID.
     * Used by the billing panel to load an existing invoice.
     *
     * @param jobCardId the JOB_CARD_ID to look up
     * @return an Optional containing the Invoice, or empty if none exists
     * @throws RuntimeException wrapping any SQLException
     */
    public Optional<Invoice> findByJobCardId(int jobCardId) {
        Connection conn = DBConnection.getConnection();
        try {
            return invoiceDao.findByJobCardId(conn, jobCardId);
        } catch (SQLException e) {
            throw new RuntimeException(
                "BillingService.findByJobCardId: Failed to find invoice for job card #" +
                jobCardId + ". Cause: " + e.getMessage(), e);
        }
    }

    /**
     * Returns all line items for the given invoice.
     *
     * @param invoiceId the INVOICE_ID to retrieve items for
     * @return list of invoice items (may be empty)
     * @throws RuntimeException wrapping any SQLException
     */
    public List<InvoiceItem> getItemsForInvoice(int invoiceId) {
        Connection conn = DBConnection.getConnection();
        try {
            return invoiceItemDao.findByInvoiceId(conn, invoiceId);
        } catch (SQLException e) {
            throw new RuntimeException(
                "BillingService.getItemsForInvoice: Failed to retrieve items for invoice #" +
                invoiceId + ". Cause: " + e.getMessage(), e);
        }
    }

    /**
     * Finds an invoice by its own surrogate primary key.
     *
     * @param invoiceId the INVOICE_ID to retrieve
     * @return an Optional containing the Invoice, or empty if none exists
     * @throws RuntimeException wrapping any SQLException
     */
    public Optional<Invoice> findById(int invoiceId) {
        Connection conn = DBConnection.getConnection();
        try {
            return invoiceDao.findById(conn, invoiceId);
        } catch (SQLException e) {
            throw new RuntimeException(
                "BillingService.findById: Failed to find invoice #" + invoiceId +
                ". Cause: " + e.getMessage(), e);
        }
    }
}
