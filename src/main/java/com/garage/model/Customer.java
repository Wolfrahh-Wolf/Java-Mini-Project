/**
 * Customer — POJO representing a garage customer.
 * Maps directly to the CUSTOMERS table in Oracle.
 *
 * Course  : UIT3361 OOP Java / UIT3311 Database Technology Lab
 * Project : Vehicle Service Management System
 */
package com.garage.model;

import java.time.LocalDateTime;

/**
 * Plain Java object representing a customer record.
 * No business logic — only fields, constructors, getters, and setters.
 */
public class Customer {

    private int           customerId;
    private String        customerName;
    private String        phone;
    private String        email;
    private String        address;
    private LocalDateTime createdAt;

    // ── Constructors ──────────────────────────────────────────────────────────

    /** No-arg constructor required for DAO result-set mapping. */
    public Customer() {}

    /**
     * Full constructor for creating a new customer record (before DB insert).
     *
     * @param customerName full name of the customer
     * @param phone        mobile phone number (unique business key)
     * @param email        email address (may be null)
     * @param address      street/city address (may be null)
     */
    public Customer(String customerName, String phone, String email, String address) {
        this.customerName = customerName;
        this.phone        = phone;
        this.email        = email;
        this.address      = address;
    }

    /**
     * Full constructor including DB-assigned fields (used when mapping from ResultSet).
     *
     * @param customerId   surrogate primary key assigned by Oracle sequence
     * @param customerName full name
     * @param phone        phone number
     * @param email        email address
     * @param address      address
     * @param createdAt    record creation timestamp
     */
    public Customer(int customerId, String customerName, String phone,
                    String email, String address, LocalDateTime createdAt) {
        this.customerId   = customerId;
        this.customerName = customerName;
        this.phone        = phone;
        this.email        = email;
        this.address      = address;
        this.createdAt    = createdAt;
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    /** @return the surrogate primary key */
    public int getCustomerId()   { return customerId; }

    /** @return the customer's full name */
    public String getCustomerName() { return customerName; }

    /** @return the customer's phone number */
    public String getPhone()     { return phone; }

    /** @return the customer's email address (may be null) */
    public String getEmail()     { return email; }

    /** @return the customer's address (may be null) */
    public String getAddress()   { return address; }

    /** @return the record creation timestamp */
    public LocalDateTime getCreatedAt() { return createdAt; }

    // ── Setters ───────────────────────────────────────────────────────────────

    /** @param customerId the surrogate PK (set by DAO after insert) */
    public void setCustomerId(int customerId)     { this.customerId = customerId; }

    /** @param customerName the customer's full name */
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    /** @param phone the customer's phone number */
    public void setPhone(String phone)            { this.phone = phone; }

    /** @param email the customer's email address */
    public void setEmail(String email)            { this.email = email; }

    /** @param address the customer's address */
    public void setAddress(String address)        { this.address = address; }

    /** @param createdAt record creation timestamp (set by DAO after insert) */
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    // ── Utility ───────────────────────────────────────────────────────────────

    @Override
    public String toString() {
        return "Customer{id=" + customerId +
               ", name='" + customerName + '\'' +
               ", phone='" + phone + '\'' + '}';
    }
}
