/**
 * InvoiceItemDAO — Data Access Object for the INVOICE_ITEMS table.
 * All SQL operations use PreparedStatement with try-with-resources.
 * Column references always use column names, never ordinal indexes.
 *
 * IMPORTANT: LINE_TOTAL is a VIRTUAL (computed) column in Oracle.
 * It must NEVER appear in INSERT or UPDATE statements.
 */
package com.garage.dao;

import com.garage.model.InvoiceItem;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Provides insert and retrieval operations for {@link InvoiceItem} records.
 *
 * <p>Every method receives a {@link Connection} parameter so that the
 * service layer can control transaction boundaries.
 */
public class InvoiceItemDAO {

    // ── SQL Constants ─────────────────────────────────────────────────────────

    /**
     * INSERT excludes LINE_TOTAL — it is a VIRTUAL column computed by Oracle
     * as {@code QUANTITY * UNIT_PRICE}.
     */
    private static final String SQL_INSERT =
            "INSERT INTO INVOICE_ITEMS (INVOICE_ID, ITEM_TYPE, DESCRIPTION, QUANTITY, UNIT_PRICE) " +
            "VALUES (?, ?, ?, ?, ?)";

    private static final String SQL_FIND_BY_INVOICE =
            "SELECT ITEM_ID, INVOICE_ID, ITEM_TYPE, DESCRIPTION, QUANTITY, UNIT_PRICE, LINE_TOTAL " +
            "FROM INVOICE_ITEMS WHERE INVOICE_ID = ? ORDER BY ITEM_ID";

    /**
     * Sum query for recomputing invoice totals by item type.
     * Returns 0 (via NVL) when no items of that type exist.
     */
    private static final String SQL_SUM_BY_TYPE =
            "SELECT NVL(SUM(LINE_TOTAL), 0) AS TYPE_TOTAL " +
            "FROM INVOICE_ITEMS WHERE INVOICE_ID = ? AND ITEM_TYPE = ?";

    // ── Public Methods ────────────────────────────────────────────────────────

    /**
     * Inserts a new invoice line item and returns the generated primary key.
     * LINE_TOTAL is intentionally excluded from the INSERT — Oracle computes it.
     *
     * @param conn active JDBC connection (transaction controlled by caller)
     * @param item the item to insert (itemId field is ignored; lineTotal is ignored for insert)
     * @return the generated ITEM_ID assigned by Oracle
     * @throws SQLException if the INSERT fails
     */
    public int insert(Connection conn, InvoiceItem item) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SQL_INSERT,
                new String[]{"ITEM_ID"})) {

            ps.setInt(   1, item.getInvoiceId());
            ps.setString(2, item.getItemType().toUpperCase().trim());
            ps.setString(3, item.getDescription().trim());
            ps.setDouble(4, item.getQuantity());
            ps.setDouble(5, item.getUnitPrice());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int generatedId = keys.getInt(1);
                    item.setItemId(generatedId);
                    // Compute lineTotal locally so caller can use it without a re-fetch
                    item.setLineTotal(item.getQuantity() * item.getUnitPrice());
                    return generatedId;
                }
                throw new SQLException(
                    "InvoiceItemDAO.insert: INSERT succeeded but no generated key was returned.");
            }
        }
    }

    /**
     * Returns all line items for a given invoice, ordered by ITEM_ID ascending.
     *
     * @param conn      active JDBC connection
     * @param invoiceId the INVOICE_ID whose items to retrieve
     * @return list of invoice items (empty if none)
     * @throws SQLException if the SELECT fails
     */
    public List<InvoiceItem> findByInvoiceId(Connection conn,
                                              int invoiceId) throws SQLException {
        List<InvoiceItem> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_INVOICE)) {
            ps.setInt(1, invoiceId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    /**
     * Computes the sum of LINE_TOTAL for all items of a given type in an invoice.
     * Uses Oracle's NVL to return 0 when no items exist.
     *
     * @param conn      active JDBC connection
     * @param invoiceId the INVOICE_ID to aggregate
     * @param itemType  "LABOUR" or "PART"
     * @return the total as a double (0.0 if no items of that type)
     * @throws SQLException if the SELECT fails
     */
    public double sumByType(Connection conn, int invoiceId,
                             String itemType) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SQL_SUM_BY_TYPE)) {
            ps.setInt(   1, invoiceId);
            ps.setString(2, itemType.toUpperCase().trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("TYPE_TOTAL");
                }
            }
        }
        return 0.0;
    }

    // ── Private Helpers ───────────────────────────────────────────────────────

    /**
     * Maps a single ResultSet row to an {@link InvoiceItem} object.
     * Reads LINE_TOTAL from the ResultSet (Oracle virtual column).
     * Always references columns by name per AGENTS.md §3.5.
     *
     * @param rs the ResultSet positioned at the row to map
     * @return a populated InvoiceItem instance
     * @throws SQLException if any column access fails
     */
    private InvoiceItem mapRow(ResultSet rs) throws SQLException {
        int    itemId      = rs.getInt("ITEM_ID");
        int    invoiceId   = rs.getInt("INVOICE_ID");
        String itemType    = rs.getString("ITEM_TYPE");
        String description = rs.getString("DESCRIPTION");
        double quantity    = rs.getDouble("QUANTITY");
        double unitPrice   = rs.getDouble("UNIT_PRICE");
        double lineTotal   = rs.getDouble("LINE_TOTAL");  // Oracle virtual column — safe to read

        return new InvoiceItem(itemId, invoiceId, itemType, description,
                               quantity, unitPrice, lineTotal);
    }
}
