/**
 * CustomerService — Business logic for customer registration and lookup.
 * Owns validation and transaction demarcation; delegates persistence to CustomerDAO.
 */
package com.garage.service;

import com.garage.dao.CustomerDAO;
import com.garage.model.Customer;
import com.garage.util.DBConnection;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Provides business operations on {@link Customer} entities.
 *
 * <p>Validation is performed here before any DAO calls are made.
 * RuntimeExceptions wrap SQLExceptions so callers (UI layer) do not
 * need to catch checked database exceptions.
 */
public class CustomerService {

    private final CustomerDAO customerDao = new CustomerDAO();

    // ── Public Methods ────────────────────────────────────────────────────────

    /**
     * Validates and registers a new customer in the database.
     *
     * <p>Validation rules:
     * <ul>
     *   <li>customerName must not be blank.</li>
     *   <li>phone must not be blank.</li>
     * </ul>
     *
     * @param customer the Customer to register (customerId will be populated after insert)
     * @throws IllegalArgumentException if validation fails
     * @throws RuntimeException         wrapping any SQLException from the DAO
     */
    public void registerCustomer(Customer customer) {
        validateCustomer(customer);

        Connection conn = DBConnection.getConnection();
        try {
            customerDao.insert(conn, customer);
        } catch (SQLException e) {
            throw new RuntimeException(
                "CustomerService.registerCustomer: Failed to register customer '" +
                customer.getCustomerName() + "'. Cause: " + e.getMessage(), e);
        }
    }

    /**
     * Looks up a customer by phone number.
     *
     * @param phone the phone number to search (trimmed before query)
     * @return an Optional containing the Customer, or empty if not found
     * @throws IllegalArgumentException if phone is blank
     * @throws RuntimeException         wrapping any SQLException
     */
    public Optional<Customer> findByPhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            throw new IllegalArgumentException(
                "CustomerService.findByPhone: Phone number must not be blank.");
        }
        Connection conn = DBConnection.getConnection();
        try {
            return customerDao.findByPhone(conn, phone.trim());
        } catch (SQLException e) {
            throw new RuntimeException(
                "CustomerService.findByPhone: Failed to look up customer by phone '" +
                phone + "'. Cause: " + e.getMessage(), e);
        }
    }

    /**
     * Looks up a customer by their surrogate primary key.
     *
     * @param customerId the CUSTOMER_ID to search
     * @return an Optional containing the Customer, or empty if not found
     * @throws RuntimeException wrapping any SQLException
     */
    public Optional<Customer> findById(int customerId) {
        Connection conn = DBConnection.getConnection();
        try {
            return customerDao.findById(conn, customerId);
        } catch (SQLException e) {
            throw new RuntimeException(
                "CustomerService.findById: Failed to look up customer with id=" +
                customerId + ". Cause: " + e.getMessage(), e);
        }
    }

    /**
     * Returns all customers from the database.
     *
     * @return list of all customers (may be empty)
     * @throws RuntimeException wrapping any SQLException
     */
    public List<Customer> getAllCustomers() {
        Connection conn = DBConnection.getConnection();
        try {
            return customerDao.findAll(conn);
        } catch (SQLException e) {
            throw new RuntimeException(
                "CustomerService.getAllCustomers: Failed to retrieve customer list. " +
                "Cause: " + e.getMessage(), e);
        }
    }

    /**
     * Searches for customers by partial name match (case-insensitive).
     *
     * @param nameSearch partial name string to search
     * @return list of matching customers
     * @throws RuntimeException wrapping any SQLException
     */
    public List<Customer> searchByName(String nameSearch) {
        Connection conn = DBConnection.getConnection();
        try {
            return customerDao.findByNameLike(conn, nameSearch);
        } catch (SQLException e) {
            throw new RuntimeException(
                "CustomerService.searchByName: Failed to search customers by name '" +
                nameSearch + "'. Cause: " + e.getMessage(), e);
        }
    }

    // ── Private Helpers ───────────────────────────────────────────────────────

    /**
     * Validates a Customer object prior to DB insert.
     *
     * @param customer the customer to validate
     * @throws IllegalArgumentException if any required field is missing
     */
    private void validateCustomer(Customer customer) {
        if (customer == null) {
            throw new IllegalArgumentException(
                "CustomerService: Customer object must not be null.");
        }
        if (customer.getCustomerName() == null || customer.getCustomerName().trim().isEmpty()) {
            throw new IllegalArgumentException(
                "CustomerService: Customer name must not be blank.");
        }
        if (customer.getPhone() == null || customer.getPhone().trim().isEmpty()) {
            throw new IllegalArgumentException(
                "CustomerService: Phone number must not be blank.");
        }
    }
}
