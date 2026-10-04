/**
 * Vehicle — POJO representing a customer-owned vehicle.
 * Maps directly to the VEHICLES table in Oracle.
 */
package com.garage.model;

import java.time.LocalDateTime;

/**
 * Plain Java object representing a vehicle record.
 * No business logic — only fields, constructors, getters, and setters.
 */
public class Vehicle {

    private int           vehicleId;
    private int           customerId;
    private String        registrationNo;
    private String        make;
    private String        model;
    private int           yearOfMfr;
    private String        fuelType;
    private String        color;
    private int           odometerKm;
    private LocalDateTime createdAt;

    // ── Constructors ──────────────────────────────────────────────────────────

    /** No-arg constructor required for DAO result-set mapping. */
    public Vehicle() {}

    /**
     * Constructor for creating a new vehicle (before DB insert).
     *
     * @param customerId     the owning customer's ID (FK)
     * @param registrationNo number plate (unique business key)
     * @param make           manufacturer name (e.g., Toyota)
     * @param model          model name (e.g., Corolla)
     * @param yearOfMfr      manufacturing year
     * @param fuelType       one of: PETROL, DIESEL, ELECTRIC, HYBRID, CNG, LPG
     * @param color          body colour (may be null)
     * @param odometerKm     current odometer reading in km
     */
    public Vehicle(int customerId, String registrationNo, String make, String model,
                   int yearOfMfr, String fuelType, String color, int odometerKm) {
        this.customerId     = customerId;
        this.registrationNo = registrationNo;
        this.make           = make;
        this.model          = model;
        this.yearOfMfr      = yearOfMfr;
        this.fuelType       = fuelType;
        this.color          = color;
        this.odometerKm     = odometerKm;
    }

    /**
     * Full constructor including DB-assigned fields (used when mapping from ResultSet).
     *
     * @param vehicleId      surrogate PK
     * @param customerId     FK to CUSTOMERS
     * @param registrationNo number plate
     * @param make           manufacturer
     * @param model          model name
     * @param yearOfMfr      manufacturing year
     * @param fuelType       fuel type enum string
     * @param color          body colour
     * @param odometerKm     odometer reading
     * @param createdAt      record creation timestamp
     */
    public Vehicle(int vehicleId, int customerId, String registrationNo, String make,
                   String model, int yearOfMfr, String fuelType, String color,
                   int odometerKm, LocalDateTime createdAt) {
        this.vehicleId      = vehicleId;
        this.customerId     = customerId;
        this.registrationNo = registrationNo;
        this.make           = make;
        this.model          = model;
        this.yearOfMfr      = yearOfMfr;
        this.fuelType       = fuelType;
        this.color          = color;
        this.odometerKm     = odometerKm;
        this.createdAt      = createdAt;
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    /** @return surrogate primary key */
    public int getVehicleId()       { return vehicleId; }

    /** @return FK referencing the owning customer */
    public int getCustomerId()      { return customerId; }

    /** @return number plate (unique business key) */
    public String getRegistrationNo() { return registrationNo; }

    /** @return manufacturer name */
    public String getMake()         { return make; }

    /** @return model name */
    public String getModel()        { return model; }

    /** @return manufacturing year */
    public int getYearOfMfr()       { return yearOfMfr; }

    /** @return fuel type string */
    public String getFuelType()     { return fuelType; }

    /** @return body colour (may be null) */
    public String getColor()        { return color; }

    /** @return current odometer reading in km */
    public int getOdometerKm()      { return odometerKm; }

    /** @return record creation timestamp */
    public LocalDateTime getCreatedAt() { return createdAt; }

    // ── Setters ───────────────────────────────────────────────────────────────

    /** @param vehicleId surrogate PK (set by DAO after insert) */
    public void setVehicleId(int vehicleId)         { this.vehicleId = vehicleId; }

    /** @param customerId FK to CUSTOMERS */
    public void setCustomerId(int customerId)       { this.customerId = customerId; }

    /** @param registrationNo number plate */
    public void setRegistrationNo(String registrationNo) { this.registrationNo = registrationNo; }

    /** @param make manufacturer name */
    public void setMake(String make)                { this.make = make; }

    /** @param model model name */
    public void setModel(String model)              { this.model = model; }

    /** @param yearOfMfr manufacturing year */
    public void setYearOfMfr(int yearOfMfr)         { this.yearOfMfr = yearOfMfr; }

    /** @param fuelType fuel type string */
    public void setFuelType(String fuelType)        { this.fuelType = fuelType; }

    /** @param color body colour */
    public void setColor(String color)              { this.color = color; }

    /** @param odometerKm odometer reading */
    public void setOdometerKm(int odometerKm)       { this.odometerKm = odometerKm; }

    /** @param createdAt record creation timestamp */
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    // ── Utility ───────────────────────────────────────────────────────────────

    @Override
    public String toString() {
        return "Vehicle{id=" + vehicleId +
               ", reg='" + registrationNo + '\'' +
               ", make='" + make + '\'' +
               ", model='" + model + '\'' +
               ", year=" + yearOfMfr + '}';
    }
}
