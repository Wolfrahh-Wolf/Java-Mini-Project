/**
 * JobCardService — Business logic for job card lifecycle management.
 * Validates transitions, demarcates transactions, and delegates to JobCardDAO.
 */
package com.garage.service;

import com.garage.dao.JobCardDAO;
import com.garage.model.JobCard;
import com.garage.util.DBConnection;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * Provides business operations for the job card lifecycle:
 * BOOKED → IN_PROGRESS → COMPLETED → DELIVERED.
 *
 * <p>All validation is performed here before any DAO call.
 * SQLExceptions are wrapped in RuntimeExceptions so Swing callers
 * only handle unchecked exceptions inside {@code SwingWorker.done()}.
 */
public class JobCardService {

    /** Legal status progression order for lifecycle validation. */
    private static final List<String> LIFECYCLE =
        List.of("BOOKED", "IN_PROGRESS", "COMPLETED", "DELIVERED");

    private final JobCardDAO jobCardDao = new JobCardDAO();

    // ── Public Methods ────────────────────────────────────────────────────────

    /**
     * Returns all job cards in the database, newest appointment first.
     *
     * @return list of all job cards (may be empty)
     * @throws RuntimeException wrapping any SQLException
     */
    public List<JobCard> getAllJobCards() {
        Connection conn = DBConnection.getConnection();
        try {
            return jobCardDao.findAll(conn);
        } catch (SQLException e) {
            throw new RuntimeException(
                "JobCardService.getAllJobCards: Failed to retrieve all job cards. " +
                "Cause: " + e.getMessage(), e);
        }
    }

    /**
     * Returns all job cards with the given status.
     *
     * @param status one of: BOOKED, IN_PROGRESS, COMPLETED, DELIVERED
     * @return list of matching job cards (may be empty)
     * @throws IllegalArgumentException if status is blank or invalid
     * @throws RuntimeException         wrapping any SQLException
     */
    public List<JobCard> getByStatus(String status) {
        if (status == null || status.trim().isEmpty()) {
            throw new IllegalArgumentException(
                "JobCardService.getByStatus: Status must not be blank.");
        }
        if (!LIFECYCLE.contains(status)) {
            throw new IllegalArgumentException(
                "JobCardService.getByStatus: Invalid status '" + status + "'. " +
                "Must be one of: " + LIFECYCLE);
        }
        Connection conn = DBConnection.getConnection();
        try {
            return jobCardDao.findByStatus(conn, status);
        } catch (SQLException e) {
            throw new RuntimeException(
                "JobCardService.getByStatus: Failed to retrieve job cards with status='" +
                status + "'. Cause: " + e.getMessage(), e);
        }
    }

    /**
     * Finds a job card by its surrogate primary key.
     *
     * @param jobCardId the JOB_CARD_ID to look up
     * @return the JobCard
     * @throws IllegalArgumentException if the job card does not exist
     * @throws RuntimeException         wrapping any SQLException
     */
    public JobCard findById(int jobCardId) {
        Connection conn = DBConnection.getConnection();
        try {
            return jobCardDao.findById(conn, jobCardId)
                .orElseThrow(() -> new IllegalArgumentException(
                    "JobCardService.findById: No job card found with id=" + jobCardId));
        } catch (SQLException e) {
            throw new RuntimeException(
                "JobCardService.findById: Failed to retrieve job card id=" + jobCardId +
                ". Cause: " + e.getMessage(), e);
        }
    }

    /**
     * Advances a job card to the next status in the lifecycle and returns
     * the refreshed {@link JobCard}.
     *
     * <p>Lifecycle order: BOOKED → IN_PROGRESS → COMPLETED → DELIVERED.
     * A job card in DELIVERED status cannot be advanced further.
     *
     * @param jobCardId the JOB_CARD_ID to advance
     * @return the refreshed JobCard with the new status populated
     * @throws IllegalArgumentException if the job card does not exist,
     *                                  is already DELIVERED, or the transition is invalid
     * @throws RuntimeException         wrapping any SQLException
     */
    public JobCard advanceStatus(int jobCardId) {
        Connection conn = DBConnection.getConnection();

        // Fetch current state
        JobCard current;
        try {
            current = jobCardDao.findById(conn, jobCardId)
                .orElseThrow(() -> new IllegalArgumentException(
                    "JobCardService.advanceStatus: No job card found with id=" + jobCardId));
        } catch (SQLException e) {
            throw new RuntimeException(
                "JobCardService.advanceStatus: Failed to load job card id=" + jobCardId +
                ". Cause: " + e.getMessage(), e);
        }

        // Determine next status
        int currentIndex = LIFECYCLE.indexOf(current.getStatus());
        if (currentIndex < 0) {
            throw new IllegalArgumentException(
                "JobCardService.advanceStatus: Unknown current status '" +
                current.getStatus() + "' for job card id=" + jobCardId);
        }
        if (currentIndex == LIFECYCLE.size() - 1) {
            throw new IllegalArgumentException(
                "JobCardService.advanceStatus: Job Card #" + jobCardId +
                " is already in final status 'DELIVERED'. No further advancement is possible.");
        }

        String newStatus = LIFECYCLE.get(currentIndex + 1);

        // Update the status in DB
        try {
            jobCardDao.updateStatus(conn, jobCardId, newStatus);
        } catch (SQLException e) {
            throw new RuntimeException(
                "JobCardService.advanceStatus: Failed to advance job card id=" + jobCardId +
                " to status='" + newStatus + "'. Cause: " + e.getMessage(), e);
        }

        // Return the refreshed job card
        try {
            return jobCardDao.findById(conn, jobCardId)
                .orElseThrow(() -> new RuntimeException(
                    "JobCardService.advanceStatus: Could not reload job card id=" + jobCardId));
        } catch (SQLException e) {
            throw new RuntimeException(
                "JobCardService.advanceStatus: Failed to reload job card id=" + jobCardId +
                " after status update. Cause: " + e.getMessage(), e);
        }
    }

    /**
     * Assigns a technician to a job card.
     *
     * @param jobCardId      the JOB_CARD_ID to update
     * @param technicianName the technician's full name (must not be blank)
     * @throws IllegalArgumentException if technicianName is blank
     * @throws RuntimeException         wrapping any SQLException
     */
    public void assignTechnician(int jobCardId, String technicianName) {
        if (technicianName == null || technicianName.trim().isEmpty()) {
            throw new IllegalArgumentException(
                "JobCardService.assignTechnician: Technician name must not be blank.");
        }
        Connection conn = DBConnection.getConnection();
        try {
            jobCardDao.updateTechnician(conn, jobCardId, technicianName.trim());
        } catch (SQLException e) {
            throw new RuntimeException(
                "JobCardService.assignTechnician: Failed to assign technician to job card id=" +
                jobCardId + ". Cause: " + e.getMessage(), e);
        }
    }

    /**
     * Records actual labour hours for a job card.
     *
     * @param jobCardId the JOB_CARD_ID to update
     * @param hours     actual labour hours (must be >= 0)
     * @throws IllegalArgumentException if hours is negative
     * @throws RuntimeException         wrapping any SQLException
     */
    public void recordLabourHours(int jobCardId, double hours) {
        if (hours < 0) {
            throw new IllegalArgumentException(
                "JobCardService.recordLabourHours: Labour hours must be zero or positive." +
                " Received: " + hours);
        }
        Connection conn = DBConnection.getConnection();
        try {
            jobCardDao.updateLabourHours(conn, jobCardId, hours);
        } catch (SQLException e) {
            throw new RuntimeException(
                "JobCardService.recordLabourHours: Failed to record labour hours for job card id=" +
                jobCardId + ". Cause: " + e.getMessage(), e);
        }
    }
}
