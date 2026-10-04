/**
 * VehicleService — Business logic for vehicle registration and retrieval.
 * Owns validation and transaction demarcation; delegates persistence to VehicleDAO.
 */
package com.garage.service;

import com.garage.dao.VehicleDAO;
import com.garage.model.Vehicle;
import com.garage.util.DBConnection;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Provides business operations on {@link Vehicle} entities.
 *
 * <p>Input validation is performed here before any DAO call.
 * SQLExceptions are wrapped in RuntimeExceptions so the Swing UI
 * only handles unchecked exceptions.
 */
public class VehicleService {

    private static final int MIN_YEAR = 1900;
    private static final int MAX_YEAR = LocalDate.now().getYear() + 1;

    private final VehicleDAO vehicleDao = new VehicleDAO();

    // ── Public Methods ────────────────────────────────────────────────────────

    /**
     * Validates and registers a new vehicle under an existing customer.
     *
     * <p>Validation rules:
     * <ul>
     *   <li>customerId must be > 0.</li>
     *   <li>registrationNo must not be blank.</li>
     *   <li>make must not be blank.</li>
     *   <li>model must not be blank.</li>
     *   <li>yearOfMfr must be between MIN_YEAR and MAX_YEAR.</li>
     *   <li>fuelType must not be blank.</li>
     *   <li>odometerKm must be >= 0.</li>
     * </ul>
     *
     * @param vehicle the vehicle to register (vehicleId will be populated after insert)
     * @throws IllegalArgumentException if validation fails
     * @throws RuntimeException         wrapping any SQLException from the DAO
     */
    public void registerVehicle(Vehicle vehicle) {
        validateVehicle(vehicle);

        Connection conn = DBConnection.getConnection();
        try {
            vehicleDao.insert(conn, vehicle);
        } catch (SQLException e) {
            throw new RuntimeException(
                "VehicleService.registerVehicle: Failed to register vehicle '" +
                vehicle.getRegistrationNo() + "'. Cause: " + e.getMessage(), e);
        }
    }

    /**
     * Returns all vehicles owned by a specific customer.
     *
     * @param customerId the owning customer's CUSTOMER_ID
     * @return list of vehicles (may be empty)
     * @throws RuntimeException wrapping any SQLException
     */
    public List<Vehicle> getVehiclesForCustomer(int customerId) {
        Connection conn = DBConnection.getConnection();
        try {
            return vehicleDao.findByCustomerId(conn, customerId);
        } catch (SQLException e) {
            throw new RuntimeException(
                "VehicleService.getVehiclesForCustomer: Failed to retrieve vehicles for " +
                "customerId=" + customerId + ". Cause: " + e.getMessage(), e);
        }
    }

    /**
     * Finds a vehicle by its registration number (case-insensitive).
     *
     * @param registrationNo the number plate to search
     * @return an Optional containing the Vehicle, or empty if not found
     * @throws IllegalArgumentException if registrationNo is blank
     * @throws RuntimeException         wrapping any SQLException
     */
    public Optional<Vehicle> findByRegistrationNo(String registrationNo) {
        if (registrationNo == null || registrationNo.trim().isEmpty()) {
            throw new IllegalArgumentException(
                "VehicleService.findByRegistrationNo: Registration number must not be blank.");
        }
        Connection conn = DBConnection.getConnection();
        try {
            return vehicleDao.findByRegistrationNo(conn, registrationNo.trim());
        } catch (SQLException e) {
            throw new RuntimeException(
                "VehicleService.findByRegistrationNo: Failed to find vehicle '" +
                registrationNo + "'. Cause: " + e.getMessage(), e);
        }
    }

    /**
     * Finds a vehicle by its surrogate primary key.
     *
     * @param vehicleId the VEHICLE_ID to look up
     * @return an Optional containing the Vehicle, or empty if not found
     * @throws RuntimeException wrapping any SQLException
     */
    public Optional<Vehicle> findById(int vehicleId) {
        Connection conn = DBConnection.getConnection();
        try {
            return vehicleDao.findById(conn, vehicleId);
        } catch (SQLException e) {
            throw new RuntimeException(
                "VehicleService.findById: Failed to find vehicle with id=" +
                vehicleId + ". Cause: " + e.getMessage(), e);
        }
    }

    /**
     * Returns all vehicles from the database.
     *
     * @return list of all vehicles (may be empty)
     * @throws RuntimeException wrapping any SQLException
     */
    public List<Vehicle> getAllVehicles() {
        Connection conn = DBConnection.getConnection();
        try {
            return vehicleDao.findAll(conn);
        } catch (SQLException e) {
            throw new RuntimeException(
                "VehicleService.getAllVehicles: Failed to retrieve all vehicles. " +
                "Cause: " + e.getMessage(), e);
        }
    }

    // ── Private Helpers ───────────────────────────────────────────────────────

    /**
     * Validates a Vehicle object prior to DB insert.
     *
     * @param vehicle the vehicle to validate
     * @throws IllegalArgumentException if any required field is invalid
     */
    private void validateVehicle(Vehicle vehicle) {
        if (vehicle == null) {
            throw new IllegalArgumentException(
                "VehicleService: Vehicle object must not be null.");
        }
        if (vehicle.getCustomerId() <= 0) {
            throw new IllegalArgumentException(
                "VehicleService: A valid customer must be selected before registering a vehicle.");
        }
        if (vehicle.getRegistrationNo() == null || vehicle.getRegistrationNo().trim().isEmpty()) {
            throw new IllegalArgumentException(
                "VehicleService: Registration number must not be blank.");
        }
        if (vehicle.getMake() == null || vehicle.getMake().trim().isEmpty()) {
            throw new IllegalArgumentException(
                "VehicleService: Vehicle make (manufacturer) must not be blank.");
        }
        if (vehicle.getModel() == null || vehicle.getModel().trim().isEmpty()) {
            throw new IllegalArgumentException(
                "VehicleService: Vehicle model must not be blank.");
        }
        if (vehicle.getYearOfMfr() < MIN_YEAR || vehicle.getYearOfMfr() > MAX_YEAR) {
            throw new IllegalArgumentException(
                "VehicleService: Year of manufacture must be between " + MIN_YEAR +
                " and " + MAX_YEAR + ".");
        }
        if (vehicle.getFuelType() == null || vehicle.getFuelType().trim().isEmpty()) {
            throw new IllegalArgumentException(
                "VehicleService: Fuel type must not be blank.");
        }
        if (vehicle.getOdometerKm() < 0) {
            throw new IllegalArgumentException(
                "VehicleService: Odometer reading must be zero or positive.");
        }
    }
}
