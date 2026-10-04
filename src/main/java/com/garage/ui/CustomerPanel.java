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
        setLayout(new BorderLayout(0, 0));
        setBackground(FluentTheme.CANVAS);

        add(buildTopSection(),    BorderLayout.NORTH);
        add(buildTableSection(),  BorderLayout.CENTER);
        add(buildStatusBar(),     BorderLayout.SOUTH);

        loadAllCustomers();
    }

    // ── UI Builders ───────────────────────────────────────────────────────────

    /**
     * Builds the top section: registration form card + search bar card.
     *
     * @return the assembled top panel
     */
    private JPanel buildTopSection() {
        JPanel wrapper = new JPanel(new BorderLayout(0, 0));
        wrapper.setBackground(FluentTheme.CANVAS);
        wrapper.add(buildRegistrationCard(), BorderLayout.CENTER);
        wrapper.add(buildSearchCard(),       BorderLayout.SOUTH);
        return wrapper;
    }

    /**
     * Builds the customer registration form inside a Fluent card.
     *
     * @return the registration form panel
     */
    private JPanel buildRegistrationCard() {
        JPanel card = new JPanel(new BorderLayout(0, 0));
        card.setBackground(FluentTheme.SURFACE);
        card.setBorder(new MatteBorder(0, 0, 1, 0, FluentTheme.BORDER));

        card.add(FluentTheme.sectionHeader("Register New Customer"), BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(FluentTheme.SURFACE);
        form.setBorder(new EmptyBorder(FluentTheme.PADDING, FluentTheme.PADDING,
                                       FluentTheme.PADDING, FluentTheme.PADDING));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets  = new Insets(6, 8, 6, 8);
        gbc.anchor  = GridBagConstraints.WEST;
        gbc.fill    = GridBagConstraints.HORIZONTAL;

        // ── Row 0: Name / Phone ───────────────────────────────────────────────
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        form.add(makeLabel("Full Name *"), gbc);

        nameField = makeTextField("Customer's full name");
        gbc.gridx = 1; gbc.weightx = 1.0;
        form.add(nameField, gbc);

        gbc.gridx = 2; gbc.weightx = 0;
        form.add(makeLabel("Phone *"), gbc);

        phoneField = makeTextField("10-15 digit mobile number");
        gbc.gridx = 3; gbc.weightx = 0.6;
        form.add(phoneField, gbc);

        // ── Row 1: Email / Address ────────────────────────────────────────────
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        form.add(makeLabel("Email"), gbc);

        emailField = makeTextField("Optional - email address");
        gbc.gridx = 1; gbc.weightx = 1.0;
        form.add(emailField, gbc);

        gbc.gridx = 2; gbc.weightx = 0;
        form.add(makeLabel("Address"), gbc);

        addressField = makeTextField("Optional - street / city");
        gbc.gridx = 3; gbc.weightx = 0.6;
        form.add(addressField, gbc);

        // ── Row 2: Buttons ────────────────────────────────────────────────────
        gbc.gridx = 0; gbc.gridy = 2;
        gbc.gridwidth = 4;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.weightx = 0;

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnRow.setBackground(FluentTheme.SURFACE);

        JButton clearBtn = FluentTheme.secondaryButton("Clear");
        clearBtn.setMnemonic('C');
        clearBtn.addActionListener(e -> clearForm());
        btnRow.add(clearBtn);

        JButton registerBtn = FluentTheme.accentButton("Register Customer");
        registerBtn.setMnemonic('R');
        registerBtn.addActionListener(e -> onRegisterCustomer());
        btnRow.add(registerBtn);

        form.add(btnRow, gbc);
        card.add(form, BorderLayout.CENTER);
        return card;
    }

    /**
     * Builds the search bar card below the registration form.
     *
     * @return the search card panel
     */
    private JPanel buildSearchCard() {
        JPanel card = new JPanel(new BorderLayout(0, 0));
        card.setBackground(FluentTheme.SURFACE_ALT);
        card.setBorder(new MatteBorder(0, 0, 1, 0, FluentTheme.BORDER));

        JPanel inner = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 10));
        inner.setBackground(FluentTheme.SURFACE_ALT);
        inner.setBorder(new EmptyBorder(0, FluentTheme.PADDING, 0, FluentTheme.PADDING));

        JLabel searchLbl = makeLabel("Search by Phone:");
        inner.add(searchLbl);

        searchPhoneField = makeTextField("Enter phone number to search");
        searchPhoneField.setPreferredSize(new Dimension(180, 32));
        inner.add(searchPhoneField);

        JButton searchBtn = FluentTheme.secondaryButton("Search");
        searchBtn.setMnemonic('S');
        searchBtn.addActionListener(e -> onSearchByPhone());
        inner.add(searchBtn);

        JButton showAllBtn = FluentTheme.ghostButton("Show All");
        showAllBtn.addActionListener(e -> {
            searchPhoneField.setText("");
            loadAllCustomers();
        });
        inner.add(showAllBtn);

        card.add(inner, BorderLayout.CENTER);
        return card;
    }

    /**
     * Builds the customer list table section.
     *
     * @return the table panel
     */
    private JPanel buildTableSection() {
        JPanel panel = new JPanel(new BorderLayout(0, 0));
        panel.setBackground(FluentTheme.CANVAS);
        panel.setBorder(new EmptyBorder(0, 0, 0, 0));

        panel.add(FluentTheme.sectionHeader("Customer List"), BorderLayout.NORTH);

        tableModel    = new CustomerTableModel();
        customerTable = new JTable(tableModel);
        FluentTheme.styleTable(customerTable);
        customerTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Padded cell renderer for all columns
        DefaultTableCellRenderer paddedRenderer = new DefaultTableCellRenderer();
        paddedRenderer.setBorder(new EmptyBorder(0, 12, 0, 12));
        for (int i = 0; i < tableModel.getColumnCount(); i++) {
            customerTable.getColumnModel().getColumn(i).setCellRenderer(paddedRenderer);
        }

        // Column widths
        customerTable.getColumnModel().getColumn(0).setPreferredWidth(55);
        customerTable.getColumnModel().getColumn(1).setPreferredWidth(200);
        customerTable.getColumnModel().getColumn(2).setPreferredWidth(130);
        customerTable.getColumnModel().getColumn(3).setPreferredWidth(200);
        customerTable.getColumnModel().getColumn(4).setPreferredWidth(280);

        JScrollPane scroll = new JScrollPane(customerTable);
        scroll.setBackground(FluentTheme.SURFACE);
        scroll.getViewport().setBackground(FluentTheme.SURFACE);
        scroll.setBorder(BorderFactory.createLineBorder(FluentTheme.BORDER, 1));

        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    /**
     * Builds the bottom status bar label.
     *
     * @return the status bar panel
     */
    private JPanel buildStatusBar() {
        statusLabel = new JLabel(" ");
        return FluentTheme.statusBar(statusLabel);
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
                    get();
                    setStatus("Customer '" + customer.getCustomerName() +
                              "' registered successfully (ID " + customer.getCustomerId() + ").");
                    clearForm();
                    loadAllCustomers();
                } catch (Exception ex) {
                    showError("Failed to register customer.\n" + extractMessage(ex));
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
                for (int i = 0; i < results.size(); i++) {
                    if (results.get(i).getPhone().equals(phone)) return i;
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
    private void setStatus(String message) { statusLabel.setText(" " + message); }

    /** Shows a validation-error dialog per AGENTS.md §4.5. */
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
     * AbstractTableModel implementation backing the customer JTable.
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

        @Override public int getRowCount()    { return data.size(); }
        @Override public int getColumnCount() { return COLUMNS.length; }
        @Override public String getColumnName(int col) { return COLUMNS[col]; }
        @Override public boolean isCellEditable(int row, int col) { return false; }

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

        /**
         * Returns the Customer at the given row index.
         *
         * @param row table row index
         * @return the Customer object at that row
         */
        public Customer getCustomerAt(int row) { return data.get(row); }
    }
}