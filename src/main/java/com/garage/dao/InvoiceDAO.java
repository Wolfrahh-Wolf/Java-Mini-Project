/**
 * InvoiceDAO — Data Access Object for the INVOICES table.
 * All SQL operations use PreparedStatement with try-with-resources.
 * Column references always use column names, never ordinal indexes.
 *
 * Course  : UIT3361 OOP Java / UIT3311 Database Technology Lab
 * Project : Vehicle Service Management System
 */
package com.garage.dao;

import com.garage.model.Invoice;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Provides CRUD and update operations for {@link Invoice} records.
 *
 * <p>Every method receives a {@link Connection} parameter so the service layer
 * controls transaction boundaries (setAutoCommit / commit / rollback).
 */
public class InvoiceDAO {

    // ── SQL Constants ─────────────────────────────────────────────────────────

    private static final String SQL_INSERT =
            "INSERT INTO INVOICES " +
            "(JOB_CARD_ID, LABOUR_TOTAL, PARTS_TOTAL, TAX_PERCENT, TAX_AMOUNT, GRAND_TOTAL, " +
            " PAYMENT_STATUS, PAYMENT_MODE, NOTES) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String SQL_FIND_BY_ID =
            "SELECT INVOICE_ID, JOB_CARD_ID, INVOICE_DATE, LABOUR_TOTAL, PARTS_TOTAL, " +
            "TAX_PERCENT, TAX_AMOUNT, GRAND_TOTAL, PAYMENT_STATUS, PAYMENT_MODE, NOTES " +
            "FROM INVOICES WHERE INVOICE_ID = ?";

    private static final String SQL_FIND_BY_JOB_CARD =
            "SELECT INVOICE_ID, JOB_CARD_ID, INVOICE_DATE, LABOUR_TOTAL, PARTS_TOTAL, " +
            "TAX_PERCENT, TAX_AMOUNT, GRAND_TOTAL, PAYMENT_STATUS, PAYMENT_MODE, NOTES " +
            "FROM INVOICES WHERE JOB_CARD_ID = ?";

    private static final String SQL_UPDATE_TOTALS =
            "UPDATE INVOICES SET LABOUR_TOTAL = ?, PARTS_TOTAL = ?, " +
            "TAX_AMOUNT = ?, GRAND_TOTAL = ? WHERE INVOICE_ID = ?";

    private static final String SQL_UPDATE_PAYMENT =
            "UPDATE INVOICES SET PAYMENT_STATUS = ?, PAYMENT_MODE = ? WHERE INVOICE_ID = ?";

    // ── Public Methods ────────────────────────────────────────────────────────

    /**
     * Inserts a new invoice record and returns the generated primary key.
     *
     * @param conn    active JDBC connection (transaction controlled by caller)
     * @param invoice the invoice to insert (invoiceId field is ignored)
     * @return the generated INVOICE_ID assigned by Oracle
     * @throws SQLException if the INSERT fails (e.g., duplicate job card constraint)
     */
    public int insert(Connection conn, Invoice invoice) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SQL_INSERT,
                new String[]{"INVOICE_ID"})) {

            ps.setInt(   1, invoice.getJobCardId());
            ps.setDouble(2, invoice.getLabourTotal());
            ps.setDouble(3, invoice.getPartsTotal());
            ps.setDouble(4, invoice.getTaxPercent());
            ps.setDouble(5, invoice.getTaxAmount());
            ps.setDouble(6, invoice.getGrandTotal());
            ps.setString(7, invoice.getPaymentStatus());

            if (invoice.getPaymentMode() != null) {
                ps.setString(8, invoice.getPaymentMode());
            } else {
                ps.setNull(8, Types.VARCHAR);
            }
            if (invoice.getNotes() != null) {
                ps.setString(9, invoice.getNotes());
            } else {
                ps.setNull(9, Types.VARCHAR);
            }

            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int generatedId = keys.getInt(1);
                    invoice.setInvoiceId(generatedId);
                    return generatedId;
                }
                throw new SQLException(
                    "InvoiceDAO.insert: INSERT succeeded but no generated key was returned.");
            }
        }
    }

    /**
     * Finds an invoice by its surrogate primary key.
     *
     * @param conn      active JDBC connection
     * @param invoiceId the INVOICE_ID to look up
     * @return an Optional containing the Invoice, or empty if not found
     * @throws SQLException if the SELECT fails
     */
    public Optional<Invoice> findById(Connection conn, int invoiceId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_ID)) {
            ps.setInt(1, invoiceId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Finds an invoice by its associated job card ID.
     * Used to check for duplicate invoice generation and to load billing data.
     *
     * @param conn      active JDBC connection
     * @param jobCardId the JOB_CARD_ID to look up
     * @return an Optional containing the Invoice, or empty if none exists
     * @throws SQLException if the SELECT fails
     */
    public Optional<Invoice> findByJobCardId(Connection conn,
                                              int jobCardId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_JOB_CARD)) {
            ps.setInt(1, jobCardId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Updates the computed total columns after parts are added or removed.
     *
     * @param conn        active JDBC connection
     * @param invoiceId   the INVOICE_ID to update
     * @param labourTotal total of all LABOUR line items
     * @param partsTotal  total of all PART line items
     * @param taxAmount   computed tax (labourTotal + partsTotal × taxPercent / 100)
     * @param grandTotal  overall total
     * @throws SQLException if the UPDATE fails
     */
    public void updateTotals(Connection conn, int invoiceId,
                             double labourTotal, double partsTotal,
                             double taxAmount, double grandTotal) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SQL_UPDATE_TOTALS)) {
            ps.setDouble(1, labourTotal);
            ps.setDouble(2, partsTotal);
            ps.setDouble(3, taxAmount);
            ps.setDouble(4, grandTotal);
            ps.setInt(   5, invoiceId);
            int rows = ps.executeUpdate();
            if (rows == 0) {
                throw new SQLException(
                    "InvoiceDAO.updateTotals: No invoice found with id=" + invoiceId);
            }
        }
    }

    /**
     * Updates the payment status and payment mode when an invoice is settled.
     *
     * @param conn          active JDBC connection
     * @param invoiceId     the INVOICE_ID to update
     * @param paymentStatus "PAID" or "WAIVED"
     * @param paymentMode   the payment method (CASH, CARD, UPI, ONLINE, CHEQUE)
     * @throws SQLException if the UPDATE fails
     */
    public void updatePaymentStatus(Connection conn, int invoiceId,
                                    String paymentStatus,
                                    String paymentMode) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SQL_UPDATE_PAYMENT)) {
            ps.setString(1, paymentStatus);
            if (paymentMode != null) {
                ps.setString(2, paymentMode);
            } else {
                ps.setNull(2, Types.VARCHAR);
            }
            ps.setInt(3, invoiceId);
            int rows = ps.executeUpdate();
            if (rows == 0) {
                throw new SQLException(
                    "InvoiceDAO.updatePaymentStatus: No invoice found with id=" + invoiceId);
            }
        }
    }

    // ── Private Helpers ───────────────────────────────────────────────────────

    /**
     * Maps a single ResultSet row to an {@link Invoice} object.
     * Always references columns by name per AGENTS.md §3.5.
     *
     * @param rs the ResultSet positioned at the row to map
     * @return a populated Invoice instance
     * @throws SQLException if any column access fails
     */
    private Invoice mapRow(ResultSet rs) throws SQLException {
        int    invoiceId     = rs.getInt("INVOICE_ID");
        int    jobCardId     = rs.getInt("JOB_CARD_ID");
        Timestamp ts         = rs.getTimestamp("INVOICE_DATE");
        LocalDateTime invoiceDate = (ts != null) ? ts.toLocalDateTime() : null;
        double labourTotal   = rs.getDouble("LABOUR_TOTAL");
        double partsTotal    = rs.getDouble("PARTS_TOTAL");
        double taxPercent    = rs.getDouble("TAX_PERCENT");
        double taxAmount     = rs.getDouble("TAX_AMOUNT");
        double grandTotal    = rs.getDouble("GRAND_TOTAL");
        String paymentStatus = rs.getString("PAYMENT_STATUS");
        String paymentMode   = rs.getString("PAYMENT_MODE");
        String notes         = rs.getString("NOTES");

        return new Invoice(invoiceId, jobCardId, invoiceDate, labourTotal, partsTotal,
                           taxPercent, taxAmount, grandTotal,
                           paymentStatus, paymentMode, notes);
    }
}
