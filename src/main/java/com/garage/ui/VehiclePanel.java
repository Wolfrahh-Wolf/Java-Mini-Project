/**
 * VehiclePanel — Swing panel for vehicle registration and vehicle list display.
 * The user first searches for a customer by phone, then registers a vehicle under them.
 * Uses BorderLayout + GridBagLayout for forms, AbstractTableModel for the vehicle list.
 * All DB calls happen inside SwingWorker to avoid blocking the EDT.
 *
 * Course  : UIT3361 OOP Java / UIT3311 Database Technology Lab
 * Project : Vehicle Service Management System
 */
package com.garage.ui;

import com.garage.model.Customer;
import com.garage.model.Vehicle;
import com.garage.service.CustomerService;
import com.garage.service.VehicleService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Panel providing:
 * <ul>
 *   <li>A customer lookup section (search by phone) to select the vehicle owner.</li>
 *   <li>A vehicle registration form: RegNo, Make, Model, Year, Fuel Type, Colour, Odometer.</li>
 *   <li>A JTable showing all vehicles for the selected customer.</li>
 * </ul>
 */
public class VehiclePanel extends JPanel {

    // ── Services ──────────────────────────────────────────────────────────────
    private final CustomerService customerService = new CustomerService();
    private final VehicleService  vehicleService  = new VehicleService();

    // ── Selected customer state ───────────────────────────────────────────────
    /** The customer currently selected via the phone lookup. */
    private Customer selectedCustomer = null;

    // ── Customer lookup ────────────────────────────────────────────────────────
    private JTextField customerPhoneField;
    private JLabel     customerInfoLabel;

    // ── Vehicle form fields ───────────────────────────────────────────────────
    private JTextField regNoField;
    private JTextField makeField;
    private JTextField modelField;
    private JTextField yearField;
    private JComboBox<String> fuelTypeCombo;
    private JTextField colorField;
    private JTextField odometerField;

    // ── Table ─────────────────────────────────────────────────────────────────
    private VehicleTableModel tableModel;
    private JTable            vehicleTable;

    // ── Status ────────────────────────────────────────────────────────────────
    private JLabel statusLabel;

    // ── Fuel type options (must match DB CHECK constraint) ────────────────────
    private static final String[] FUEL_TYPES =
        {"PETROL", "DIESEL", "ELECTRIC", "HYBRID", "CNG", "LPG"};

    // ── Constructor ───────────────────────────────────────────────────────────

    /**
     * Constructs the VehiclePanel and initialises the UI.
     */
    public VehiclePanel() {
        setLayout(new BorderLayout(0, 0));
        setBackground(FluentTheme.CANVAS);

        add(buildTopSection(),   BorderLayout.NORTH);
        add(buildTableSection(), BorderLayout.CENTER);
        add(buildStatusBar(),    BorderLayout.SOUTH);
    }

    // ── UI Builders ───────────────────────────────────────────────────────────

    /**
     * Builds the top section: customer lookup card + vehicle form card stacked vertically.
     *
     * @return the assembled top panel
     */
    private JPanel buildTopSection() {
        JPanel wrapper = new JPanel(new BorderLayout(0, 0));
        wrapper.setBackground(FluentTheme.CANVAS);
        wrapper.add(buildCustomerLookupCard(), BorderLayout.NORTH);
        wrapper.add(buildVehicleFormCard(),    BorderLayout.CENTER);
        return wrapper;
    }

    /**
     * Builds the customer lookup card (Step 1).
     *
     * @return the customer lookup panel
     */
    private JPanel buildCustomerLookupCard() {
        JPanel card = new JPanel(new BorderLayout(0, 0));
        card.setBackground(FluentTheme.SURFACE);
        card.setBorder(new MatteBorder(0, 0, 1, 0, FluentTheme.BORDER));

        card.add(FluentTheme.sectionHeader("Step 1 - Select Customer Owner"), BorderLayout.NORTH);

        JPanel inner = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 10));
        inner.setBackground(FluentTheme.SURFACE);
        inner.setBorder(new EmptyBorder(0, FluentTheme.PADDING, 4, FluentTheme.PADDING));

        JLabel phoneLbl = new JLabel("Customer Phone:");
        phoneLbl.setFont(FluentTheme.FONT_BODY);
        phoneLbl.setForeground(FluentTheme.TEXT_MUTED);
        inner.add(phoneLbl);

        customerPhoneField = new JTextField(16);
        FluentTheme.styleTextField(customerPhoneField);
        customerPhoneField.setToolTipText("Enter phone number and click Find");
        inner.add(customerPhoneField);

        JButton findBtn = FluentTheme.secondaryButton("Find Customer");
        findBtn.setMnemonic('F');
        findBtn.addActionListener(e -> onFindCustomer());
        inner.add(findBtn);

        customerInfoLabel = new JLabel("  No customer selected.");
        customerInfoLabel.setFont(FluentTheme.FONT_BODY);
        customerInfoLabel.setForeground(FluentTheme.TEXT_MUTED);
        inner.add(customerInfoLabel);

        card.add(inner, BorderLayout.CENTER);
        return card;
    }

    /**
     * Builds the vehicle registration form card (Step 2).
     *
     * @return the vehicle form panel
     */
    private JPanel buildVehicleFormCard() {
        JPanel card = new JPanel(new BorderLayout(0, 0));
        card.setBackground(FluentTheme.SURFACE_ALT);
        card.setBorder(new MatteBorder(0, 0, 1, 0, FluentTheme.BORDER));

        card.add(FluentTheme.sectionHeader("Step 2 - Register Vehicle"), BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(FluentTheme.SURFACE_ALT);
        form.setBorder(new EmptyBorder(FluentTheme.PADDING, FluentTheme.PADDING,
                                       FluentTheme.PADDING, FluentTheme.PADDING));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 8, 6, 8);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill   = GridBagConstraints.HORIZONTAL;

        // ── Row 0: Reg No / Make ───────────────────────────────────────────────
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        form.add(makeLabel("Registration No *"), gbc);
        regNoField = makeTextField("Number plate, e.g. TN09AB1234");
        gbc.gridx = 1; gbc.weightx = 1.0;
        form.add(regNoField, gbc);

        gbc.gridx = 2; gbc.weightx = 0;
        form.add(makeLabel("Make *"), gbc);
        makeField = makeTextField("Manufacturer, e.g. Toyota");
        gbc.gridx = 3; gbc.weightx = 1.0;
        form.add(makeField, gbc);

        // ── Row 1: Model / Year ────────────────────────────────────────────────
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        form.add(makeLabel("Model *"), gbc);
        modelField = makeTextField("Model name, e.g. Camry");
        gbc.gridx = 1; gbc.weightx = 1.0;
        form.add(modelField, gbc);

        gbc.gridx = 2; gbc.weightx = 0;
        form.add(makeLabel("Year *"), gbc);
        yearField = makeTextField("Year of manufacture");
        gbc.gridx = 3; gbc.weightx = 0.5;
        form.add(yearField, gbc);

        // ── Row 2: Fuel Type / Colour ──────────────────────────────────────────
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0;
        form.add(makeLabel("Fuel Type *"), gbc);
        fuelTypeCombo = new JComboBox<>(FUEL_TYPES);
        FluentTheme.styleCombo(fuelTypeCombo);
        gbc.gridx = 1; gbc.weightx = 1.0;
        form.add(fuelTypeCombo, gbc);

        gbc.gridx = 2; gbc.weightx = 0;
        form.add(makeLabel("Colour"), gbc);
        colorField = makeTextField("Optional - body colour");
        gbc.gridx = 3; gbc.weightx = 1.0;
        form.add(colorField, gbc);

        // ── Row 3: Odometer ───────────────────────────────────────────────────
        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0;
        form.add(makeLabel("Odometer (km)"), gbc);
        odometerField = makeTextField("Current odometer reading in km");
        odometerField.setText("0");
        gbc.gridx = 1; gbc.weightx = 0.5;
        form.add(odometerField, gbc);

        // ── Row 4: Buttons ─────────────────────────────────────────────────────
        gbc.gridx = 0; gbc.gridy = 4;
        gbc.gridwidth = 4;
        gbc.fill   = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.weightx = 0;

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnRow.setBackground(FluentTheme.SURFACE_ALT);

        JButton clearBtn = FluentTheme.secondaryButton("Clear");
        clearBtn.setMnemonic('C');
        clearBtn.addActionListener(e -> clearVehicleForm());
        btnRow.add(clearBtn);

        JButton registerBtn = FluentTheme.accentButton("Register Vehicle");
        registerBtn.setMnemonic('R');
        registerBtn.addActionListener(e -> onRegisterVehicle());
        btnRow.add(registerBtn);

        form.add(btnRow, gbc);
        card.add(form, BorderLayout.CENTER);
        return card;
    }

    /**
     * Builds the vehicle table section.
     *
     * @return the table panel
     */
    private JPanel buildTableSection() {
        JPanel panel = new JPanel(new BorderLayout(0, 0));
        panel.setBackground(FluentTheme.CANVAS);

        panel.add(FluentTheme.sectionHeader("Vehicles for Selected Customer"), BorderLayout.NORTH);

        tableModel   = new VehicleTableModel();
        vehicleTable = new JTable(tableModel);
        FluentTheme.styleTable(vehicleTable);
        vehicleTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        DefaultTableCellRenderer paddedRenderer = new DefaultTableCellRenderer();
        paddedRenderer.setBorder(new EmptyBorder(0, 12, 0, 12));
        for (int i = 0; i < tableModel.getColumnCount(); i++) {
            vehicleTable.getColumnModel().getColumn(i).setCellRenderer(paddedRenderer);
        }

        // Column widths
        vehicleTable.getColumnModel().getColumn(0).setPreferredWidth(55);
        vehicleTable.getColumnModel().getColumn(1).setPreferredWidth(120);
        vehicleTable.getColumnModel().getColumn(2).setPreferredWidth(120);
        vehicleTable.getColumnModel().getColumn(3).setPreferredWidth(120);
        vehicleTable.getColumnModel().getColumn(4).setPreferredWidth(60);
        vehicleTable.getColumnModel().getColumn(5).setPreferredWidth(80);
        vehicleTable.getColumnModel().getColumn(6).setPreferredWidth(100);
        vehicleTable.getColumnModel().getColumn(7).setPreferredWidth(110);

        JScrollPane scroll = new JScrollPane(vehicleTable);
        scroll.setBackground(FluentTheme.SURFACE);
        scroll.getViewport().setBackground(FluentTheme.SURFACE);
        scroll.setBorder(BorderFactory.createLineBorder(FluentTheme.BORDER, 1));

        panel.add(scroll, BorderLayout.CENTER);
        return panel;
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
     * Handles the "Find Customer" button. Searches by phone and populates selectedCustomer.
     */
    private void onFindCustomer() {
        String phone = customerPhoneField.getText().trim();
        if (phone.isEmpty()) {
            showError("Please enter a customer phone number.");
            return;
        }

        setStatus("Looking up customer…");

        new SwingWorker<Customer, Void>() {
            @Override
            protected Customer doInBackground() throws Exception {
                return customerService.findByPhone(phone).orElse(null);
            }

            @Override
            protected void done() {
                try {
                    Customer c = get();
                    if (c != null) {
                        selectedCustomer = c;
                        customerInfoLabel.setText("  ✔  " + c.getCustomerName() +
                                                  "  (ID " + c.getCustomerId() + ")");
                        customerInfoLabel.setForeground(FluentTheme.STATUS_SUCCESS);
                        setStatus("Customer found. Fill in vehicle details and click Register.");
                        loadVehiclesForCustomer(c.getCustomerId());
                    } else {
                        selectedCustomer = null;
                        customerInfoLabel.setText("  ✘  No customer found for phone: " + phone);
                        customerInfoLabel.setForeground(FluentTheme.STATUS_ERROR);
                        tableModel.setData(new ArrayList<>());
                        setStatus("Customer not found.");
                    }
                } catch (Exception ex) {
                    showError("Customer lookup failed.\n" + extractMessage(ex));
                    setStatus("Lookup failed.");
                }
            }
        }.execute();
    }

    /**
     * Handles the "Register Vehicle" button click.
     */
    private void onRegisterVehicle() {
        if (selectedCustomer == null) {
            showError("Please find and select a customer first (Step 1).");
            return;
        }

        String regNo    = regNoField.getText().trim();
        String make     = makeField.getText().trim();
        String model    = modelField.getText().trim();
        String yearStr  = yearField.getText().trim();
        String fuelType = (String) fuelTypeCombo.getSelectedItem();
        String color    = colorField.getText().trim();
        String odomStr  = odometerField.getText().trim();

        if (regNo.isEmpty())   { showError("Registration number is required."); regNoField.requestFocus();  return; }
        if (make.isEmpty())    { showError("Make (manufacturer) is required."); makeField.requestFocus();   return; }
        if (model.isEmpty())   { showError("Model name is required.");          modelField.requestFocus();  return; }
        if (yearStr.isEmpty()) { showError("Year of manufacture is required."); yearField.requestFocus();   return; }

        int year;
        try { year = Integer.parseInt(yearStr); }
        catch (NumberFormatException ex) {
            showError("Year must be a valid 4-digit number.");
            yearField.requestFocus();
            return;
        }

        int odometer = 0;
        if (!odomStr.isEmpty()) {
            try { odometer = Integer.parseInt(odomStr); }
            catch (NumberFormatException ex) {
                showError("Odometer must be a non-negative integer.");
                odometerField.requestFocus();
                return;
            }
        }

        Vehicle vehicle = new Vehicle(
            selectedCustomer.getCustomerId(),
            regNo, make, model, year, fuelType,
            color.isEmpty() ? null : color,
            odometer
        );

        setStatus("Registering vehicle…");

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                vehicleService.registerVehicle(vehicle);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    setStatus("Vehicle '" + vehicle.getRegistrationNo() +
                              "' registered (ID " + vehicle.getVehicleId() + ").");
                    clearVehicleForm();
                    loadVehiclesForCustomer(selectedCustomer.getCustomerId());
                } catch (Exception ex) {
                    showError("Failed to register vehicle.\n" + extractMessage(ex));
                    setStatus("Registration failed.");
                }
            }
        }.execute();
    }

    // ── Data Loading ──────────────────────────────────────────────────────────

    /**
     * Loads all vehicles for the given customer from the DB and refreshes the table.
     *
     * @param customerId the CUSTOMER_ID whose vehicles to load
     */
    private void loadVehiclesForCustomer(int customerId) {
        setStatus("Loading vehicles…");

        new SwingWorker<List<Vehicle>, Void>() {
            @Override
            protected List<Vehicle> doInBackground() throws Exception {
                return vehicleService.getVehiclesForCustomer(customerId);
            }

            @Override
            protected void done() {
                try {
                    List<Vehicle> data = get();
                    tableModel.setData(data);
                    setStatus(data.size() + " vehicle(s) for " +
                              selectedCustomer.getCustomerName() + ".");
                } catch (Exception ex) {
                    showError("Could not load vehicles.\n" + extractMessage(ex));
                    setStatus("Load failed.");
                }
            }
        }.execute();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Clears all vehicle form input fields. */
    private void clearVehicleForm() {
        regNoField.setText("");
        makeField.setText("");
        modelField.setText("");
        yearField.setText("");
        fuelTypeCombo.setSelectedIndex(0);
        colorField.setText("");
        odometerField.setText("0");
        regNoField.requestFocus();
    }

    /** Updates the status bar text. */
    private void setStatus(String message) { statusLabel.setText(" " + message); }

    /** Shows a validation error dialog per AGENTS.md §4.5. */
    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Validation Error",
                                      JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Extracts a readable message from a (possibly wrapped) exception.
     *
     * @param ex the exception
     * @return the cause message or the exception's own message
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

    /** Creates a styled input JTextField with placeholder tooltip. */
    private JTextField makeTextField(String tooltip) {
        JTextField f = new JTextField();
        f.setToolTipText(tooltip);
        FluentTheme.styleTextField(f);
        return f;
    }

    // ── Inner Table Model ─────────────────────────────────────────────────────

    /**
     * AbstractTableModel backing the vehicle JTable.
     */
    private static class VehicleTableModel extends AbstractTableModel {

        private static final String[] COLUMNS =
            {"ID", "Reg No", "Make", "Model", "Year", "Fuel", "Colour", "Odometer (km)"};

        private List<Vehicle> data = new ArrayList<>();

        /**
         * Replaces data and notifies listeners.
         *
         * @param vehicles the new list of vehicles
         */
        public void setData(List<Vehicle> vehicles) {
            this.data = (vehicles != null) ? vehicles : new ArrayList<>();
            fireTableDataChanged();
        }

        @Override public int getRowCount()    { return data.size(); }
        @Override public int getColumnCount() { return COLUMNS.length; }
        @Override public String getColumnName(int col) { return COLUMNS[col]; }
        @Override public boolean isCellEditable(int row, int col) { return false; }

        @Override
        public Object getValueAt(int row, int col) {
            Vehicle v = data.get(row);
            return switch (col) {
                case 0 -> v.getVehicleId();
                case 1 -> v.getRegistrationNo();
                case 2 -> v.getMake();
                case 3 -> v.getModel();
                case 4 -> v.getYearOfMfr();
                case 5 -> v.getFuelType();
                case 6 -> v.getColor() != null ? v.getColor() : "";
                case 7 -> v.getOdometerKm();
                default -> "";
            };
        }

        @Override
        public Class<?> getColumnClass(int col) {
            return switch (col) {
                case 0, 4, 7 -> Integer.class;
                default -> String.class;
            };
        }
    }
}
