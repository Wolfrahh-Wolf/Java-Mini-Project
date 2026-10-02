/**
 * JobCardDAO — Data Access Object for the JOB_CARDS table.
 * All SQL operations use PreparedStatement with try-with-resources.
 * Column references always use column names, never ordinal indexes.
 *
 * Course  : UIT3361 OOP Java / UIT3311 Database Technology Lab
 * Project : Vehicle Service Management System
 */
package com.garage.dao;

import com.garage.model.JobCard;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Provides CRUD and lifecycle operations for {@link JobCard} records.
 *
 * <p>Every method receives a {@link Connection} parameter so that the
 * service layer can control transaction boundaries (setAutoCommit /
 * commit / rollback) across multiple DAO calls.
 */
public class JobCardDAO {

    // ── SQL Constants ─────────────────────────────────────────────────────────

    private static final String SQL_INSERT =
            "INSERT INTO JOB_CARDS " +
            "(VEHICLE_ID, SERVICE_ID, TECHNICIAN_NAME, STATUS, APPOINTMENT_DT, " +
            " START_DT, COMPLETION_DT, DELIVERY_DT, LABOUR_HOURS, REMARKS) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String SQL_FIND_BY_ID =
            "SELECT JOB_CARD_ID, VEHICLE_ID, SERVICE_ID, TECHNICIAN_NAME, STATUS, " +
            "APPOINTMENT_DT, START_DT, COMPLETION_DT, DELIVERY_DT, " +
            "LABOUR_HOURS, REMARKS, CREATED_AT, UPDATED_AT " +
            "FROM JOB_CARDS WHERE JOB_CARD_ID = ?";

    private static final String SQL_FIND_BY_VEHICLE =
            "SELECT JOB_CARD_ID, VEHICLE_ID, SERVICE_ID, TECHNICIAN_NAME, STATUS, " +
            "APPOINTMENT_DT, START_DT, COMPLETION_DT, DELIVERY_DT, " +
            "LABOUR_HOURS, REMARKS, CREATED_AT, UPDATED_AT " +
            "FROM JOB_CARDS WHERE VEHICLE_ID = ? ORDER BY APPOINTMENT_DT DESC";

    private static final String SQL_FIND_BY_STATUS =
            "SELECT JOB_CARD_ID, VEHICLE_ID, SERVICE_ID, TECHNICIAN_NAME, STATUS, " +
            "APPOINTMENT_DT, START_DT, COMPLETION_DT, DELIVERY_DT, " +
            "LABOUR_HOURS, REMARKS, CREATED_AT, UPDATED_AT " +
            "FROM JOB_CARDS WHERE STATUS = ? ORDER BY APPOINTMENT_DT";

    private static final String SQL_FIND_ALL =
            "SELECT JOB_CARD_ID, VEHICLE_ID, SERVICE_ID, TECHNICIAN_NAME, STATUS, " +
            "APPOINTMENT_DT, START_DT, COMPLETION_DT, DELIVERY_DT, " +
            "LABOUR_HOURS, REMARKS, CREATED_AT, UPDATED_AT " +
            "FROM JOB_CARDS ORDER BY APPOINTMENT_DT DESC";

    private static final String SQL_UPDATE_STATUS_IN_PROGRESS =
            "UPDATE JOB_CARDS SET STATUS = 'IN_PROGRESS', START_DT = SYSTIMESTAMP " +
            "WHERE JOB_CARD_ID = ?";

    private static final String SQL_UPDATE_STATUS_COMPLETED =
            "UPDATE JOB_CARDS SET STATUS = 'COMPLETED', COMPLETION_DT = SYSTIMESTAMP " +
            "WHERE JOB_CARD_ID = ?";

    private static final String SQL_UPDATE_STATUS_DELIVERED =
            "UPDATE JOB_CARDS SET STATUS = 'DELIVERED', DELIVERY_DT = SYSTIMESTAMP " +
            "WHERE JOB_CARD_ID = ?";

    private static final String SQL_UPDATE_TECHNICIAN =
            "UPDATE JOB_CARDS SET TECHNICIAN_NAME = ? WHERE JOB_CARD_ID = ?";

    private static final String SQL_UPDATE_LABOUR_HOURS =
            "UPDATE JOB_CARDS SET LABOUR_HOURS = ? WHERE JOB_CARD_ID = ?";

    // ── Public Methods ────────────────────────────────────────────────────────

    /**
     * Inserts a new job card record and returns the generated primary key.
     *
     * @param conn    active JDBC connection (transaction controlled by caller)
     * @param jobCard the job card to insert (jobCardId field is ignored)
     * @return the generated JOB_CARD_ID assigned by Oracle
     * @throws SQLException if the INSERT fails
     */
    public int insert(Connection conn, JobCard jobCard) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SQL_INSERT,
                new String[]{"JOB_CARD_ID"})) {

            ps.setInt(   1, jobCard.getVehicleId());
            ps.setInt(   2, jobCard.getServiceId());

            if (jobCard.getTechnicianName() != null) {
                ps.setString(3, jobCard.getTechnicianName());
            } else {
                ps.setNull(3, Types.VARCHAR);
            }

            ps.setString(4, jobCard.getStatus());
            ps.setTimestamp(5, Timestamp.valueOf(jobCard.getAppointmentDt()));

            setNullableTimestamp(ps, 6,  jobCard.getStartDt());
            setNullableTimestamp(ps, 7,  jobCard.getCompletionDt());
            setNullableTimestamp(ps, 8,  jobCard.getDeliveryDt());

            ps.setDouble(9,  jobCard.getLabourHours());

            if (jobCard.getRemarks() != null) {
                ps.setString(10, jobCard.getRemarks());
            } else {
                ps.setNull(10, Types.VARCHAR);
            }

            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int generatedId = keys.getInt(1);
                    jobCard.setJobCardId(generatedId);
                    return generatedId;
                }
                throw new SQLException(
                    "JobCardDAO.insert: INSERT succeeded but no generated key was returned.");
            }
        }
    }

    /**
     * Finds a job card by its surrogate primary key.
     *
     * @param conn      active JDBC connection
     * @param jobCardId the JOB_CARD_ID to look up
     * @return an Optional containing the JobCard, or empty if not found
     * @throws SQLException if the SELECT fails
     */
    public Optional<JobCard> findById(Connection conn, int jobCardId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_ID)) {
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
     * Returns all job cards for a given vehicle, newest appointment first.
     * Used for the service history module.
     *
     * @param conn      active JDBC connection
     * @param vehicleId the VEHICLE_ID whose job cards to retrieve
     * @return list of job cards (empty if none)
     * @throws SQLException if the SELECT fails
     */
    public List<JobCard> findByVehicleId(Connection conn, int vehicleId) throws SQLException {
        List<JobCard> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_VEHICLE)) {
            ps.setInt(1, vehicleId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    /**
     * Returns all job cards with a specific status, ordered by appointment date.
     *
     * @param conn   active JDBC connection
     * @param status one of: BOOKED, IN_PROGRESS, COMPLETED, DELIVERED
     * @return list of matching job cards (empty if none)
     * @throws SQLException if the SELECT fails
     */
    public List<JobCard> findByStatus(Connection conn, String status) throws SQLException {
        List<JobCard> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_STATUS)) {
            ps.setString(1, status);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    /**
     * Returns all job cards, newest appointment first.
     *
     * @param conn active JDBC connection
     * @return list of all job cards
     * @throws SQLException if the SELECT fails
     */
    public List<JobCard> findAll(Connection conn) throws SQLException {
        List<JobCard> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(SQL_FIND_ALL);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }
        return list;
    }

    /**
     * Advances the job card status to the next lifecycle state and sets
     * the corresponding timestamp column.
     *
     * <p>Valid transitions:
     * <ul>
     *   <li>BOOKED → IN_PROGRESS (sets START_DT)</li>
     *   <li>IN_PROGRESS → COMPLETED (sets COMPLETION_DT)</li>
     *   <li>COMPLETED → DELIVERED (sets DELIVERY_DT)</li>
     * </ul>
     *
     * @param conn      active JDBC connection
     * @param jobCardId the job card to advance
     * @param newStatus the target status ("IN_PROGRESS", "COMPLETED", or "DELIVERED")
     * @throws SQLException             if the UPDATE fails
     * @throws IllegalArgumentException if newStatus is not a valid next state
     */
    public void updateStatus(Connection conn, int jobCardId,
                             String newStatus) throws SQLException {
        String sql = switch (newStatus) {
            case "IN_PROGRESS" -> SQL_UPDATE_STATUS_IN_PROGRESS;
            case "COMPLETED"   -> SQL_UPDATE_STATUS_COMPLETED;
            case "DELIVERED"   -> SQL_UPDATE_STATUS_DELIVERED;
            default -> throw new IllegalArgumentException(
                "JobCardDAO.updateStatus: Invalid target status '" + newStatus + "'.");
        };

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, jobCardId);
            int rows = ps.executeUpdate();
            if (rows == 0) {
                throw new SQLException(
                    "JobCardDAO.updateStatus: No job card found with id=" + jobCardId);
            }
        }
    }

    /**
     * Assigns a technician to a job card.
     *
     * @param conn           active JDBC connection
     * @param jobCardId      the job card to update
     * @param technicianName the technician's name
     * @throws SQLException if the UPDATE fails
     */
    public void updateTechnician(Connection conn, int jobCardId,
                                 String technicianName) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SQL_UPDATE_TECHNICIAN)) {
            ps.setString(1, technicianName);
            ps.setInt(   2, jobCardId);
            ps.executeUpdate();
        }
    }

    /**
     * Records the actual labour hours for a job card.
     *
     * @param conn       active JDBC connection
     * @param jobCardId  the job card to update
     * @param hours      labour hours (must be >= 0)
     * @throws SQLException if the UPDATE fails
     */
    public void updateLabourHours(Connection conn, int jobCardId,
                                  double hours) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SQL_UPDATE_LABOUR_HOURS)) {
            ps.setDouble(1, hours);
            ps.setInt(   2, jobCardId);
            ps.executeUpdate();
        }
    }

    // ── Private Helpers ───────────────────────────────────────────────────────

    /**
     * Maps a single ResultSet row to a {@link JobCard} object.
     * Always references columns by name per AGENTS.md §3.5.
     *
     * @param rs the ResultSet positioned at the row to map
     * @return a populated JobCard instance
     * @throws SQLException if any column access fails
     */
    private JobCard mapRow(ResultSet rs) throws SQLException {
        int    jobCardId      = rs.getInt("JOB_CARD_ID");
        int    vehicleId      = rs.getInt("VEHICLE_ID");
        int    serviceId      = rs.getInt("SERVICE_ID");
        String technicianName = rs.getString("TECHNICIAN_NAME");
        String status         = rs.getString("STATUS");
        LocalDateTime appointmentDt = toLocalDateTime(rs.getTimestamp("APPOINTMENT_DT"));
        LocalDateTime startDt       = toLocalDateTime(rs.getTimestamp("START_DT"));
        LocalDateTime completionDt  = toLocalDateTime(rs.getTimestamp("COMPLETION_DT"));
        LocalDateTime deliveryDt    = toLocalDateTime(rs.getTimestamp("DELIVERY_DT"));
        double labourHours    = rs.getDouble("LABOUR_HOURS");
        String remarks        = rs.getString("REMARKS");
        LocalDateTime createdAt  = toLocalDateTime(rs.getTimestamp("CREATED_AT"));
        LocalDateTime updatedAt  = toLocalDateTime(rs.getTimestamp("UPDATED_AT"));

        return new JobCard(jobCardId, vehicleId, serviceId, technicianName, status,
                           appointmentDt, startDt, completionDt, deliveryDt,
                           labourHours, remarks, createdAt, updatedAt);
    }

    /**
     * Converts a nullable {@link Timestamp} to {@link LocalDateTime}.
     *
     * @param ts the Timestamp (may be null)
     * @return the LocalDateTime, or null if ts is null
     */
    private LocalDateTime toLocalDateTime(Timestamp ts) {
        return (ts != null) ? ts.toLocalDateTime() : null;
    }

    /**
     * Sets a nullable Timestamp parameter on a PreparedStatement.
     *
     * @param ps    the PreparedStatement
     * @param index the parameter index
     * @param ldt   the LocalDateTime value (may be null)
     * @throws SQLException if the parameter cannot be set
     */
    private void setNullableTimestamp(PreparedStatement ps, int index,
                                      LocalDateTime ldt) throws SQLException {
        if (ldt != null) {
            ps.setTimestamp(index, Timestamp.valueOf(ldt));
        } else {
            ps.setNull(index, Types.TIMESTAMP);
        }
    }
}
