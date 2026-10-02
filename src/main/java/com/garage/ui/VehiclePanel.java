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
import javax.swing.border.TitledBorder;
import javax.swing.table.AbstractTableModel;
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
        setLayout(new BorderLayout(0, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        add(buildTopSection(),   BorderLayout.NORTH);
        add(buildTableSection(), BorderLayout.CENTER);
        add(buildStatusBar(),    BorderLayout.SOUTH);
    }

    // ── UI Builders ───────────────────────────────────────────────────────────

    /**
     * Builds the top section: customer lookup + vehicle registration form stacked vertically.
     *
     * @return the assembled top panel
     */
    private JPanel buildTopSection() {
        JPanel wrapper = new JPanel(new BorderLayout(0, 6));
        wrapper.add(buildCustomerLookup(), BorderLayout.NORTH);
        wrapper.add(buildVehicleForm(),    BorderLayout.CENTER);
        return wrapper;
    }

    /**
     * Builds the customer lookup sub-panel.
     *
     * @return the customer lookup panel
     */
    private JPanel buildCustomerLookup() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(), "Step 1 — Select Customer Owner",
            TitledBorder.LEFT, TitledBorder.TOP));

        panel.add(new JLabel("Customer Phone:"));

        customerPhoneField = new JTextField(14);
        customerPhoneField.setToolTipText("Enter phone number and click Find");
        panel.add(customerPhoneField);

        JButton findBtn = new JButton("Find Customer");
        findBtn.setMnemonic('F');
        findBtn.addActionListener(e -> onFindCustomer());
        panel.add(findBtn);

        customerInfoLabel = new JLabel("  No customer selected.");
        customerInfoLabel.setFont(customerInfoLabel.getFont().deriveFont(Font.BOLD));
        customerInfoLabel.setForeground(Color.DARK_GRAY);
        panel.add(customerInfoLabel);

        return panel;
    }

    /**
     * Builds the vehicle registration form using GridBagLayout.
     *
     * @return the vehicle registration form panel
     */
    private JPanel buildVehicleForm() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(), "Step 2 — Register Vehicle",
            TitledBorder.LEFT, TitledBorder.TOP));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 6, 4, 6);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill   = GridBagConstraints.HORIZONTAL;

        // ── Row 0 ─────────────────────────────────────────────────────────
        addFormRow(panel, gbc, 0, 0, "Registration No *:", regNoField = new JTextField(12),
                   2, 3, "Make *:", makeField = new JTextField(14));

        // ── Row 1 ─────────────────────────────────────────────────────────
        addFormRow(panel, gbc, 0, 1, "Model *:", modelField = new JTextField(14),
                   2, 3, "Year *:", yearField = new JTextField(6));

        // ── Row 2 ─────────────────────────────────────────────────────────
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0;
        panel.add(new JLabel("Fuel Type *:"), gbc);

        fuelTypeCombo = new JComboBox<>(FUEL_TYPES);
        gbc.gridx = 1; gbc.weightx = 0.5;
        panel.add(fuelTypeCombo, gbc);

        gbc.gridx = 2; gbc.weightx = 0;
        panel.add(new JLabel("Colour:"), gbc);

        colorField = new JTextField(14);
        colorField.setToolTipText("Optional — body colour");
        gbc.gridx = 3; gbc.weightx = 0.5;
        panel.add(colorField, gbc);

        // ── Row 3 ─────────────────────────────────────────────────────────
        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0;
        panel.add(new JLabel("Odometer (km):"), gbc);

        odometerField = new JTextField(8);
        odometerField.setText("0");
        odometerField.setToolTipText("Current odometer reading in km");
        gbc.gridx = 1; gbc.weightx = 0.5;
        panel.add(odometerField, gbc);

        // ── Row 4: Buttons ────────────────────────────────────────────────
        gbc.gridx = 0; gbc.gridy = 4;
        gbc.gridwidth = 4;
        gbc.fill   = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.weightx = 0;

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        JButton clearBtn    = new JButton("Clear");
        JButton registerBtn = new JButton("Register Vehicle");

        clearBtn.setMnemonic('C');
        registerBtn.setMnemonic('R');

        clearBtn.addActionListener(e -> clearVehicleForm());
        registerBtn.addActionListener(e -> onRegisterVehicle());

        btnPanel.add(clearBtn);
        btnPanel.add(registerBtn);
        panel.add(btnPanel, gbc);

        return panel;
    }

    /**
     * Helper to add a label + field pair in a GridBagLayout row (two columns per row).
     */
    private void addFormRow(JPanel panel, GridBagConstraints gbc,
                            int col1, int row, String label1, JTextField field1,
                            int col2, int col3, String label2, JTextField field2) {
        gbc.gridx = col1; gbc.gridy = row; gbc.weightx = 0; gbc.gridwidth = 1;
        panel.add(new JLabel(label1), gbc);
        gbc.gridx = col2 - 1; gbc.weightx = 1.0;
        panel.add(field1, gbc);
        gbc.gridx = col2; gbc.weightx = 0;
        panel.add(new JLabel(label2), gbc);
        gbc.gridx = col3; gbc.weightx = 1.0;
        panel.add(field2, gbc);
    }

    /**
     * Builds the vehicle table section.
     *
     * @return the table panel
     */
    private JPanel buildTableSection() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(), "Vehicles for Selected Customer",
            TitledBorder.LEFT, TitledBorder.TOP));

        tableModel   = new VehicleTableModel();
        vehicleTable = new JTable(tableModel);
        vehicleTable.setRowHeight(22);
        vehicleTable.setFillsViewportHeight(true);
        vehicleTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        vehicleTable.getTableHeader().setReorderingAllowed(false);

        // Column widths
        vehicleTable.getColumnModel().getColumn(0).setPreferredWidth(50);
        vehicleTable.getColumnModel().getColumn(1).setPreferredWidth(110);
        vehicleTable.getColumnModel().getColumn(2).setPreferredWidth(110);
        vehicleTable.getColumnModel().getColumn(3).setPreferredWidth(110);
        vehicleTable.getColumnModel().getColumn(4).setPreferredWidth(50);
        vehicleTable.getColumnModel().getColumn(5).setPreferredWidth(80);
        vehicleTable.getColumnModel().getColumn(6).setPreferredWidth(100);
        vehicleTable.getColumnModel().getColumn(7).setPreferredWidth(90);

        panel.add(new JScrollPane(vehicleTable), BorderLayout.CENTER);
        return panel;
    }

    /**
     * Builds the status bar.
     *
     * @return the status panel
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
                        customerInfoLabel.setText(
                            "  ✔  " + c.getCustomerName() + "  (ID " + c.getCustomerId() + ")");
                        customerInfoLabel.setForeground(new Color(0, 130, 0));
                        setStatus("Customer found. Fill in vehicle details and click Register.");
                        loadVehiclesForCustomer(c.getCustomerId());
                    } else {
                        selectedCustomer = null;
                        customerInfoLabel.setText("  ✘  No customer found for phone: " + phone);
                        customerInfoLabel.setForeground(Color.RED);
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
     * Validates all fields and then inserts via VehicleService in a SwingWorker.
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

        // UI-level validation
        if (regNo.isEmpty())  { showError("Registration number is required."); regNoField.requestFocus();  return; }
        if (make.isEmpty())   { showError("Make (manufacturer) is required."); makeField.requestFocus();   return; }
        if (model.isEmpty())  { showError("Model name is required.");          modelField.requestFocus();  return; }
        if (yearStr.isEmpty()){ showError("Year of manufacture is required."); yearField.requestFocus();   return; }

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
    private void setStatus(String message) {
        statusLabel.setText(" " + message);
    }

    /** Shows a validation error dialog. */
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

        @Override
        public int getRowCount()    { return data.size(); }

        @Override
        public int getColumnCount() { return COLUMNS.length; }

        @Override
        public String getColumnName(int col) { return COLUMNS[col]; }

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

        @Override
        public boolean isCellEditable(int row, int col) { return false; }
    }
}
