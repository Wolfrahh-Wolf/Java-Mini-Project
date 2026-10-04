/**
 * JobCard — POJO representing a single vehicle service visit record.
 * Maps directly to the JOB_CARDS table in Oracle.
 */
package com.garage.model;

import java.time.LocalDateTime;

/**
 * Plain Java object representing a job card.
 * A job card is created for every vehicle service visit and tracks
 * the full lifecycle: BOOKED → IN_PROGRESS → COMPLETED → DELIVERED.
 *
 * <p>No business logic — only fields, constructors, getters, and setters.
 */
public class JobCard {

    private int           jobCardId;
    private int           vehicleId;
    private int           serviceId;
    private String        technicianName;
    private String        status;           // BOOKED | IN_PROGRESS | COMPLETED | DELIVERED
    private LocalDateTime appointmentDt;
    private LocalDateTime startDt;          // set when status → IN_PROGRESS
    private LocalDateTime completionDt;     // set when status → COMPLETED
    private LocalDateTime deliveryDt;       // set when status → DELIVERED
    private double        labourHours;
    private String        remarks;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ── Constructors ──────────────────────────────────────────────────────────

    /** No-arg constructor required for DAO result-set mapping. */
    public JobCard() {}

    /**
     * Constructor for creating a new BOOKED job card (before DB insert).
     *
     * @param vehicleId     the vehicle being serviced
     * @param serviceId     the service type selected
     * @param appointmentDt the scheduled appointment date/time
     * @param remarks       optional customer/advisor notes
     */
    public JobCard(int vehicleId, int serviceId,
                   LocalDateTime appointmentDt, String remarks) {
        this.vehicleId     = vehicleId;
        this.serviceId     = serviceId;
        this.appointmentDt = appointmentDt;
        this.remarks       = remarks;
        this.status        = "BOOKED";
        this.labourHours   = 0.0;
    }

    /**
     * Full constructor including all DB-assigned fields (used when mapping from ResultSet).
     *
     * @param jobCardId      surrogate PK
     * @param vehicleId      FK to VEHICLES
     * @param serviceId      FK to SERVICES
     * @param technicianName assigned technician (may be null)
     * @param status         lifecycle status string
     * @param appointmentDt  scheduled appointment
     * @param startDt        actual work start time (may be null)
     * @param completionDt   work completion time (may be null)
     * @param deliveryDt     vehicle delivery time (may be null)
     * @param labourHours    actual labour hours recorded
     * @param remarks        technician/advisor notes (may be null)
     * @param createdAt      record creation timestamp
     * @param updatedAt      last modification timestamp
     */
    public JobCard(int jobCardId, int vehicleId, int serviceId,
                   String technicianName, String status,
                   LocalDateTime appointmentDt, LocalDateTime startDt,
                   LocalDateTime completionDt, LocalDateTime deliveryDt,
                   double labourHours, String remarks,
                   LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.jobCardId      = jobCardId;
        this.vehicleId      = vehicleId;
        this.serviceId      = serviceId;
        this.technicianName = technicianName;
        this.status         = status;
        this.appointmentDt  = appointmentDt;
        this.startDt        = startDt;
        this.completionDt   = completionDt;
        this.deliveryDt     = deliveryDt;
        this.labourHours    = labourHours;
        this.remarks        = remarks;
        this.createdAt      = createdAt;
        this.updatedAt      = updatedAt;
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    /** @return surrogate primary key */
    public int getJobCardId()           { return jobCardId; }

    /** @return FK referencing the vehicle being serviced */
    public int getVehicleId()           { return vehicleId; }

    /** @return FK referencing the service type */
    public int getServiceId()           { return serviceId; }

    /** @return assigned technician name (may be null) */
    public String getTechnicianName()   { return technicianName; }

    /** @return current lifecycle status */
    public String getStatus()           { return status; }

    /** @return scheduled appointment date/time */
    public LocalDateTime getAppointmentDt() { return appointmentDt; }

    /** @return actual work start time (null until IN_PROGRESS) */
    public LocalDateTime getStartDt()       { return startDt; }

    /** @return work completion time (null until COMPLETED) */
    public LocalDateTime getCompletionDt()  { return completionDt; }

    /** @return vehicle delivery time (null until DELIVERED) */
    public LocalDateTime getDeliveryDt()    { return deliveryDt; }

    /** @return actual labour hours recorded */
    public double getLabourHours()          { return labourHours; }

    /** @return technician/advisor notes (may be null) */
    public String getRemarks()              { return remarks; }

    /** @return record creation timestamp */
    public LocalDateTime getCreatedAt()     { return createdAt; }

    /** @return last modification timestamp */
    public LocalDateTime getUpdatedAt()     { return updatedAt; }

    // ── Setters ───────────────────────────────────────────────────────────────

    /** @param jobCardId surrogate PK (set by DAO after insert) */
    public void setJobCardId(int jobCardId)             { this.jobCardId = jobCardId; }

    /** @param vehicleId FK to VEHICLES */
    public void setVehicleId(int vehicleId)             { this.vehicleId = vehicleId; }

    /** @param serviceId FK to SERVICES */
    public void setServiceId(int serviceId)             { this.serviceId = serviceId; }

    /** @param technicianName assigned technician name */
    public void setTechnicianName(String technicianName){ this.technicianName = technicianName; }

    /** @param status lifecycle status */
    public void setStatus(String status)                { this.status = status; }

    /** @param appointmentDt scheduled appointment */
    public void setAppointmentDt(LocalDateTime appointmentDt){ this.appointmentDt = appointmentDt; }

    /** @param startDt actual work start time */
    public void setStartDt(LocalDateTime startDt)       { this.startDt = startDt; }

    /** @param completionDt work completion time */
    public void setCompletionDt(LocalDateTime completionDt){ this.completionDt = completionDt; }

    /** @param deliveryDt vehicle delivery time */
    public void setDeliveryDt(LocalDateTime deliveryDt) { this.deliveryDt = deliveryDt; }

    /** @param labourHours actual labour hours */
    public void setLabourHours(double labourHours)      { this.labourHours = labourHours; }

    /** @param remarks technician/advisor notes */
    public void setRemarks(String remarks)              { this.remarks = remarks; }

    /** @param createdAt record creation timestamp */
    public void setCreatedAt(LocalDateTime createdAt)   { this.createdAt = createdAt; }

    /** @param updatedAt last modification timestamp */
    public void setUpdatedAt(LocalDateTime updatedAt)   { this.updatedAt = updatedAt; }

    // ── Utility ───────────────────────────────────────────────────────────────

    @Override
    public String toString() {
        return "JobCard{id=" + jobCardId +
               ", vehicleId=" + vehicleId +
               ", status='" + status + '\'' +
               ", appt=" + appointmentDt + '}';
    }
}
