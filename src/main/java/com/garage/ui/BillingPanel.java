/**
 * BillingPanel — Swing panel for generating and managing invoices.
 * Supports job card lookup, invoice generation, adding spare parts,
 * viewing itemised totals with GST, and marking invoices as paid.
 * All DB calls happen inside SwingWorker to avoid blocking the EDT.
 *
 * Course  : UIT3361 OOP Java / UIT3311 Database Technology Lab
 * Project : Vehicle Service Management System
 */
package com.garage.ui;

import com.garage.dao.CustomerDAO;
import com.garage.dao.VehicleDAO;
import com.garage.model.Customer;
import com.garage.model.Invoice;
import com.garage.model.InvoiceItem;
import com.garage.model.Vehicle;
import com.garage.service.BillingService;
import com.garage.util.DBConnection;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Provides the full billing workflow:
 * <ol>
 *   <li>Look up an existing invoice by Job Card ID, or generate a new one.</li>
 *   <li>Display invoice header (Invoice #, date, vehicle, customer).</li>
 *   <li>Show itemised line items (labour + parts) in a JTable.</li>
 *   <li>Add spare parts via input dialog.</li>
 *   <li>Show computed summary: Labour Total, Parts Total, GST, Grand Total.</li>
 *   <li>Mark invoice as PAID with a chosen payment mode.</li>
 * </ol>
 *
 * <p>All database operations execute in {@link SwingWorker#doInBackground()}.
 */
public class BillingPanel extends JPanel {

    // ── Services / DAOs ───────────────────────────────────────────────────────
    private final BillingService billingService = new BillingService();

    // ── Formatters ────────────────────────────────────────────────────────────
    private static final NumberFormat CURRENCY_FMT =
        NumberFormat.getNumberInstance(new Locale("en", "IN"));
    private static final DateTimeFormatter DT_FMT =
        DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm");

    static {
        CURRENCY_FMT.setMinimumFractionDigits(2);
        CURRENCY_FMT.setMaximumFractionDigits(2);
    }

    // ── State ─────────────────────────────────────────────────────────────────
    /** Invoice currently loaded; null if none. */
    private Invoice currentInvoice = null;

    // ── Search section ────────────────────────────────────────────────────────
    private JTextField jobCardIdField;

    // ── Header section ────────────────────────────────────────────────────────
    private JLabel invoiceIdLabel;
    private JLabel invoiceDateLabel;
    private JLabel vehicleLabel;
    private JLabel customerLabel;
    private JLabel paymentStatusBadge;

    // ── Line items table ──────────────────────────────────────────────────────
    private InvoiceItemTableModel itemTableModel;

    // ── Summary section ───────────────────────────────────────────────────────
    private JLabel labourTotalLabel;
    private JLabel partsTotalLabel;
    private JLabel taxAmountLabel;
    private JLabel grandTotalLabel;

    // ── Action buttons ────────────────────────────────────────────────────────
    private JButton generateBtn;
    private JButton addPartBtn;
    private JButton markPaidBtn;
    private JComboBox<String> paymentModeCombo;

    // ── Status ────────────────────────────────────────────────────────────────
    private JLabel statusLabel;

    // ── Constructor ───────────────────────────────────────────────────────────

    /**
     * Constructs the BillingPanel and assembles all sub-sections.
     */
    public BillingPanel() {
        setLayout(new BorderLayout(0, 6));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        add(buildSearchSection(),  BorderLayout.NORTH);
        add(buildMainSection(),    BorderLayout.CENTER);
        add(buildStatusBar(),      BorderLayout.SOUTH);

        updateButtonStates(false);
    }

    // ── UI Builders ───────────────────────────────────────────────────────────

    /**
     * Builds the job card lookup section at the top.
     *
     * @return the search panel
     */
    private JPanel buildSearchSection() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(), "Load Invoice by Job Card ID",
            TitledBorder.LEFT, TitledBorder.TOP));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 6, 4, 6);
        gbc.anchor = GridBagConstraints.WEST;

        // Row 0
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Job Card ID:"), gbc);

        jobCardIdField = new JTextField(8);
        jobCardIdField.setToolTipText("Enter the Job Card number (e.g., 1)");
        gbc.gridx = 1;
        panel.add(jobCardIdField, gbc);

        JButton loadBtn = new JButton("Load / Find Invoice");
        loadBtn.setMnemonic('L');
        loadBtn.addActionListener(e -> onLoadInvoice());
        gbc.gridx = 2;
        panel.add(loadBtn, gbc);

        generateBtn = new JButton("Generate New Invoice");
        generateBtn.setMnemonic('G');
        generateBtn.addActionListener(e -> onGenerateInvoice());
        generateBtn.setEnabled(false);
        gbc.gridx = 3;
        panel.add(generateBtn, gbc);

        return panel;
    }

    /**
     * Builds the main area: invoice header left, line items centre, summary right.
     *
     * @return the main content panel
     */
    private JComponent buildMainSection() {
        JPanel outer = new JPanel(new BorderLayout(6, 0));

        outer.add(buildHeaderSection(), BorderLayout.NORTH);
        outer.add(buildItemsSection(),  BorderLayout.CENTER);
        outer.add(buildSummaryPanel(),  BorderLayout.EAST);

        return outer;
    }

    /**
     * Builds the invoice header (Invoice #, date, vehicle, customer, payment status).
     *
     * @return the header panel
     */
    private JPanel buildHeaderSection() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(), "Invoice Details",
            TitledBorder.LEFT, TitledBorder.TOP));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 8, 3, 8);
        gbc.anchor = GridBagConstraints.WEST;

        Font bold = panel.getFont().deriveFont(Font.BOLD);

        invoiceIdLabel   = new JLabel("—"); invoiceIdLabel.setFont(bold);
        invoiceDateLabel = new JLabel("—");
        vehicleLabel     = new JLabel("—");
        customerLabel    = new JLabel("—");
        paymentStatusBadge = new JLabel("—");
        paymentStatusBadge.setFont(bold);
        paymentStatusBadge.setOpaque(true);

        Object[][] rows = {
            {"Invoice #:", invoiceIdLabel},
            {"Date:",      invoiceDateLabel},
            {"Vehicle:",   vehicleLabel},
            {"Customer:",  customerLabel},
            {"Status:",    paymentStatusBadge}
        };

        for (int i = 0; i < rows.length; i++) {
            gbc.gridx = 0; gbc.gridy = i; gbc.weightx = 0;
            panel.add(new JLabel((String) rows[i][0]), gbc);
            gbc.gridx = 1; gbc.weightx = 1.0;
            panel.add((JComponent) rows[i][1], gbc);
        }

        return panel;
    }

    /**
     * Builds the line items section (JTable + Add Part button).
     *
     * @return the items panel
     */
    private JPanel buildItemsSection() {
        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(), "Invoice Line Items",
            TitledBorder.LEFT, TitledBorder.TOP));

        itemTableModel = new InvoiceItemTableModel();
        JTable itemTable = new JTable(itemTableModel);
        itemTable.setRowHeight(22);
        itemTable.setFillsViewportHeight(true);
        itemTable.getTableHeader().setReorderingAllowed(false);
        itemTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Column widths
        int[] widths = {60, 55, 250, 60, 90, 90};
        for (int i = 0; i < widths.length; i++) {
            itemTable.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }

        panel.add(new JScrollPane(itemTable), BorderLayout.CENTER);

        addPartBtn = new JButton("➕ Add Spare Part");
        addPartBtn.setMnemonic('P');
        addPartBtn.addActionListener(e -> onAddPart());
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        btnRow.add(addPartBtn);
        panel.add(btnRow, BorderLayout.SOUTH);

        return panel;
    }

    /**
     * Builds the summary panel (totals + payment controls).
     *
     * @return the summary panel
     */
    private JPanel buildSummaryPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setPreferredSize(new Dimension(220, 0));
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(), "Summary",
            TitledBorder.LEFT, TitledBorder.TOP));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 8, 4, 8);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill   = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        Font bold   = panel.getFont().deriveFont(Font.BOLD);
        Font bigger = panel.getFont().deriveFont(Font.BOLD, 14f);

        labourTotalLabel = new JLabel("₹ —");
        partsTotalLabel  = new JLabel("₹ —");
        taxAmountLabel   = new JLabel("₹ —");
        grandTotalLabel  = new JLabel("₹ —");
        grandTotalLabel.setFont(bigger);
        grandTotalLabel.setForeground(new Color(0, 100, 0));

        int row = 0;
        addSummaryRow(panel, gbc, row++, "Labour Total:",   labourTotalLabel, null);
        addSummaryRow(panel, gbc, row++, "Parts Total:",    partsTotalLabel,  null);
        addSummaryRow(panel, gbc, row++, "GST (18%):",      taxAmountLabel,   null);
        addSeparator(panel, gbc, row++);
        addSummaryRow(panel, gbc, row++, "GRAND TOTAL:", grandTotalLabel, bigger);

        // Payment controls
        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2;
        gbc.insets = new Insets(16, 8, 2, 8);
        panel.add(new JSeparator(), gbc);

        gbc.insets = new Insets(4, 8, 4, 8);
        gbc.gridy = row++;
        JLabel modeLabel = new JLabel("Payment Mode:");
        modeLabel.setFont(bold);
        panel.add(modeLabel, gbc);

        paymentModeCombo = new JComboBox<>(
            new String[]{"CASH", "CARD", "UPI", "ONLINE", "CHEQUE"});
        gbc.gridy = row++;
        panel.add(paymentModeCombo, gbc);

        markPaidBtn = new JButton("✔ Mark as PAID");
        markPaidBtn.setFont(bold);
        markPaidBtn.addActionListener(e -> onMarkAsPaid());
        gbc.gridy = row++;
        panel.add(markPaidBtn, gbc);

        // Spacer at bottom
        gbc.gridy = row;
        gbc.weighty = 1.0;
        panel.add(new JLabel(), gbc);

        return panel;
    }

    /**
     * Adds a label-value row to the summary grid.
     *
     * @param panel  target panel
     * @param gbc    shared constraints
     * @param row    row index
     * @param label  left-side label text
     * @param value  right-side value component
     * @param font   font override for the value label (may be null)
     */
    private void addSummaryRow(JPanel panel, GridBagConstraints gbc,
                               int row, String label, JLabel value, Font font) {
        gbc.gridwidth = 1;
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        gbc.insets = new Insets(3, 8, 3, 4);
        panel.add(new JLabel(label), gbc);

        if (font != null) value.setFont(font);
        gbc.gridx = 1; gbc.weightx = 1.0;
        gbc.insets = new Insets(3, 4, 3, 8);
        panel.add(value, gbc);
    }

    /**
     * Adds a full-width horizontal separator row.
     *
     * @param panel target panel
     * @param gbc   shared constraints
     * @param row   row index
     */
    private void addSeparator(JPanel panel, GridBagConstraints gbc, int row) {
        gbc.gridx = 0; gbc.gridy = row;
        gbc.gridwidth = 2; gbc.weightx = 1.0;
        gbc.insets = new Insets(2, 4, 2, 4);
        panel.add(new JSeparator(), gbc);
    }

    /** Builds the status bar panel. */
    private JPanel buildStatusBar() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        statusLabel = new JLabel(" ");
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.PLAIN, 11f));
        panel.add(statusLabel);
        return panel;
    }

    // ── Event Handlers ────────────────────────────────────────────────────────

    /**
     * Handles "Load / Find Invoice". Tries to find an existing invoice
     * for the entered job card ID. If none found, enables "Generate" button.
     */
    private void onLoadInvoice() {
        String input = jobCardIdField.getText().trim();
        if (input.isEmpty()) {
            showError("Please enter a Job Card ID.");
            jobCardIdField.requestFocus();
            return;
        }
        int jobCardId;
        try {
            jobCardId = Integer.parseInt(input);
        } catch (NumberFormatException ex) {
            showError("Invalid Job Card ID: '" + input + "'\nPlease enter a numeric value.");
            return;
        }

        setStatus("Looking up invoice for Job Card #" + jobCardId + "...");
        clearDisplay();
        updateButtonStates(false);

        new SwingWorker<Object[], Void>() {
            @Override
            protected Object[] doInBackground() throws Exception {
                Optional<Invoice> inv = billingService.findByJobCardId(jobCardId);
                if (inv.isPresent()) {
                    List<InvoiceItem> items =
                        billingService.getItemsForInvoice(inv.get().getInvoiceId());
                    String[] vehicleCustomer = resolveVehicleAndCustomer(jobCardId);
                    return new Object[]{ inv.get(), items, vehicleCustomer };
                }
                return null;  // no existing invoice
            }

            @Override
            protected void done() {
                try {
                    Object[] result = get();
                    if (result == null) {
                        setStatus("No invoice found for Job Card #" + jobCardId +
                                  ". Click \"Generate New Invoice\" to create one.");
                        generateBtn.setEnabled(true);
                        currentInvoice = null;
                    } else {
                        currentInvoice  = (Invoice)       result[0];
                        @SuppressWarnings("unchecked")
                        List<InvoiceItem> items = (List<InvoiceItem>) result[1];
                        String[] vc     = (String[])      result[2];
                        populateDisplay(currentInvoice, items, vc[0], vc[1]);
                        updateButtonStates(true);
                        setStatus("Invoice #" + currentInvoice.getInvoiceId() +
                                  " loaded for Job Card #" + jobCardId + ".");
                    }
                } catch (Exception ex) {
                    showError("Failed to load invoice.\n" + extractMessage(ex));
                    setStatus("Load failed.");
                }
            }
        }.execute();
    }

    /**
     * Handles "Generate New Invoice". Creates a new invoice for the job card ID.
     */
    private void onGenerateInvoice() {
        String input = jobCardIdField.getText().trim();
        int jobCardId;
        try {
            jobCardId = Integer.parseInt(input);
        } catch (NumberFormatException ex) {
            showError("Invalid Job Card ID: '" + input + "'");
            return;
        }

        setStatus("Generating invoice for Job Card #" + jobCardId + "...");
        generateBtn.setEnabled(false);

        new SwingWorker<Object[], Void>() {
            @Override
            protected Object[] doInBackground() throws Exception {
                Invoice inv = billingService.generateInvoice(jobCardId);
                List<InvoiceItem> items =
                    billingService.getItemsForInvoice(inv.getInvoiceId());
                String[] vc = resolveVehicleAndCustomer(jobCardId);
                return new Object[]{ inv, items, vc };
            }

            @Override
            protected void done() {
                try {
                    Object[] result = get();
                    currentInvoice  = (Invoice)       result[0];
                    @SuppressWarnings("unchecked")
                    List<InvoiceItem> items = (List<InvoiceItem>) result[1];
                    String[] vc     = (String[])      result[2];
                    populateDisplay(currentInvoice, items, vc[0], vc[1]);
                    updateButtonStates(true);
                    setStatus("Invoice #" + currentInvoice.getInvoiceId() +
                              " generated successfully.");
                } catch (Exception ex) {
                    showError("Invoice generation failed.\n" + extractMessage(ex));
                    generateBtn.setEnabled(true);
                    setStatus("Generation failed.");
                }
            }
        }.execute();
    }

    /**
     * Handles "Add Spare Part". Prompts for description, qty, unit price
     * and adds a PART line item to the current invoice.
     */
    private void onAddPart() {
        if (currentInvoice == null) return;

        JTextField descField  = new JTextField(20);
        JTextField qtyField   = new JTextField("1", 5);
        JTextField priceField = new JTextField(8);

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4, 4, 4, 4);
        g.anchor = GridBagConstraints.WEST;

        g.gridx = 0; g.gridy = 0; form.add(new JLabel("Description *:"), g);
        g.gridx = 1; g.fill = GridBagConstraints.HORIZONTAL; g.weightx = 1;
        form.add(descField, g);

        g.fill = GridBagConstraints.NONE; g.weightx = 0;
        g.gridx = 0; g.gridy = 1; form.add(new JLabel("Quantity *:"), g);
        g.gridx = 1; form.add(qtyField, g);

        g.gridx = 0; g.gridy = 2; form.add(new JLabel("Unit Price (₹) *:"), g);
        g.gridx = 1; form.add(priceField, g);

        int choice = JOptionPane.showConfirmDialog(
            this, form, "Add Spare Part",
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (choice != JOptionPane.OK_OPTION) return;

        String desc  = descField.getText().trim();
        String qtyStr   = qtyField.getText().trim();
        String priceStr = priceField.getText().trim();

        if (desc.isEmpty() || qtyStr.isEmpty() || priceStr.isEmpty()) {
            showError("All three fields are required.");
            return;
        }

        double qty, price;
        try {
            qty   = Double.parseDouble(qtyStr);
            price = Double.parseDouble(priceStr);
        } catch (NumberFormatException ex) {
            showError("Quantity and Unit Price must be numeric values.");
            return;
        }

        final int invoiceId  = currentInvoice.getInvoiceId();
        final String finalDesc = desc;

        setStatus("Adding part: " + desc + "...");

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                billingService.addPart(invoiceId, finalDesc, qty, price);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    // Reload invoice + items to reflect new totals
                    reloadCurrentInvoice();
                } catch (Exception ex) {
                    showError("Failed to add part.\n" + extractMessage(ex));
                    setStatus("Add part failed.");
                }
            }
        }.execute();
    }

    /**
     * Handles "Mark as PAID". Confirms and persists payment with the selected mode.
     */
    private void onMarkAsPaid() {
        if (currentInvoice == null) return;

        if ("PAID".equals(currentInvoice.getPaymentStatus())) {
            showError("This invoice is already marked as PAID.");
            return;
        }

        String mode = (String) paymentModeCombo.getSelectedItem();
        int confirm = JOptionPane.showConfirmDialog(
            this,
            "Mark Invoice #" + currentInvoice.getInvoiceId() +
            " as PAID via " + mode + "?\n" +
            "Grand Total: ₹" + fmt(currentInvoice.getGrandTotal()),
            "Confirm Payment",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE
        );
        if (confirm != JOptionPane.YES_OPTION) return;

        setStatus("Marking invoice #" + currentInvoice.getInvoiceId() + " as PAID...");

        final int invoiceId = currentInvoice.getInvoiceId();
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                billingService.markAsPaid(invoiceId, mode);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    reloadCurrentInvoice();
                    setStatus("Invoice #" + invoiceId + " marked as PAID via " + mode + ".");
                } catch (Exception ex) {
                    showError("Failed to mark as paid.\n" + extractMessage(ex));
                    setStatus("Mark paid failed.");
                }
            }
        }.execute();
    }

    // ── Data Loading ──────────────────────────────────────────────────────────

    /**
     * Reloads the current invoice (after a part is added or payment is made).
     * Uses the stored invoiceId to re-fetch from DB.
     */
    private void reloadCurrentInvoice() {
        if (currentInvoice == null) return;
        final int invoiceId = currentInvoice.getInvoiceId();
        final int jobCardId = currentInvoice.getJobCardId();

        new SwingWorker<Object[], Void>() {
            @Override
            protected Object[] doInBackground() throws Exception {
                Optional<Invoice> inv = billingService.findById(invoiceId);
                if (inv.isEmpty()) return null;
                List<InvoiceItem> items = billingService.getItemsForInvoice(invoiceId);
                String[] vc = resolveVehicleAndCustomer(jobCardId);
                return new Object[]{ inv.get(), items, vc };
            }

            @Override
            protected void done() {
                try {
                    Object[] result = get();
                    if (result != null) {
                        currentInvoice = (Invoice) result[0];
                        @SuppressWarnings("unchecked")
                        List<InvoiceItem> items = (List<InvoiceItem>) result[1];
                        String[] vc = (String[]) result[2];
                        populateDisplay(currentInvoice, items, vc[0], vc[1]);
                    }
                } catch (Exception ex) {
                    showError("Could not reload invoice.\n" + extractMessage(ex));
                }
            }
        }.execute();
    }

    // ── Display Helpers ───────────────────────────────────────────────────────

    /**
     * Populates the invoice header, line-items table, and summary section.
     *
     * @param inv          the loaded invoice
     * @param items        the line items
     * @param vehicleInfo  formatted vehicle string
     * @param customerInfo formatted customer string
     */
    private void populateDisplay(Invoice inv, List<InvoiceItem> items,
                                  String vehicleInfo, String customerInfo) {
        invoiceIdLabel.setText("#" + inv.getInvoiceId());
        invoiceDateLabel.setText(inv.getInvoiceDate() != null
            ? inv.getInvoiceDate().format(DT_FMT) : "—");
        vehicleLabel.setText(vehicleInfo);
        customerLabel.setText(customerInfo);

        // Payment status badge with colour coding
        String status = inv.getPaymentStatus();
        paymentStatusBadge.setText("  " + status + "  ");
        paymentStatusBadge.setBackground("PAID".equals(status)
            ? new Color(198, 239, 206)   // green
            : "WAIVED".equals(status)
              ? new Color(255, 235, 156)  // yellow
              : new Color(255, 199, 206)  // red for PENDING
        );
        paymentStatusBadge.setForeground("PAID".equals(status)
            ? new Color(0, 97, 0)
            : "WAIVED".equals(status)
              ? new Color(127, 100, 0)
              : new Color(156, 0, 6)
        );

        itemTableModel.setData(items);

        labourTotalLabel.setText("₹ " + fmt(inv.getLabourTotal()));
        partsTotalLabel.setText("₹ "  + fmt(inv.getPartsTotal()));
        taxAmountLabel.setText("₹ "   + fmt(inv.getTaxAmount()));
        grandTotalLabel.setText("₹ "  + fmt(inv.getGrandTotal()));

        // Disable mark-paid if already settled
        markPaidBtn.setEnabled(!"PAID".equals(status) && !"WAIVED".equals(status));
        generateBtn.setEnabled(false);
    }

    /** Resets all display fields to placeholder state. */
    private void clearDisplay() {
        invoiceIdLabel.setText("—");
        invoiceDateLabel.setText("—");
        vehicleLabel.setText("—");
        customerLabel.setText("—");
        paymentStatusBadge.setText("—");
        paymentStatusBadge.setBackground(null);
        paymentStatusBadge.setOpaque(false);
        itemTableModel.setData(new ArrayList<>());
        labourTotalLabel.setText("₹ —");
        partsTotalLabel.setText("₹ —");
        taxAmountLabel.setText("₹ —");
        grandTotalLabel.setText("₹ —");
        currentInvoice = null;
    }

    /**
     * Enables or disables action buttons based on whether an invoice is loaded.
     *
     * @param invoiceLoaded true if a current invoice is loaded
     */
    private void updateButtonStates(boolean invoiceLoaded) {
        addPartBtn.setEnabled(invoiceLoaded);
        markPaidBtn.setEnabled(invoiceLoaded);
        paymentModeCombo.setEnabled(invoiceLoaded);
        if (!invoiceLoaded) generateBtn.setEnabled(false);
    }

    /**
     * Fetches the vehicle registration and customer name for header display.
     * Runs inside {@code doInBackground()} so it is safe to call JDBC here.
     *
     * @param jobCardId the job card whose vehicle/customer we need
     * @return String[2]: [0] = vehicle info, [1] = customer info
     * @throws SQLException if any DB lookup fails
     */
    private String[] resolveVehicleAndCustomer(int jobCardId) throws SQLException {
        Connection conn = DBConnection.getConnection();

        // We need to find the vehicle via JOB_CARDS → VEHICLE_ID
        // Use JobCardDAO indirectly via the Connection
        String vehicleInfo   = "—";
        String customerInfo  = "—";

        try {
            com.garage.dao.JobCardDAO jcDao = new com.garage.dao.JobCardDAO();
            com.garage.model.JobCard jc = jcDao.findById(conn, jobCardId).orElse(null);
            if (jc != null) {
                VehicleDAO  vDao = new VehicleDAO();
                CustomerDAO cDao = new CustomerDAO();
                Vehicle v = vDao.findById(conn, jc.getVehicleId()).orElse(null);
                if (v != null) {
                    vehicleInfo = v.getRegistrationNo() + " — " +
                                  v.getMake() + " " + v.getModel() +
                                  " (" + v.getYearOfMfr() + ")";
                    Customer c = cDao.findById(conn, v.getCustomerId()).orElse(null);
                    if (c != null) {
                        customerInfo = c.getCustomerName() + "  (Ph: " + c.getPhone() + ")";
                    }
                }
            }
        } catch (SQLException ignored) {
            // Non-critical — display "—" if lookup fails
        }

        return new String[]{ vehicleInfo, customerInfo };
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Formats a double as a 2-decimal currency string.
     *
     * @param amount the amount to format
     * @return formatted string (e.g., "1,200.00")
     */
    private String fmt(double amount) {
        return CURRENCY_FMT.format(amount);
    }

    /** Updates the status bar. */
    private void setStatus(String msg) { statusLabel.setText(" " + msg); }

    /** Shows a validation/error dialog per AGENTS.md §4.5. */
    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Validation Error",
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

    // ── Inner: Invoice Items Table Model ──────────────────────────────────────

    /**
     * AbstractTableModel backing the invoice line items JTable.
     * Columns: #, Type, Description, Qty, Unit Price, Line Total.
     */
    private class InvoiceItemTableModel extends AbstractTableModel {

        private static final String[] COLUMNS =
            {"#", "Type", "Description", "Qty", "Unit Price", "Line Total"};

        private List<InvoiceItem> data = new ArrayList<>();

        /**
         * Replaces current data and fires a full repaint notification.
         *
         * @param items the new list of invoice items
         */
        public void setData(List<InvoiceItem> items) {
            this.data = (items != null) ? items : new ArrayList<>();
            fireTableDataChanged();
        }

        @Override public int getRowCount()    { return data.size(); }
        @Override public int getColumnCount() { return COLUMNS.length; }
        @Override public String getColumnName(int col) { return COLUMNS[col]; }
        @Override public boolean isCellEditable(int r, int c) { return false; }

        @Override
        public Class<?> getColumnClass(int col) {
            return switch (col) {
                case 0     -> Integer.class;
                case 3, 4, 5 -> Double.class;
                default    -> String.class;
            };
        }

        @Override
        public Object getValueAt(int row, int col) {
            InvoiceItem item = data.get(row);
            return switch (col) {
                case 0 -> item.getItemId();
                case 1 -> item.getItemType();
                case 2 -> item.getDescription();
                case 3 -> item.getQuantity();
                case 4 -> "₹ " + fmt(item.getUnitPrice());
                case 5 -> "₹ " + fmt(item.getLineTotal());
                default -> "";
            };
        }
    }
}
