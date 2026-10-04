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
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
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
 *   <li>Click "Book Appointment" — job card is created in BOOKED status.</li>
 * </ol>
 *
 * <p>All database operations execute in {@link SwingWorker#doInBackground()}.
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
    private JComboBox<Service> serviceCombo;
    private JSpinner           appointmentSpinner;
    private JTextArea          remarksArea;
    private JButton            bookBtn;

    // ── Status ────────────────────────────────────────────────────────────────
    private JLabel statusLabel;

    // ── Constructor ───────────────────────────────────────────────────────────

    /**
     * Constructs the BookingPanel, builds the UI, and pre-loads the service catalogue.
     */
    public BookingPanel() {
        setLayout(new BorderLayout(0, 0));
        setBackground(FluentTheme.CANVAS);

        add(buildLookupCard(),  BorderLayout.NORTH);
        add(buildBookingCard(), BorderLayout.CENTER);
        add(buildStatusBar(),   BorderLayout.SOUTH);

        loadServiceCatalogue();
    }

    // ── UI Builders ───────────────────────────────────────────────────────────

    /**
     * Builds the vehicle lookup card (Step 1).
     *
     * @return the lookup panel
     */
    private JPanel buildLookupCard() {
        JPanel card = new JPanel(new BorderLayout(0, 0));
        card.setBackground(FluentTheme.SURFACE);
        card.setBorder(new MatteBorder(0, 0, 1, 0, FluentTheme.BORDER));

        card.add(FluentTheme.sectionHeader("Step 1 - Find Vehicle by Registration Number"),
                 BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(FluentTheme.SURFACE);
        form.setBorder(new EmptyBorder(FluentTheme.PADDING, FluentTheme.PADDING,
                                       FluentTheme.PADDING, FluentTheme.PADDING));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 8, 6, 8);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill   = GridBagConstraints.HORIZONTAL;

        // Row 0: search field + button
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        form.add(makeLabel("Registration No:"), gbc);

        regNoSearchField = new JTextField(16);
        FluentTheme.styleTextField(regNoSearchField);
        regNoSearchField.setToolTipText("Enter the number plate (e.g., TN09AB1234)");
        gbc.gridx = 1; gbc.weightx = 1.0;
        form.add(regNoSearchField, gbc);

        JButton findBtn = FluentTheme.secondaryButton("Find Vehicle");
        findBtn.setMnemonic('F');
        findBtn.addActionListener(e -> onFindVehicle());
        gbc.gridx = 2; gbc.weightx = 0;
        form.add(findBtn, gbc);

        // Row 1: vehicle info
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        form.add(makeLabel("Vehicle:"), gbc);

        vehicleInfoLabel = new JLabel("- not found yet -");
        vehicleInfoLabel.setFont(FluentTheme.FONT_SEMIBOLD);
        vehicleInfoLabel.setForeground(FluentTheme.TEXT_MUTED);
        gbc.gridx = 1; gbc.gridwidth = 2; gbc.weightx = 1.0;
        form.add(vehicleInfoLabel, gbc);

        // Row 2: owner info
        gbc.gridwidth = 1;
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0;
        form.add(makeLabel("Owner:"), gbc);

        ownerInfoLabel = new JLabel("-");
        ownerInfoLabel.setFont(FluentTheme.FONT_SEMIBOLD);
        ownerInfoLabel.setForeground(FluentTheme.TEXT_MUTED);
        gbc.gridx = 1; gbc.gridwidth = 2; gbc.weightx = 1.0;
        form.add(ownerInfoLabel, gbc);

        card.add(form, BorderLayout.CENTER);
        return card;
    }

    /**
     * Builds the booking form card (Steps 2–5).
     *
     * @return the booking form panel
     */
    private JPanel buildBookingCard() {
        JPanel card = new JPanel(new BorderLayout(0, 0));
        card.setBackground(FluentTheme.SURFACE_ALT);
        card.setBorder(new MatteBorder(0, 0, 1, 0, FluentTheme.BORDER));

        card.add(FluentTheme.sectionHeader("Step 2 - Book Appointment"), BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(FluentTheme.SURFACE_ALT);
        form.setBorder(new EmptyBorder(FluentTheme.PADDING, FluentTheme.PADDING,
                                       FluentTheme.PADDING, FluentTheme.PADDING));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 8, 6, 8);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill   = GridBagConstraints.HORIZONTAL;

        // ── Row 0: Service selection ───────────────────────────────────────────
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        form.add(makeLabel("Service Type *:"), gbc);

        serviceCombo = new JComboBox<>();
        serviceCombo.setPrototypeDisplayValue(
            new Service(0, "Loading services...", null, 0, "FIXED", null));
        FluentTheme.styleCombo(serviceCombo);
        serviceCombo.setToolTipText("Select the type of service required");
        gbc.gridx = 1; gbc.weightx = 1.0; gbc.gridwidth = 3;
        form.add(serviceCombo, gbc);

        // ── Row 1: Date/Time ──────────────────────────────────────────────────
        gbc.gridwidth = 1;
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        form.add(makeLabel("Appointment Date/Time *:"), gbc);

        Calendar tomorrow = Calendar.getInstance();
        tomorrow.add(Calendar.DAY_OF_YEAR, 1);
        tomorrow.set(Calendar.HOUR_OF_DAY, 9);
        tomorrow.set(Calendar.MINUTE, 0);
        tomorrow.set(Calendar.SECOND, 0);
        tomorrow.set(Calendar.MILLISECOND, 0);

        SpinnerDateModel dateModel = new SpinnerDateModel(
            tomorrow.getTime(), null, null, Calendar.MINUTE);
        appointmentSpinner = new JSpinner(dateModel);
        JSpinner.DateEditor dateEditor = new JSpinner.DateEditor(
            appointmentSpinner, "dd-MMM-yyyy  HH:mm");
        appointmentSpinner.setEditor(dateEditor);
        appointmentSpinner.setBackground(FluentTheme.INPUT_BG);
        appointmentSpinner.setToolTipText("Select the appointment date and time");

        gbc.gridx = 1; gbc.weightx = 0.5; gbc.gridwidth = 1;
        form.add(appointmentSpinner, gbc);

        // ── Row 2: Remarks ────────────────────────────────────────────────────
        gbc.gridwidth = 1;
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        form.add(makeLabel("Remarks:"), gbc);

        remarksArea = new JTextArea(4, 40);
        remarksArea.setLineWrap(true);
        remarksArea.setWrapStyleWord(true);
        remarksArea.setToolTipText("Optional notes about the service requirement");
        FluentTheme.styleTextArea(remarksArea);
        JScrollPane remarksScroll = new JScrollPane(remarksArea);
        remarksScroll.setBorder(BorderFactory.createLineBorder(FluentTheme.BORDER, 1));
        remarksScroll.getViewport().setBackground(FluentTheme.INPUT_BG);
        gbc.gridx = 1; gbc.gridwidth = 3; gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        form.add(remarksScroll, gbc);

        // ── Row 3: Buttons ────────────────────────────────────────────────────
        gbc.anchor = GridBagConstraints.EAST;
        gbc.fill   = GridBagConstraints.NONE;
        gbc.gridx = 0; gbc.gridy = 3;
        gbc.gridwidth = 4;
        gbc.weightx = 0;

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnRow.setBackground(FluentTheme.SURFACE_ALT);

        JButton clearBtn = FluentTheme.secondaryButton("Clear Form");
        clearBtn.setMnemonic('C');
        clearBtn.addActionListener(e -> clearBookingForm());
        btnRow.add(clearBtn);

        bookBtn = FluentTheme.accentButton("Book Appointment");
        bookBtn.setMnemonic('B');
        bookBtn.addActionListener(e -> onBookAppointment());
        btnRow.add(bookBtn);

        form.add(btnRow, gbc);
        card.add(form, BorderLayout.CENTER);
        return card;
    }

    /**
     * Builds the status bar.
     *
     * @return the status bar panel
     */
    private JPanel buildStatusBar() {
        statusLabel = new JLabel(" ");
        return FluentTheme.statusBar(statusLabel);
    }

    // ── Event Handlers ────────────────────────────────────────────────────────

    /**
     * Handles the "Find Vehicle" button.
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
        vehicleInfoLabel.setForeground(FluentTheme.TEXT_MUTED);
        ownerInfoLabel.setText("-");

        new SwingWorker<Object[], Void>() {
            @Override
            protected Object[] doInBackground() throws Exception {
                Vehicle v = bookingService.findVehicleByRegistration(regNo).orElse(null);
                if (v == null) return null;
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
                        vehicleInfoLabel.setForeground(FluentTheme.STATUS_ERROR);
                        ownerInfoLabel.setText("-");
                        ownerInfoLabel.setForeground(FluentTheme.TEXT_MUTED);
                        setStatus("Vehicle not found.");
                    } else {
                        selectedVehicle = (Vehicle) result[0];
                        String ownerName = (String) result[1];
                        vehicleInfoLabel.setText("✔  " + selectedVehicle.getMake() +
                            " " + selectedVehicle.getModel() +
                            "  (" + selectedVehicle.getYearOfMfr() + ")  -  " +
                            selectedVehicle.getFuelType());
                        vehicleInfoLabel.setForeground(FluentTheme.STATUS_SUCCESS);
                        ownerInfoLabel.setText(ownerName);
                        ownerInfoLabel.setForeground(FluentTheme.STATUS_INFO);
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
            .atZone(ZoneId.systemDefault()).toLocalDateTime();

        String remarks = remarksArea.getText().trim();

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

                    setStatus("Job Card #" + jc.getJobCardId() + " created - status: BOOKED.");
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
                    for (Service s : services) serviceCombo.addItem(s);
                    if (services.isEmpty()) {
                        setStatus("Warning: No services found in catalogue. " +
                                  "Ensure schema.sql seed data was applied.");
                    } else {
                        setStatus(services.size() + " service(s) loaded. Find a vehicle to begin booking.");
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
        vehicleInfoLabel.setText("- not found yet -");
        vehicleInfoLabel.setForeground(FluentTheme.TEXT_MUTED);
        ownerInfoLabel.setText("-");
        ownerInfoLabel.setForeground(FluentTheme.TEXT_MUTED);

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
    private void setStatus(String message) { statusLabel.setText(" " + message); }

    /** Shows a validation/error dialog per AGENTS.md §4.5. */
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

    /** Creates a styled muted-foreground JLabel. */
    private JLabel makeLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(FluentTheme.FONT_BODY);
        lbl.setForeground(FluentTheme.TEXT_MUTED);
        return lbl;
    }
}
