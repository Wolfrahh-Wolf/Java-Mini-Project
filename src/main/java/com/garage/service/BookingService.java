/**
 * BookingService — Business logic for booking service appointments.
 * Validates inputs, demarcates transactions, and coordinates DAO calls.
 */
package com.garage.service;

import com.garage.dao.JobCardDAO;
import com.garage.dao.ServiceDAO;
import com.garage.dao.VehicleDAO;
import com.garage.model.JobCard;
import com.garage.model.Service;
import com.garage.model.Vehicle;
import com.garage.util.DBConnection;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Provides business operations for booking service appointments.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Validate that the vehicle exists before booking.</li>
 *   <li>Validate that the appointment date/time is in the future.</li>
 *   <li>Create a {@link JobCard} with status {@code BOOKED}.</li>
 *   <li>Load the service catalogue for the booking panel.</li>
 * </ul>
 *
 * <p>SQLExceptions are wrapped in RuntimeExceptions so Swing callers
 * only handle unchecked exceptions in their {@code SwingWorker.done()} methods.
 */
public class BookingService {

    private final VehicleDAO  vehicleDao  = new VehicleDAO();
    private final ServiceDAO  serviceDao  = new ServiceDAO();
    private final JobCardDAO  jobCardDao  = new JobCardDAO();

    // ── Public Methods ────────────────────────────────────────────────────────

    /**
     * Books a service appointment for a vehicle.
     *
     * <p>Validation rules:
     * <ul>
     *   <li>vehicleId must correspond to an existing vehicle in the database.</li>
     *   <li>serviceId must be a positive integer.</li>
     *   <li>appointmentDt must be after the current date/time.</li>
     * </ul>
     *
     * @param vehicleId     the VEHICLE_ID of the vehicle to service
     * @param serviceId     the SERVICE_ID from the catalogue
     * @param appointmentDt the scheduled appointment date and time
     * @param remarks       optional notes from the customer or advisor (may be null)
     * @return the created {@link JobCard} with its generated ID populated
     * @throws IllegalArgumentException if any validation rule is violated
     * @throws RuntimeException         wrapping any SQLException from the DAO
     */
    public JobCard bookAppointment(int vehicleId, int serviceId,
                                   LocalDateTime appointmentDt, String remarks) {
        // ── Validate inputs ───────────────────────────────────────────────────
        if (vehicleId <= 0) {
            throw new IllegalArgumentException(
                "BookingService: A valid vehicle must be selected.");
        }
        if (serviceId <= 0) {
            throw new IllegalArgumentException(
                "BookingService: A valid service type must be selected.");
        }
        if (appointmentDt == null) {
            throw new IllegalArgumentException(
                "BookingService: Appointment date/time must not be null.");
        }
        if (!appointmentDt.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException(
                "BookingService: Appointment date/time must be in the future.");
        }

        Connection conn = DBConnection.getConnection();

        // ── Verify vehicle exists ─────────────────────────────────────────────
        try {
            Optional<Vehicle> vehicle = vehicleDao.findById(conn, vehicleId);
            if (vehicle.isEmpty()) {
                throw new IllegalArgumentException(
                    "BookingService: No vehicle found with id=" + vehicleId +
                    ". Cannot book appointment.");
            }
        } catch (SQLException e) {
            throw new RuntimeException(
                "BookingService.bookAppointment: Failed to verify vehicle id=" +
                vehicleId + ". Cause: " + e.getMessage(), e);
        }

        // ── Create and insert the job card ────────────────────────────────────
        String cleanRemarks = (remarks != null && !remarks.trim().isEmpty())
            ? remarks.trim() : null;

        JobCard jobCard = new JobCard(vehicleId, serviceId, appointmentDt, cleanRemarks);

        try {
            jobCardDao.insert(conn, jobCard);
        } catch (SQLException e) {
            throw new RuntimeException(
                "BookingService.bookAppointment: Failed to create job card for " +
                "vehicleId=" + vehicleId + ". Cause: " + e.getMessage(), e);
        }

        return jobCard;
    }

    /**
     * Returns all services in the catalogue.
     * Used to populate the service JComboBox in the booking panel.
     *
     * @return list of all catalogue services (never null)
     * @throws RuntimeException wrapping any SQLException
     */
    public List<Service> getAllServices() {
        Connection conn = DBConnection.getConnection();
        try {
            return serviceDao.findAll(conn);
        } catch (SQLException e) {
            throw new RuntimeException(
                "BookingService.getAllServices: Failed to load service catalogue. " +
                "Cause: " + e.getMessage(), e);
        }
    }

    /**
     * Finds a vehicle by its registration number.
     * Used by the booking panel to display vehicle details before booking.
     *
     * @param registrationNo the number plate to look up (case-insensitive)
     * @return an Optional containing the Vehicle, or empty if not found
     * @throws IllegalArgumentException if registrationNo is blank
     * @throws RuntimeException         wrapping any SQLException
     */
    public Optional<Vehicle> findVehicleByRegistration(String registrationNo) {
        if (registrationNo == null || registrationNo.trim().isEmpty()) {
            throw new IllegalArgumentException(
                "BookingService: Registration number must not be blank.");
        }
        Connection conn = DBConnection.getConnection();
        try {
            return vehicleDao.findByRegistrationNo(conn, registrationNo.trim());
        } catch (SQLException e) {
            throw new RuntimeException(
                "BookingService.findVehicleByRegistration: Failed to look up vehicle '" +
                registrationNo + "'. Cause: " + e.getMessage(), e);
        }
    }
}
