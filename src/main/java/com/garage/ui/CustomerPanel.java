/**
 * CustomerPanel — Swing panel for customer registration and search.
 * Uses GridBagLayout for the form and AbstractTableModel for the customer list.
 * All DB operations are delegated to CustomerService via SwingWorker.
 *
 * Course  : UIT3361 OOP Java / UIT3311 Database Technology Lab
 * Project : Vehicle Service Management System
 */
package com.garage.ui;

import com.garage.model.Customer;
import com.garage.service.CustomerService;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Panel providing:
 * <ul>
 *   <li>A registration form (Name, Phone, Email, Address) with a Register button.</li>
 *   <li>A search field (by phone) with a Search button that highlights matching rows.</li>
 *   <li>A JTable showing all customers, refreshed after every registration.</li>
 * </ul>
 *
 * <p>All database interactions happen inside {@link SwingWorker#doInBackground()}
 * to avoid blocking the Event Dispatch Thread.
 */
public class CustomerPanel extends JPanel {

    // ── Service ───────────────────────────────────────────────────────────────
    private final CustomerService customerService = new CustomerService();

    // ── Form fields ───────────────────────────────────────────────────────────
    private JTextField nameField;
    private JTextField phoneField;
    private JTextField emailField;
    private JTextField addressField;

    // ── Search ────────────────────────────────────────────────────────────────
    private JTextField searchPhoneField;

    // ── Table ─────────────────────────────────────────────────────────────────
    private CustomerTableModel tableModel;
    private JTable             customerTable;

    // ── Status ────────────────────────────────────────────────────────────────
    private JLabel statusLabel;

    // ── Constructor ───────────────────────────────────────────────────────────

    /**
     * Constructs the CustomerPanel, builds the UI, and loads initial data.
     */
    public CustomerPanel() {
        setLayout(new BorderLayout(0, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        add(buildFormSection(),   BorderLayout.NORTH);
        add(buildTableSection(),  BorderLayout.CENTER);
        add(buildStatusBar(),     BorderLayout.SOUTH);

        loadAllCustomers();
    }

    // ── UI Builders ───────────────────────────────────────────────────────────

    /**
     * Builds the top section containing the registration form and the search bar.
     *
     * @return a JPanel containing both subsections
     */
    private JPanel buildFormSection() {
        JPanel wrapper = new JPanel(new BorderLayout(0, 6));
        wrapper.add(buildRegistrationForm(), BorderLayout.CENTER);
        wrapper.add(buildSearchBar(),        BorderLayout.SOUTH);
        return wrapper;
    }

    /**
     * Builds the customer registration form using GridBagLayout.
     *
     * @return the registration form panel
     */
    private JPanel buildRegistrationForm() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(), "Register New Customer",
            TitledBorder.LEFT, TitledBorder.TOP));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets  = new Insets(4, 6, 4, 6);
        gbc.anchor  = GridBagConstraints.WEST;
        gbc.fill    = GridBagConstraints.HORIZONTAL;

        // ── Row 0: Name ───────────────────────────────────────────────────
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        panel.add(new JLabel("Full Name *:"), gbc);

        nameField = new JTextField(20);
        nameField.setToolTipText("Enter the customer's full name");
        gbc.gridx = 1; gbc.weightx = 1.0;
        panel.add(nameField, gbc);

        // ── Row 0: Phone (col 2-3) ────────────────────────────────────────
        gbc.gridx = 2; gbc.weightx = 0;
        panel.add(new JLabel("Phone *:"), gbc);

        phoneField = new JTextField(12);
        phoneField.setToolTipText("Enter a 10-15 digit mobile number");
        gbc.gridx = 3; gbc.weightx = 0.5;
        panel.add(phoneField, gbc);

        // ── Row 1: Email ──────────────────────────────────────────────────
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        panel.add(new JLabel("Email:"), gbc);

        emailField = new JTextField(20);
        emailField.setToolTipText("Optional — email address");
        gbc.gridx = 1; gbc.weightx = 1.0;
        panel.add(emailField, gbc);

        // ── Row 1: Address (spans 2 cols) ─────────────────────────────────
        gbc.gridx = 2; gbc.weightx = 0;
        panel.add(new JLabel("Address:"), gbc);

        addressField = new JTextField(24);
        addressField.setToolTipText("Optional — street / city address");
        gbc.gridx = 3; gbc.weightx = 0.5;
        panel.add(addressField, gbc);

        // ── Row 2: Button row ─────────────────────────────────────────────
        gbc.gridx = 0; gbc.gridy = 2;
        gbc.gridwidth = 4;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.weightx = 0;

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        JButton registerBtn = new JButton("Register Customer");
        JButton clearBtn    = new JButton("Clear");

        registerBtn.setMnemonic('R');
        clearBtn.setMnemonic('C');

        registerBtn.addActionListener(e -> onRegisterCustomer());
        clearBtn.addActionListener(e -> clearForm());

        btnPanel.add(clearBtn);
        btnPanel.add(registerBtn);
        panel.add(btnPanel, gbc);

        return panel;
    }

    /**
     * Builds the phone search bar below the registration form.
     *
     * @return the search panel
     */
    private JPanel buildSearchBar() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(), "Search Customer",
            TitledBorder.LEFT, TitledBorder.TOP));

        panel.add(new JLabel("Search by Phone:"));

        searchPhoneField = new JTextField(15);
        searchPhoneField.setToolTipText("Enter phone number to search");
        panel.add(searchPhoneField);

        JButton searchBtn = new JButton("Search");
        searchBtn.setMnemonic('S');
        searchBtn.addActionListener(e -> onSearchByPhone());
        panel.add(searchBtn);

        JButton showAllBtn = new JButton("Show All");
        showAllBtn.addActionListener(e -> {
            searchPhoneField.setText("");
            loadAllCustomers();
        });
        panel.add(showAllBtn);

        return panel;
    }

    /**
     * Builds the customer list table section.
     *
     * @return the table panel
     */
    private JPanel buildTableSection() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(), "Customer List",
            TitledBorder.LEFT, TitledBorder.TOP));

        tableModel    = new CustomerTableModel();
        customerTable = new JTable(tableModel);
        customerTable.setRowHeight(22);
        customerTable.setFillsViewportHeight(true);
        customerTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        customerTable.getTableHeader().setReorderingAllowed(false);

        // Column widths
        customerTable.getColumnModel().getColumn(0).setPreferredWidth(50);
        customerTable.getColumnModel().getColumn(1).setPreferredWidth(180);
        customerTable.getColumnModel().getColumn(2).setPreferredWidth(120);
        customerTable.getColumnModel().getColumn(3).setPreferredWidth(170);
        customerTable.getColumnModel().getColumn(4).setPreferredWidth(250);

        panel.add(new JScrollPane(customerTable), BorderLayout.CENTER);
        return panel;
    }

    /**
     * Builds the bottom status bar label.
     *
     * @return the status label wrapped in a panel
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
     * Handles the "Register Customer" button click.
     * Validates input, then inserts via CustomerService in a SwingWorker.
     */
    private void onRegisterCustomer() {
        String name    = nameField.getText().trim();
        String phone   = phoneField.getText().trim();
        String email   = emailField.getText().trim();
        String address = addressField.getText().trim();

        // Inline UI-level validation
        if (name.isEmpty()) {
            showError("Customer name is required.");
            nameField.requestFocus();
            return;
        }
        if (phone.isEmpty()) {
            showError("Phone number is required.");
            phoneField.requestFocus();
            return;
        }

        Customer customer = new Customer(
            name,
            phone,
            email.isEmpty()   ? null : email,
            address.isEmpty() ? null : address
        );

        setStatus("Registering customer…");

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                customerService.registerCustomer(customer);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();  // re-throws any exception from doInBackground
                    setStatus("Customer '" + customer.getCustomerName() +
                              "' registered successfully (ID " + customer.getCustomerId() + ").");
                    clearForm();
                    loadAllCustomers();
                } catch (Exception ex) {
                    String msg = extractMessage(ex);
                    showError("Failed to register customer.\n" + msg);
                    setStatus("Registration failed.");
                }
            }
        }.execute();
    }

    /**
     * Handles the "Search" button click for phone-based lookup.
     * Highlights the matching row in the table, or shows a not-found message.
     */
    private void onSearchByPhone() {
        String phone = searchPhoneField.getText().trim();
        if (phone.isEmpty()) {
            showError("Please enter a phone number to search.");
            return;
        }

        setStatus("Searching…");

        new SwingWorker<Integer, Void>() {
            private List<Customer> results;

            @Override
            protected Integer doInBackground() throws Exception {
                results = customerService.getAllCustomers();
                // find the row index matching the phone
                for (int i = 0; i < results.size(); i++) {
                    if (results.get(i).getPhone().equals(phone)) {
                        return i;
                    }
                }
                return -1;
            }

            @Override
            protected void done() {
                try {
                    int rowIndex = get();
                    tableModel.setData(results);

                    if (rowIndex >= 0) {
                        customerTable.setRowSelectionInterval(rowIndex, rowIndex);
                        customerTable.scrollRectToVisible(
                            customerTable.getCellRect(rowIndex, 0, true));
                        setStatus("Customer found: " + results.get(rowIndex).getCustomerName());
                    } else {
                        setStatus("No customer found with phone: " + phone);
                        JOptionPane.showMessageDialog(
                            CustomerPanel.this,
                            "No customer found with phone number: " + phone,
                            "Not Found",
                            JOptionPane.INFORMATION_MESSAGE
                        );
                    }
                } catch (Exception ex) {
                    showError("Search failed.\n" + extractMessage(ex));
                    setStatus("Search failed.");
                }
            }
        }.execute();
    }

    // ── Data Loading ──────────────────────────────────────────────────────────

    /**
     * Loads all customers from the database and refreshes the table.
     * Runs the DB call in a SwingWorker to avoid blocking the EDT.
     */
    private void loadAllCustomers() {
        setStatus("Loading customers…");

        new SwingWorker<List<Customer>, Void>() {
            @Override
            protected List<Customer> doInBackground() throws Exception {
                return customerService.getAllCustomers();
            }

            @Override
            protected void done() {
                try {
                    List<Customer> data = get();
                    tableModel.setData(data);
                    setStatus(data.size() + " customer(s) loaded.");
                } catch (Exception ex) {
                    showError("Could not load customers.\n" + extractMessage(ex));
                    setStatus("Load failed.");
                }
            }
        }.execute();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Clears all registration form input fields. */
    private void clearForm() {
        nameField.setText("");
        phoneField.setText("");
        emailField.setText("");
        addressField.setText("");
        nameField.requestFocus();
    }

    /** Updates the status bar label text. */
    private void setStatus(String message) {
        statusLabel.setText(" " + message);
    }

    /** Shows a validation-error dialog. */
    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Validation Error",
                                      JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Extracts a readable message from a (possibly wrapped) exception.
     *
     * @param ex the exception to extract from
     * @return the cause message if available, otherwise the exception message
     */
    private String extractMessage(Exception ex) {
        Throwable cause = ex.getCause();
        return (cause != null) ? cause.getMessage() : ex.getMessage();
    }

    // ── Inner Table Model ─────────────────────────────────────────────────────

    /**
     * AbstractTableModel implementation backing the customer JTable.
     * Keeps the data as a List of Customer objects.
     */
    private static class CustomerTableModel extends AbstractTableModel {

        private static final String[] COLUMNS =
            {"ID", "Name", "Phone", "Email", "Address"};

        private List<Customer> data = new ArrayList<>();

        /**
         * Replaces the current data and notifies the table to repaint.
         *
         * @param customers the new list of customers
         */
        public void setData(List<Customer> customers) {
            this.data = (customers != null) ? customers : new ArrayList<>();
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
            Customer c = data.get(row);
            return switch (col) {
                case 0 -> c.getCustomerId();
                case 1 -> c.getCustomerName();
                case 2 -> c.getPhone();
                case 3 -> c.getEmail() != null ? c.getEmail() : "";
                case 4 -> c.getAddress() != null ? c.getAddress() : "";
                default -> "";
            };
        }

        @Override
        public Class<?> getColumnClass(int col) {
            return (col == 0) ? Integer.class : String.class;
        }

        @Override
        public boolean isCellEditable(int row, int col) { return false; }

        /**
         * Returns the Customer at the given row index.
         *
         * @param row table row index
         * @return the Customer object at that row
         */
        public Customer getCustomerAt(int row) {
            return data.get(row);
        }
    }
}
