/**
 * CustomerDAO — Data Access Object for the CUSTOMERS table.
 * All SQL operations use PreparedStatement with try-with-resources.
 * Column references always use column names, never ordinal indexes.
 */
package com.garage.dao;

import com.garage.model.Customer;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Provides CRUD operations for {@link Customer} records.
 *
 * <p>Every method receives a {@link Connection} parameter so that the
 * service layer can control transaction boundaries (setAutoCommit /
 * commit / rollback) across multiple DAO calls.
 */
public class CustomerDAO {

    // ── SQL Constants ─────────────────────────────────────────────────────────

    private static final String SQL_INSERT =
            "INSERT INTO CUSTOMERS (CUSTOMER_NAME, PHONE, EMAIL, ADDRESS) " +
            "VALUES (?, ?, ?, ?)";

    private static final String SQL_FIND_BY_ID =
            "SELECT CUSTOMER_ID, CUSTOMER_NAME, PHONE, EMAIL, ADDRESS, CREATED_AT " +
            "FROM CUSTOMERS WHERE CUSTOMER_ID = ?";

    private static final String SQL_FIND_BY_PHONE =
            "SELECT CUSTOMER_ID, CUSTOMER_NAME, PHONE, EMAIL, ADDRESS, CREATED_AT " +
            "FROM CUSTOMERS WHERE PHONE = ?";

    private static final String SQL_FIND_ALL =
            "SELECT CUSTOMER_ID, CUSTOMER_NAME, PHONE, EMAIL, ADDRESS, CREATED_AT " +
            "FROM CUSTOMERS ORDER BY CUSTOMER_ID";

    private static final String SQL_FIND_BY_NAME =
            "SELECT CUSTOMER_ID, CUSTOMER_NAME, PHONE, EMAIL, ADDRESS, CREATED_AT " +
            "FROM CUSTOMERS WHERE UPPER(CUSTOMER_NAME) LIKE UPPER(?) ORDER BY CUSTOMER_NAME";

    // ── Public Methods ────────────────────────────────────────────────────────

    /**
     * Inserts a new customer record and returns the generated primary key.
     *
     * @param conn     active JDBC connection (transaction controlled by caller)
     * @param customer the customer to insert (customerId field is ignored)
     * @return the generated CUSTOMER_ID assigned by Oracle
     * @throws SQLException if the INSERT fails (e.g., duplicate phone)
     */
    public int insert(Connection conn, Customer customer) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SQL_INSERT,
                new String[]{"CUSTOMER_ID"})) {

            ps.setString(1, customer.getCustomerName());
            ps.setString(2, customer.getPhone());
            ps.setString(3, customer.getEmail());
            ps.setString(4, customer.getAddress());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int generatedId = keys.getInt(1);
                    customer.setCustomerId(generatedId);
                    return generatedId;
                }
                throw new SQLException(
                    "CustomerDAO.insert: INSERT succeeded but no generated key was returned.");
            }
        }
    }

    /**
     * Finds a customer by their surrogate primary key.
     *
     * @param conn       active JDBC connection
     * @param customerId the CUSTOMER_ID to look up
     * @return an Optional containing the Customer, or empty if not found
     * @throws SQLException if the SELECT fails
     */
    public Optional<Customer> findById(Connection conn, int customerId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_ID)) {
            ps.setInt(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Finds a customer by their phone number (unique business key).
     *
     * @param conn  active JDBC connection
     * @param phone the phone number to search for
     * @return an Optional containing the Customer, or empty if not found
     * @throws SQLException if the SELECT fails
     */
    public Optional<Customer> findByPhone(Connection conn, String phone) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_PHONE)) {
            ps.setString(1, phone.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Returns all customers ordered by CUSTOMER_ID ascending.
     *
     * @param conn active JDBC connection
     * @return list of all customers (empty list if none exist)
     * @throws SQLException if the SELECT fails
     */
    public List<Customer> findAll(Connection conn) throws SQLException {
        List<Customer> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(SQL_FIND_ALL);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }
        return list;
    }

    /**
     * Searches for customers whose name contains the given text (case-insensitive).
     *
     * @param conn       active JDBC connection
     * @param nameSearch partial name to search for
     * @return list of matching customers
     * @throws SQLException if the SELECT fails
     */
    public List<Customer> findByNameLike(Connection conn, String nameSearch) throws SQLException {
        List<Customer> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_NAME)) {
            ps.setString(1, "%" + nameSearch.trim() + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    // ── Private Helpers ───────────────────────────────────────────────────────

    /**
     * Maps a single ResultSet row to a {@link Customer} object.
     * Always references columns by name per AGENTS.md §3.5.
     *
     * @param rs the ResultSet positioned at the row to map
     * @return a populated Customer instance
     * @throws SQLException if any column access fails
     */
    private Customer mapRow(ResultSet rs) throws SQLException {
        int    id        = rs.getInt("CUSTOMER_ID");
        String name      = rs.getString("CUSTOMER_NAME");
        String phone     = rs.getString("PHONE");
        String email     = rs.getString("EMAIL");
        String address   = rs.getString("ADDRESS");
        Timestamp ts     = rs.getTimestamp("CREATED_AT");
        LocalDateTime createdAt = (ts != null) ? ts.toLocalDateTime() : null;

        return new Customer(id, name, phone, email, address, createdAt);
    }
}
