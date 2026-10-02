/**
 * BookingPanel — Swing panel for booking vehicle service appointments.
 * Uses GridBagLayout for the form, SpinnerDateModel for date/time selection,
 * and JComboBox populated from the service catalogue.
 * All DB calls happen inside SwingWorker to avoid blocking the EDT.
 *
 * Course  : UIT3361 OOP Java / UIT3311 Database Technology Lab
 * Project : Vehicle Service Management System
 */
package com.garage.ui;

import com.garage.model.JobCard;
import com.garage.model.Service;
import com.garage.model.Vehicle;
import com.garage.service.BookingService;
import com.garage.service.CustomerService;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Panel providing the complete appointment booking workflow:
 * <ol>
 *   <li>Find vehicle by registration number.</li>
 *   <li>Display vehicle details and owner name.</li>
 *   <li>Select service type from the catalogue JComboBox.</li>
 *   <li>Set appointment date/time via JSpinner.</li>
 *   <li>Enter optional remarks.</li>
 *   <li>Click "Book Appointment" → job card is created in BOOKED status.</li>
 * </ol>
 *
 * <p>All database operations execute in {@link SwingWorker#doInBackground()}
 * to keep the Event Dispatch Thread responsive.
 */
public class BookingPanel extends JPanel {

    // ── Services ──────────────────────────────────────────────────────────────
    private final BookingService  bookingService  = new BookingService();
    private final CustomerService customerService = new CustomerService();

    // ── State ─────────────────────────────────────────────────────────────────
    /** Vehicle currently selected by the user; null if none. */
    private Vehicle selectedVehicle = null;

    // ── Vehicle lookup ────────────────────────────────────────────────────────
    private JTextField regNoSearchField;
    private JLabel     vehicleInfoLabel;
    private JLabel     ownerInfoLabel;

    // ── Booking form ──────────────────────────────────────────────────────────
    private JComboBox<Service>  serviceCombo;
    private JSpinner            appointmentSpinner;
    private JTextArea           remarksArea;
    private JButton             bookBtn;

    // ── Status ────────────────────────────────────────────────────────────────
    private JLabel statusLabel;

    // ── Constructor ───────────────────────────────────────────────────────────

    /**
     * Constructs the BookingPanel, builds the UI, and pre-loads the service catalogue.
     */
    public BookingPanel() {
        setLayout(new BorderLayout(0, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        add(buildLookupSection(),  BorderLayout.NORTH);
        add(buildBookingForm(),    BorderLayout.CENTER);
        add(buildStatusBar(),      BorderLayout.SOUTH);

        loadServiceCatalogue();
    }

    // ── UI Builders ───────────────────────────────────────────────────────────

    /**
     * Builds the vehicle lookup section (Step 1).
     *
     * @return the lookup panel
     */
    private JPanel buildLookupSection() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(),
            "Step 1 — Find Vehicle by Registration Number",
            TitledBorder.LEFT, TitledBorder.TOP));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 6, 4, 6);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill   = GridBagConstraints.HORIZONTAL;

        // Row 0: search field + button
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        panel.add(new JLabel("Registration No:"), gbc);

        regNoSearchField = new JTextField(14);
        regNoSearchField.setToolTipText("Enter the number plate (e.g., TN09AB1234)");
        gbc.gridx = 1; gbc.weightx = 1.0;
        panel.add(regNoSearchField, gbc);

        JButton findBtn = new JButton("Find Vehicle");
        findBtn.setMnemonic('F');
        findBtn.addActionListener(e -> onFindVehicle());
        gbc.gridx = 2; gbc.weightx = 0;
        panel.add(findBtn, gbc);

        // Row 1: vehicle info
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        panel.add(new JLabel("Vehicle:"), gbc);

        vehicleInfoLabel = new JLabel("— not found yet —");
        vehicleInfoLabel.setFont(vehicleInfoLabel.getFont().deriveFont(Font.BOLD));
        gbc.gridx = 1; gbc.gridwidth = 2; gbc.weightx = 1.0;
        panel.add(vehicleInfoLabel, gbc);

        // Row 2: owner info
        gbc.gridwidth = 1;
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0;
        panel.add(new JLabel("Owner:"), gbc);

        ownerInfoLabel = new JLabel("—");
        ownerInfoLabel.setFont(ownerInfoLabel.getFont().deriveFont(Font.BOLD));
        gbc.gridx = 1; gbc.gridwidth = 2; gbc.weightx = 1.0;
        panel.add(ownerInfoLabel, gbc);

        return panel;
    }

    /**
     * Builds the booking form section (Steps 2–5).
     *
     * @return the booking form panel
     */
    private JPanel buildBookingForm() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(),
            "Step 2 — Book Appointment",
            TitledBorder.LEFT, TitledBorder.TOP));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 6, 4, 6);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill   = GridBagConstraints.HORIZONTAL;

        // ── Row 0: Service selection ───────────────────────────────────────
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        panel.add(new JLabel("Service Type *:"), gbc);

        serviceCombo = new JComboBox<>();
        serviceCombo.setPrototypeDisplayValue(
            new Service(0, "Loading services...", null, 0, "FIXED", null));
        serviceCombo.setToolTipText("Select the type of service required");
        gbc.gridx = 1; gbc.weightx = 1.0; gbc.gridwidth = 3;
        panel.add(serviceCombo, gbc);

        // ── Row 1: Appointment date/time ───────────────────────────────────
        gbc.gridwidth = 1;
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        panel.add(new JLabel("Appointment Date/Time *:"), gbc);

        // Default: tomorrow at 09:00
        Calendar tomorrow = Calendar.getInstance();
        tomorrow.add(Calendar.DAY_OF_YEAR, 1);
        tomorrow.set(Calendar.HOUR_OF_DAY, 9);
        tomorrow.set(Calendar.MINUTE, 0);
        tomorrow.set(Calendar.SECOND, 0);
        tomorrow.set(Calendar.MILLISECOND, 0);

        SpinnerDateModel dateModel = new SpinnerDateModel(
            tomorrow.getTime(),
            null,           // no minimum (validation done in service layer)
            null,           // no maximum
            Calendar.MINUTE
        );
        appointmentSpinner = new JSpinner(dateModel);
        JSpinner.DateEditor dateEditor = new JSpinner.DateEditor(
            appointmentSpinner, "dd-MMM-yyyy  HH:mm");
        appointmentSpinner.setEditor(dateEditor);
        appointmentSpinner.setToolTipText("Select the appointment date and time");

        gbc.gridx = 1; gbc.weightx = 0.5; gbc.gridwidth = 1;
        panel.add(appointmentSpinner, gbc);

        // ── Row 2: Remarks ─────────────────────────────────────────────────
        gbc.gridwidth = 1;
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        panel.add(new JLabel("Remarks:"), gbc);

        remarksArea = new JTextArea(4, 40);
        remarksArea.setLineWrap(true);
        remarksArea.setWrapStyleWord(true);
        remarksArea.setToolTipText("Optional notes about the service requirement");
        JScrollPane remarksScroll = new JScrollPane(remarksArea);
        gbc.gridx = 1; gbc.gridwidth = 3; gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(remarksScroll, gbc);

        // ── Row 3: Buttons ─────────────────────────────────────────────────
        gbc.anchor = GridBagConstraints.EAST;
        gbc.fill   = GridBagConstraints.NONE;
        gbc.gridx = 0; gbc.gridy = 3;
        gbc.gridwidth = 4;
        gbc.weightx = 0;

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));

        JButton clearBtn = new JButton("Clear Form");
        clearBtn.setMnemonic('C');
        clearBtn.addActionListener(e -> clearBookingForm());
        btnPanel.add(clearBtn);

        bookBtn = new JButton("Book Appointment");
        bookBtn.setMnemonic('B');
        bookBtn.addActionListener(e -> onBookAppointment());
        btnPanel.add(bookBtn);

        panel.add(btnPanel, gbc);

        return panel;
    }

    /**
     * Builds the status bar.
     *
     * @return the status bar panel
     */
    private JPanel buildStatusBar() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        statusLabel = new JLabel(" ");
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.PLAIN, 11f));
        panel.add(statusLabel);
        return panel;
    }

    // ── Event Handlers ────────────────────────────────────────────────────────

    /**
     * Handles the "Find Vehicle" button.
     * Looks up the vehicle by registration and displays owner details.
     */
    private void onFindVehicle() {
        String regNo = regNoSearchField.getText().trim();
        if (regNo.isEmpty()) {
            showError("Please enter a registration number.");
            regNoSearchField.requestFocus();
            return;
        }

        setStatus("Looking up vehicle '" + regNo + "'...");
        vehicleInfoLabel.setText("Searching...");
        vehicleInfoLabel.setForeground(Color.DARK_GRAY);
        ownerInfoLabel.setText("—");

        new SwingWorker<Object[], Void>() {
            @Override
            protected Object[] doInBackground() throws Exception {
                Vehicle v = bookingService.findVehicleByRegistration(regNo).orElse(null);
                if (v == null) return null;
                // Also fetch the owner's name
                String ownerName = customerService.findById(v.getCustomerId())
                    .map(c -> c.getCustomerName() + "  (Ph: " + c.getPhone() + ")")
                    .orElse("Owner not found");
                return new Object[]{v, ownerName};
            }

            @Override
            protected void done() {
                try {
                    Object[] result = get();
                    if (result == null) {
                        selectedVehicle = null;
                        vehicleInfoLabel.setText("✘  No vehicle found for: " + regNo);
                        vehicleInfoLabel.setForeground(Color.RED);
                        ownerInfoLabel.setText("—");
                        setStatus("Vehicle not found.");
                    } else {
                        selectedVehicle = (Vehicle) result[0];
                        String ownerName = (String) result[1];
                        vehicleInfoLabel.setText(
                            "✔  " + selectedVehicle.getMake() +
                            " " + selectedVehicle.getModel() +
                            "  (" + selectedVehicle.getYearOfMfr() + ")  —  " +
                            selectedVehicle.getFuelType());
                        vehicleInfoLabel.setForeground(new Color(0, 120, 0));
                        ownerInfoLabel.setText(ownerName);
                        ownerInfoLabel.setForeground(new Color(0, 90, 160));
                        setStatus("Vehicle found. Select a service and appointment time.");
                    }
                } catch (Exception ex) {
                    selectedVehicle = null;
                    showError("Vehicle lookup failed.\n" + extractMessage(ex));
                    setStatus("Lookup failed.");
                }
            }
        }.execute();
    }

    /**
     * Handles the "Book Appointment" button.
     * Validates inputs and creates a job card via BookingService in a SwingWorker.
     */
    private void onBookAppointment() {
        if (selectedVehicle == null) {
            showError("Please find and select a vehicle first (Step 1).");
            return;
        }

        Service selectedService = (Service) serviceCombo.getSelectedItem();
        if (selectedService == null) {
            showError("Please select a service type.");
            return;
        }

        Date selectedDate = (Date) appointmentSpinner.getValue();
        LocalDateTime appointmentDt = selectedDate.toInstant()
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime();

        String remarks = remarksArea.getText().trim();

        // Pre-flight future-date check in UI for immediate feedback
        if (!appointmentDt.isAfter(LocalDateTime.now())) {
            showError("Appointment date/time must be in the future.\n" +
                      "Please select a date and time after: " +
                      new SimpleDateFormat("dd-MMM-yyyy HH:mm").format(new Date()));
            appointmentSpinner.requestFocus();
            return;
        }

        setStatus("Booking appointment...");
        bookBtn.setEnabled(false);

        final int vehicleId = selectedVehicle.getVehicleId();
        final int serviceId = selectedService.getServiceId();

        new SwingWorker<JobCard, Void>() {
            @Override
            protected JobCard doInBackground() throws Exception {
                return bookingService.bookAppointment(
                    vehicleId, serviceId, appointmentDt,
                    remarks.isEmpty() ? null : remarks);
            }

            @Override
            protected void done() {
                bookBtn.setEnabled(true);
                try {
                    JobCard jc = get();
                    String apptStr = new SimpleDateFormat("dd-MMM-yyyy HH:mm")
                        .format(Date.from(appointmentDt
                            .atZone(ZoneId.systemDefault()).toInstant()));

                    JOptionPane.showMessageDialog(
                        BookingPanel.this,
                        "Appointment booked successfully!\n\n" +
                        "Job Card #" + jc.getJobCardId() + "\n" +
                        "Vehicle : " + selectedVehicle.getRegistrationNo() + "\n" +
                        "Service : " + selectedService.getServiceName() + "\n" +
                        "Date    : " + apptStr + "\n\n" +
                        "Status  : BOOKED",
                        "Appointment Confirmed",
                        JOptionPane.INFORMATION_MESSAGE
                    );

                    setStatus("Job Card #" + jc.getJobCardId() +
                              " created — status: BOOKED.");
                    clearBookingForm();

                } catch (Exception ex) {
                    showError("Booking failed.\n" + extractMessage(ex));
                    setStatus("Booking failed.");
                }
            }
        }.execute();
    }

    // ── Data Loading ──────────────────────────────────────────────────────────

    /**
     * Loads the service catalogue from the DB and populates the JComboBox.
     * Runs in a SwingWorker to avoid blocking the EDT on panel initialisation.
     */
    private void loadServiceCatalogue() {
        setStatus("Loading service catalogue...");

        new SwingWorker<List<Service>, Void>() {
            @Override
            protected List<Service> doInBackground() throws Exception {
                return bookingService.getAllServices();
            }

            @Override
            protected void done() {
                try {
                    List<Service> services = get();
                    serviceCombo.removeAllItems();
                    for (Service s : services) {
                        serviceCombo.addItem(s);
                    }
                    if (services.isEmpty()) {
                        setStatus("Warning: No services found in catalogue. " +
                                  "Ensure schema.sql seed data was applied.");
                    } else {
                        setStatus(services.size() + " service(s) loaded. " +
                                  "Find a vehicle to begin booking.");
                    }
                } catch (Exception ex) {
                    showError("Could not load service catalogue.\n" + extractMessage(ex));
                    setStatus("Failed to load services.");
                }
            }
        }.execute();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Resets all booking form fields to their defaults. */
    private void clearBookingForm() {
        regNoSearchField.setText("");
        remarksArea.setText("");
        selectedVehicle = null;
        vehicleInfoLabel.setText("— not found yet —");
        vehicleInfoLabel.setForeground(Color.DARK_GRAY);
        ownerInfoLabel.setText("—");
        ownerInfoLabel.setForeground(Color.DARK_GRAY);

        // Reset spinner to tomorrow 09:00
        Calendar tomorrow = Calendar.getInstance();
        tomorrow.add(Calendar.DAY_OF_YEAR, 1);
        tomorrow.set(Calendar.HOUR_OF_DAY, 9);
        tomorrow.set(Calendar.MINUTE, 0);
        tomorrow.set(Calendar.SECOND, 0);
        tomorrow.set(Calendar.MILLISECOND, 0);
        appointmentSpinner.setValue(tomorrow.getTime());

        regNoSearchField.requestFocus();
    }

    /** Updates the status bar text. */
    private void setStatus(String message) {
        statusLabel.setText(" " + message);
    }

    /** Shows a validation/error dialog. */
    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Validation Error",
                                      JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Extracts a readable message from a possibly wrapped exception.
     *
     * @param ex the exception
     * @return the cause message if available, otherwise the exception message
     */
    private String extractMessage(Exception ex) {
        Throwable cause = ex.getCause();
        return (cause != null) ? cause.getMessage() : ex.getMessage();
    }
}
