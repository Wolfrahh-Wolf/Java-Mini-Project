/**
 * ServiceDAO — Data Access Object for the SERVICES catalogue table.
 * All SQL operations use PreparedStatement with try-with-resources.
 * Column references always use column names, never ordinal indexes.
 *
 * Course  : UIT3361 OOP Java / UIT3311 Database Technology Lab
 * Project : Vehicle Service Management System
 */
package com.garage.dao;

import com.garage.model.Service;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Provides read operations for the {@link Service} catalogue.
 *
 * <p>The service catalogue is managed via seed data in schema.sql;
 * insert/update/delete are not exposed through the application UI
 * in the current academic scope.
 */
public class ServiceDAO {

    // ── SQL Constants ─────────────────────────────────────────────────────────

    private static final String SQL_FIND_ALL =
            "SELECT SERVICE_ID, SERVICE_NAME, DESCRIPTION, LABOUR_RATE, RATE_TYPE, CREATED_AT " +
            "FROM SERVICES ORDER BY SERVICE_ID";

    private static final String SQL_FIND_BY_ID =
            "SELECT SERVICE_ID, SERVICE_NAME, DESCRIPTION, LABOUR_RATE, RATE_TYPE, CREATED_AT " +
            "FROM SERVICES WHERE SERVICE_ID = ?";

    // ── Public Methods ────────────────────────────────────────────────────────

    /**
     * Returns all service catalogue entries ordered by SERVICE_ID.
     * Used to populate the service JComboBox in the booking panel.
     *
     * @param conn active JDBC connection
     * @return list of all services (never null; empty if none configured)
     * @throws SQLException if the SELECT fails
     */
    public List<Service> findAll(Connection conn) throws SQLException {
        List<Service> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(SQL_FIND_ALL);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }
        return list;
    }

    /**
     * Finds a service by its surrogate primary key.
     * Used during invoice generation to fetch the labour rate.
     *
     * @param conn      active JDBC connection
     * @param serviceId the SERVICE_ID to look up
     * @return an Optional containing the Service, or empty if not found
     * @throws SQLException if the SELECT fails
     */
    public Optional<Service> findById(Connection conn, int serviceId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_ID)) {
            ps.setInt(1, serviceId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        }
        return Optional.empty();
    }

    // ── Private Helpers ───────────────────────────────────────────────────────

    /**
     * Maps a single ResultSet row to a {@link Service} object.
     * Always references columns by name per AGENTS.md §3.5.
     *
     * @param rs the ResultSet positioned at the row to map
     * @return a populated Service instance
     * @throws SQLException if any column access fails
     */
    private Service mapRow(ResultSet rs) throws SQLException {
        int    serviceId   = rs.getInt("SERVICE_ID");
        String serviceName = rs.getString("SERVICE_NAME");
        String description = rs.getString("DESCRIPTION");
        double labourRate  = rs.getDouble("LABOUR_RATE");
        String rateType    = rs.getString("RATE_TYPE");
        Timestamp ts       = rs.getTimestamp("CREATED_AT");
        LocalDateTime createdAt = (ts != null) ? ts.toLocalDateTime() : null;

        return new Service(serviceId, serviceName, description, labourRate, rateType, createdAt);
    }
}
