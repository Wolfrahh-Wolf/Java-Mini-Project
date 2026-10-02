/**
 * Service — POJO representing a service type in the garage catalogue.
 * Maps directly to the SERVICES table in Oracle.
 *
 * Course  : UIT3361 OOP Java / UIT3311 Database Technology Lab
 * Project : Vehicle Service Management System
 */
package com.garage.model;

import java.time.LocalDateTime;

/**
 * Plain Java object representing a service catalogue entry.
 * No business logic — only fields, constructors, getters, and setters.
 *
 * <p>RATE_TYPE is either "FIXED" (flat fee) or "HOURLY" (per technician-hour).
 * This determines how labour cost is computed during invoicing.
 */
public class Service {

    private int           serviceId;
    private String        serviceName;
    private String        description;
    private double        labourRate;
    private String        rateType;       // "FIXED" or "HOURLY"
    private LocalDateTime createdAt;

    // ── Constructors ──────────────────────────────────────────────────────────

    /** No-arg constructor required for DAO result-set mapping. */
    public Service() {}

    /**
     * Constructor for creating a new service entry (before DB insert).
     *
     * @param serviceName name of the service (unique in catalogue)
     * @param description detailed description (may be null)
     * @param labourRate  rate in INR — flat fee if FIXED, per-hour if HOURLY
     * @param rateType    "FIXED" or "HOURLY"
     */
    public Service(String serviceName, String description,
                   double labourRate, String rateType) {
        this.serviceName = serviceName;
        this.description = description;
        this.labourRate  = labourRate;
        this.rateType    = rateType;
    }

    /**
     * Full constructor including DB-assigned fields (used when mapping from ResultSet).
     *
     * @param serviceId   surrogate PK assigned by Oracle sequence
     * @param serviceName name of the service
     * @param description detailed description
     * @param labourRate  labour rate in INR
     * @param rateType    "FIXED" or "HOURLY"
     * @param createdAt   record creation timestamp
     */
    public Service(int serviceId, String serviceName, String description,
                   double labourRate, String rateType, LocalDateTime createdAt) {
        this.serviceId   = serviceId;
        this.serviceName = serviceName;
        this.description = description;
        this.labourRate  = labourRate;
        this.rateType    = rateType;
        this.createdAt   = createdAt;
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    /** @return surrogate primary key */
    public int getServiceId()       { return serviceId; }

    /** @return the service name (unique in catalogue) */
    public String getServiceName()  { return serviceName; }

    /** @return detailed description (may be null) */
    public String getDescription()  { return description; }

    /** @return labour rate in INR */
    public double getLabourRate()   { return labourRate; }

    /** @return "FIXED" or "HOURLY" */
    public String getRateType()     { return rateType; }

    /** @return record creation timestamp */
    public LocalDateTime getCreatedAt() { return createdAt; }

    // ── Setters ───────────────────────────────────────────────────────────────

    /** @param serviceId surrogate PK (set by DAO after insert) */
    public void setServiceId(int serviceId)       { this.serviceId = serviceId; }

    /** @param serviceName the service name */
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }

    /** @param description the service description */
    public void setDescription(String description) { this.description = description; }

    /** @param labourRate labour rate in INR */
    public void setLabourRate(double labourRate)   { this.labourRate = labourRate; }

    /** @param rateType "FIXED" or "HOURLY" */
    public void setRateType(String rateType)       { this.rateType = rateType; }

    /** @param createdAt record creation timestamp */
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    // ── Utility ───────────────────────────────────────────────────────────────

    /**
     * Returns a display-friendly string used in JComboBox rendering.
     *
     * @return "ServiceName (FIXED: ₹1200)" or "ServiceName (HOURLY: ₹350/hr)"
     */
    @Override
    public String toString() {
        String rateLabel = "HOURLY".equals(rateType)
            ? String.format("₹%.0f/hr", labourRate)
            : String.format("₹%.0f fixed", labourRate);
        return serviceName + "  [" + rateLabel + "]";
    }
}
