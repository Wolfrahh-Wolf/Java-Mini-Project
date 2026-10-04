/**
 * HistoryService — Business logic for vehicle service history retrieval.
 * Executes a multi-table JOIN across VEHICLES, JOB_CARDS, SERVICES, and INVOICES.
 */
package com.garage.service;

import com.garage.util.DBConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Provides the complete service history for a given vehicle.
 *
 * <p>Uses a single SQL JOIN query across VEHICLES, JOB_CARDS, SERVICES,
 * and INVOICES (LEFT JOIN — invoice may not exist for BOOKED/IN_PROGRESS cards).
 * Returns a flat {@link ServiceHistoryRecord} DTO — no model objects exposed.
 */
public class HistoryService {

    // ── SQL ───────────────────────────────────────────────────────────────────

    /**
     * Multi-table JOIN query as specified in WORK_SPLIT.md Phase 5 Step 1.
     * LEFT JOIN on INVOICES so job cards without invoices are still returned.
     */
    private static final String SQL_HISTORY =
            "SELECT JC.JOB_CARD_ID, " +
            "       JC.APPOINTMENT_DT, " +
            "       JC.STATUS, " +
            "       S.SERVICE_NAME, " +
            "       JC.TECHNICIAN_NAME, " +
            "       JC.LABOUR_HOURS, " +
            "       I.GRAND_TOTAL, " +
            "       I.PAYMENT_STATUS " +
            "FROM   JOB_CARDS JC " +
            "JOIN   VEHICLES  V  ON V.VEHICLE_ID  = JC.VEHICLE_ID " +
            "JOIN   SERVICES  S  ON S.SERVICE_ID  = JC.SERVICE_ID " +
            "LEFT   JOIN INVOICES I ON I.JOB_CARD_ID = JC.JOB_CARD_ID " +
            "WHERE  UPPER(V.REGISTRATION_NO) = UPPER(?) " +
            "ORDER  BY JC.APPOINTMENT_DT DESC";

    private static final String SQL_VEHICLE_SUMMARY =
            "SELECT V.MAKE, V.MODEL, V.YEAR_OF_MFR, V.FUEL_TYPE, " +
            "       C.CUSTOMER_NAME, C.PHONE " +
            "FROM   VEHICLES V " +
            "JOIN   CUSTOMERS C ON C.CUSTOMER_ID = V.CUSTOMER_ID " +
            "WHERE  UPPER(V.REGISTRATION_NO) = UPPER(?)";

    // ── Public Methods ────────────────────────────────────────────────────────

    /**
     * Returns the complete service history for a vehicle identified by its
     * registration number.
     *
     * @param registrationNo the number plate to look up (case-insensitive)
     * @return list of history records, newest visit first (empty if none found)
     * @throws IllegalArgumentException if registrationNo is blank
     * @throws RuntimeException         wrapping any SQLException
     */
    public List<ServiceHistoryRecord> getServiceHistory(String registrationNo) {
        if (registrationNo == null || registrationNo.trim().isEmpty()) {
            throw new IllegalArgumentException(
                "HistoryService: Registration number must not be blank.");
        }

        Connection conn = DBConnection.getConnection();
        List<ServiceHistoryRecord> records = new ArrayList<>();

        try (PreparedStatement ps = conn.prepareStatement(SQL_HISTORY)) {
            ps.setString(1, registrationNo.trim());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    records.add(mapHistoryRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(
                "HistoryService.getServiceHistory: Failed to retrieve history for '" +
                registrationNo + "'. Cause: " + e.getMessage(), e);
        }

        return records;
    }

    /**
     * Returns a summary of vehicle and owner details for the header display.
     * Returns null if the vehicle is not found.
     *
     * @param registrationNo the number plate (case-insensitive)
     * @return a {@link VehicleSummary} DTO, or null if not found
     * @throws RuntimeException wrapping any SQLException
     */
    public VehicleSummary getVehicleSummary(String registrationNo) {
        if (registrationNo == null || registrationNo.trim().isEmpty()) {
            return null;
        }
        Connection conn = DBConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(SQL_VEHICLE_SUMMARY)) {
            ps.setString(1, registrationNo.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new VehicleSummary(
                        rs.getString("MAKE"),
                        rs.getString("MODEL"),
                        rs.getInt("YEAR_OF_MFR"),
                        rs.getString("FUEL_TYPE"),
                        rs.getString("CUSTOMER_NAME"),
                        rs.getString("PHONE")
                    );
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(
                "HistoryService.getVehicleSummary: Failed to load vehicle summary for '" +
                registrationNo + "'. Cause: " + e.getMessage(), e);
        }
        return null;
    }

    // ── Private Helpers ───────────────────────────────────────────────────────

    /**
     * Maps one ResultSet row into a {@link ServiceHistoryRecord}.
     * Always references columns by name per AGENTS.md §3.5.
     *
     * @param rs positioned ResultSet
     * @return populated record
     * @throws SQLException if any column access fails
     */
    private ServiceHistoryRecord mapHistoryRow(ResultSet rs) throws SQLException {
        int    jobCardId      = rs.getInt("JOB_CARD_ID");
        Timestamp apptTs      = rs.getTimestamp("APPOINTMENT_DT");
        LocalDateTime apptDt  = (apptTs != null) ? apptTs.toLocalDateTime() : null;
        String status         = rs.getString("STATUS");
        String serviceName    = rs.getString("SERVICE_NAME");
        String technicianName = rs.getString("TECHNICIAN_NAME");
        double labourHours    = rs.getDouble("LABOUR_HOURS");

        // LEFT JOIN — these may be null if no invoice exists yet
        double grandTotal     = rs.getDouble("GRAND_TOTAL");
        boolean hasInvoice    = !rs.wasNull();
        String paymentStatus  = rs.getString("PAYMENT_STATUS");

        return new ServiceHistoryRecord(
            jobCardId, apptDt, status, serviceName,
            technicianName, labourHours,
            hasInvoice ? grandTotal : null,
            paymentStatus
        );
    }

    // ── Inner DTOs ────────────────────────────────────────────────────────────

    /**
     * Flat DTO representing one row in the service history table.
     * Not a model object — used only for display.
     */
    public static final class ServiceHistoryRecord {

        public final int           jobCardId;
        public final LocalDateTime appointmentDt;
        public final String        status;
        public final String        serviceName;
        public final String        technicianName;
        public final double        labourHours;
        /** null when no invoice has been generated yet */
        public final Double        grandTotal;
        /** null when no invoice exists */
        public final String        paymentStatus;

        /**
         * Constructs a history record DTO.
         *
         * @param jobCardId      job card PK
         * @param appointmentDt  scheduled appointment date/time
         * @param status         job card lifecycle status
         * @param serviceName    catalogue service name
         * @param technicianName assigned technician (may be null)
         * @param labourHours    recorded labour hours
         * @param grandTotal     invoice grand total (null if not invoiced)
         * @param paymentStatus  invoice payment status (null if not invoiced)
         */
        public ServiceHistoryRecord(int jobCardId, LocalDateTime appointmentDt,
                                    String status, String serviceName,
                                    String technicianName, double labourHours,
                                    Double grandTotal, String paymentStatus) {
            this.jobCardId      = jobCardId;
            this.appointmentDt  = appointmentDt;
            this.status         = status;
            this.serviceName    = serviceName;
            this.technicianName = technicianName;
            this.labourHours    = labourHours;
            this.grandTotal     = grandTotal;
            this.paymentStatus  = paymentStatus;
        }
    }

    /**
     * Flat DTO for the vehicle + owner summary header.
     */
    public static final class VehicleSummary {

        public final String make;
        public final String model;
        public final int    yearOfMfr;
        public final String fuelType;
        public final String customerName;
        public final String phone;

        /**
         * Constructs a vehicle summary DTO.
         *
         * @param make         vehicle make
         * @param model        vehicle model
         * @param yearOfMfr    year of manufacture
         * @param fuelType     fuel type
         * @param customerName owner's name
         * @param phone        owner's phone
         */
        public VehicleSummary(String make, String model, int yearOfMfr,
                              String fuelType, String customerName, String phone) {
            this.make         = make;
            this.model        = model;
            this.yearOfMfr    = yearOfMfr;
            this.fuelType     = fuelType;
            this.customerName = customerName;
            this.phone        = phone;
        }
    }
}
