/**
 * VehicleDAO — Data Access Object for the VEHICLES table.
 * All SQL operations use PreparedStatement with try-with-resources.
 * Column references always use column names, never ordinal indexes.
 *
 * Course  : UIT3361 OOP Java / UIT3311 Database Technology Lab
 * Project : Vehicle Service Management System
 */
package com.garage.dao;

import com.garage.model.Vehicle;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Provides CRUD and query operations for {@link Vehicle} records.
 *
 * <p>Every method receives a {@link Connection} parameter so that the
 * service layer can control transaction boundaries.
 */
public class VehicleDAO {

    // ── SQL Constants ─────────────────────────────────────────────────────────

    private static final String SQL_INSERT =
            "INSERT INTO VEHICLES " +
            "(CUSTOMER_ID, REGISTRATION_NO, MAKE, MODEL, YEAR_OF_MFR, FUEL_TYPE, COLOR, ODOMETER_KM) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String SQL_FIND_BY_ID =
            "SELECT VEHICLE_ID, CUSTOMER_ID, REGISTRATION_NO, MAKE, MODEL, " +
            "YEAR_OF_MFR, FUEL_TYPE, COLOR, ODOMETER_KM, CREATED_AT " +
            "FROM VEHICLES WHERE VEHICLE_ID = ?";

    private static final String SQL_FIND_BY_CUSTOMER =
            "SELECT VEHICLE_ID, CUSTOMER_ID, REGISTRATION_NO, MAKE, MODEL, " +
            "YEAR_OF_MFR, FUEL_TYPE, COLOR, ODOMETER_KM, CREATED_AT " +
            "FROM VEHICLES WHERE CUSTOMER_ID = ? ORDER BY VEHICLE_ID";

    private static final String SQL_FIND_BY_REG =
            "SELECT VEHICLE_ID, CUSTOMER_ID, REGISTRATION_NO, MAKE, MODEL, " +
            "YEAR_OF_MFR, FUEL_TYPE, COLOR, ODOMETER_KM, CREATED_AT " +
            "FROM VEHICLES WHERE UPPER(REGISTRATION_NO) = UPPER(?)";

    private static final String SQL_FIND_ALL =
            "SELECT VEHICLE_ID, CUSTOMER_ID, REGISTRATION_NO, MAKE, MODEL, " +
            "YEAR_OF_MFR, FUEL_TYPE, COLOR, ODOMETER_KM, CREATED_AT " +
            "FROM VEHICLES ORDER BY VEHICLE_ID";

    // ── Public Methods ────────────────────────────────────────────────────────

    /**
     * Inserts a new vehicle record and returns the generated primary key.
     *
     * @param conn    active JDBC connection (transaction controlled by caller)
     * @param vehicle the vehicle to insert (vehicleId field is ignored)
     * @return the generated VEHICLE_ID assigned by Oracle
     * @throws SQLException if the INSERT fails (e.g., duplicate registration)
     */
    public int insert(Connection conn, Vehicle vehicle) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SQL_INSERT,
                new String[]{"VEHICLE_ID"})) {

            ps.setInt(   1, vehicle.getCustomerId());
            ps.setString(2, vehicle.getRegistrationNo().toUpperCase().trim());
            ps.setString(3, vehicle.getMake().trim());
            ps.setString(4, vehicle.getModel().trim());
            ps.setInt(   5, vehicle.getYearOfMfr());
            ps.setString(6, vehicle.getFuelType().toUpperCase().trim());
            ps.setString(7, vehicle.getColor());
            ps.setInt(   8, vehicle.getOdometerKm());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int generatedId = keys.getInt(1);
                    vehicle.setVehicleId(generatedId);
                    return generatedId;
                }
                throw new SQLException(
                    "VehicleDAO.insert: INSERT succeeded but no generated key was returned.");
            }
        }
    }

    /**
     * Finds a vehicle by its surrogate primary key.
     *
     * @param conn      active JDBC connection
     * @param vehicleId the VEHICLE_ID to look up
     * @return an Optional containing the Vehicle, or empty if not found
     * @throws SQLException if the SELECT fails
     */
    public Optional<Vehicle> findById(Connection conn, int vehicleId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_ID)) {
            ps.setInt(1, vehicleId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Returns all vehicles owned by a given customer, ordered by VEHICLE_ID.
     *
     * @param conn       active JDBC connection
     * @param customerId the owning customer's CUSTOMER_ID
     * @return list of vehicles for the customer (empty list if none)
     * @throws SQLException if the SELECT fails
     */
    public List<Vehicle> findByCustomerId(Connection conn, int customerId) throws SQLException {
        List<Vehicle> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_CUSTOMER)) {
            ps.setInt(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    /**
     * Finds a vehicle by its registration number (case-insensitive).
     *
     * @param conn           active JDBC connection
     * @param registrationNo the number plate to search for
     * @return an Optional containing the Vehicle, or empty if not found
     * @throws SQLException if the SELECT fails
     */
    public Optional<Vehicle> findByRegistrationNo(Connection conn,
                                                   String registrationNo) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_REG)) {
            ps.setString(1, registrationNo.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Returns all vehicles in the database, ordered by VEHICLE_ID.
     *
     * @param conn active JDBC connection
     * @return list of all vehicles
     * @throws SQLException if the SELECT fails
     */
    public List<Vehicle> findAll(Connection conn) throws SQLException {
        List<Vehicle> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(SQL_FIND_ALL);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }
        return list;
    }

    // ── Private Helpers ───────────────────────────────────────────────────────

    /**
     * Maps a single ResultSet row to a {@link Vehicle} object.
     * Always references columns by name per AGENTS.md §3.5.
     *
     * @param rs the ResultSet positioned at the row to map
     * @return a populated Vehicle instance
     * @throws SQLException if any column access fails
     */
    private Vehicle mapRow(ResultSet rs) throws SQLException {
        int    vehicleId      = rs.getInt("VEHICLE_ID");
        int    customerId     = rs.getInt("CUSTOMER_ID");
        String registrationNo = rs.getString("REGISTRATION_NO");
        String make           = rs.getString("MAKE");
        String model          = rs.getString("MODEL");
        int    yearOfMfr      = rs.getInt("YEAR_OF_MFR");
        String fuelType       = rs.getString("FUEL_TYPE");
        String color          = rs.getString("COLOR");
        int    odometerKm     = rs.getInt("ODOMETER_KM");
        Timestamp ts          = rs.getTimestamp("CREATED_AT");
        LocalDateTime createdAt = (ts != null) ? ts.toLocalDateTime() : null;

        return new Vehicle(vehicleId, customerId, registrationNo, make, model,
                           yearOfMfr, fuelType, color, odometerKm, createdAt);
    }
}
