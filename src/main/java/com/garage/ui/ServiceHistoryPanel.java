/**
 * ServiceHistoryPanel — Swing panel for vehicle service history lookup.
 * Searches by registration number, displays a vehicle summary header,
 * and shows a colour-coded history table with an export option.
 * All DB calls happen inside SwingWorker to avoid blocking the EDT.
 *
 * Course  : UIT3361 OOP Java / UIT3311 Database Technology Lab
 * Project : Vehicle Service Management System
 */
package com.garage.ui;

import com.garage.service.HistoryService;
import com.garage.service.HistoryService.ServiceHistoryRecord;
import com.garage.service.HistoryService.VehicleSummary;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Displays the complete service history for a vehicle identified by its
 * registration number. Includes:
 * <ul>
 *   <li>Search field + button</li>
 *   <li>Vehicle summary header (Make, Model, Year, Owner, Phone)</li>
 *   <li>History JTable with colour-coded Status column</li>
 *   <li>Export to text dialog</li>
 * </ul>
 *
 * <p>All database operations execute in {@link SwingWorker#doInBackground()}.
 */
public class ServiceHistoryPanel extends JPanel {

    // ── Service ───────────────────────────────────────────────────────────────
    private final HistoryService historyService = new HistoryService();

    // ── Formatters ────────────────────────────────────────────────────────────
    private static final DateTimeFormatter DT_FMT =
        DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm");
    private static final NumberFormat CURRENCY_FMT =
        NumberFormat.getNumberInstance(new Locale("en", "IN"));

    static {
        CURRENCY_FMT.setMinimumFractionDigits(2);
        CURRENCY_FMT.setMaximumFractionDigits(2);
    }

    // ── State ─────────────────────────────────────────────────────────────────
    private List<ServiceHistoryRecord> currentRecords = new ArrayList<>();

    // ── Search section ────────────────────────────────────────────────────────
    private JTextField regNoField;

    // ── Summary header ────────────────────────────────────────────────────────
    private JLabel vehicleDetailLabel;
    private JLabel ownerDetailLabel;
    private JLabel totalServicesLabel;

    // ── Table ─────────────────────────────────────────────────────────────────
    private HistoryTableModel tableModel;

    // ── Status ────────────────────────────────────────────────────────────────
    private JLabel statusLabel;

    // ── Constructor ───────────────────────────────────────────────────────────

    /**
     * Constructs the ServiceHistoryPanel and builds all UI sections.
     */
    public ServiceHistoryPanel() {
        setLayout(new BorderLayout(0, 6));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        add(buildSearchSection(),  BorderLayout.NORTH);
        add(buildHistoryTable(),   BorderLayout.CENTER);
        add(buildStatusBar(),      BorderLayout.SOUTH);
    }

    // ── UI Builders ───────────────────────────────────────────────────────────

    /**
     * Builds the search input section (registration field + summary header).
     *
     * @return the search + summary panel
     */
    private JPanel buildSearchSection() {
        JPanel outer = new JPanel(new BorderLayout(0, 4));

        // ── Row 1: search bar ──────────────────────────────────────────────
        JPanel searchRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        searchRow.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(),
            "Search Vehicle Service History",
            TitledBorder.LEFT, TitledBorder.TOP));

        searchRow.add(new JLabel("Registration No:"));
        regNoField = new JTextField(14);
        regNoField.setToolTipText("Enter the vehicle number plate (e.g., TN09AB1234)");
        searchRow.add(regNoField);

        JButton searchBtn = new JButton("Search History");
        searchBtn.setMnemonic('S');
        searchBtn.addActionListener(e -> onSearch());
        searchRow.add(searchBtn);

        JButton exportBtn = new JButton("Export to Text");
        exportBtn.setMnemonic('E');
        exportBtn.addActionListener(e -> onExport());
        searchRow.add(exportBtn);

        JButton clearBtn = new JButton("Clear");
        clearBtn.setMnemonic('C');
        clearBtn.addActionListener(e -> clearDisplay());
        searchRow.add(clearBtn);

        outer.add(searchRow, BorderLayout.NORTH);

        // Allow pressing Enter in the search field
        regNoField.addActionListener(e -> onSearch());

        // ── Row 2: vehicle summary header ──────────────────────────────────
        JPanel summaryPanel = new JPanel(new GridBagLayout());
        summaryPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(),
            "Vehicle & Owner Details",
            TitledBorder.LEFT, TitledBorder.TOP));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 8, 3, 8);
        gbc.anchor = GridBagConstraints.WEST;

        Font bold = summaryPanel.getFont().deriveFont(Font.BOLD);

        vehicleDetailLabel = new JLabel("—");
        vehicleDetailLabel.setFont(bold);
        ownerDetailLabel   = new JLabel("—");
        ownerDetailLabel.setFont(bold);
        totalServicesLabel = new JLabel("—");
        totalServicesLabel.setFont(bold);
        totalServicesLabel.setForeground(new Color(0, 90, 160));

        Object[][] rows = {
            {"Vehicle:",        vehicleDetailLabel},
            {"Owner:",          ownerDetailLabel},
            {"Total Visits:",   totalServicesLabel}
        };
        for (int i = 0; i < rows.length; i++) {
            gbc.gridx = 0; gbc.gridy = i; gbc.weightx = 0;
            summaryPanel.add(new JLabel((String) rows[i][0]), gbc);
            gbc.gridx = 1; gbc.weightx = 1.0;
            summaryPanel.add((JComponent) rows[i][1], gbc);
        }

        outer.add(summaryPanel, BorderLayout.CENTER);
        return outer;
    }

    /**
     * Builds the history table section.
     *
     * @return the table panel
     */
    private JPanel buildHistoryTable() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(),
            "Service History",
            TitledBorder.LEFT, TitledBorder.TOP));

        tableModel = new HistoryTableModel();
        JTable table = new JTable(tableModel);
        table.setRowHeight(24);
        table.setFillsViewportHeight(true);
        table.getTableHeader().setReorderingAllowed(false);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Colour-code the Status column (index 2)
        table.getColumnModel().getColumn(2).setCellRenderer(new StatusCellRenderer());

        // Right-align the Invoice Total column (index 6)
        DefaultTableCellRenderer rightAlign = new DefaultTableCellRenderer();
        rightAlign.setHorizontalAlignment(SwingConstants.RIGHT);
        table.getColumnModel().getColumn(6).setCellRenderer(rightAlign);

        // Column preferred widths
        int[] widths = {55, 130, 100, 150, 110, 70, 100, 80};
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }

        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    /** Builds the bottom status bar. */
    private JPanel buildStatusBar() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        statusLabel = new JLabel("  Enter a registration number and click \"Search History\".");
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.PLAIN, 11f));
        panel.add(statusLabel);
        return panel;
    }

    // ── Event Handlers ────────────────────────────────────────────────────────

    /**
     * Handles the Search button / Enter key.
     * Fetches history and vehicle summary in a SwingWorker.
     */
    private void onSearch() {
        String regNo = regNoField.getText().trim();
        if (regNo.isEmpty()) {
            showError("Please enter a registration number.");
            regNoField.requestFocus();
            return;
        }

        setStatus("Searching history for '" + regNo + "'...");
        clearSummaryLabels();
        tableModel.setData(new ArrayList<>());

        new SwingWorker<Object[], Void>() {
            @Override
            protected Object[] doInBackground() throws Exception {
                VehicleSummary summary = historyService.getVehicleSummary(regNo);
                List<ServiceHistoryRecord> records =
                    historyService.getServiceHistory(regNo);
                return new Object[]{ summary, records };
            }

            @Override
            protected void done() {
                try {
                    Object[] result = get();
                    VehicleSummary summary = (VehicleSummary) result[0];
                    @SuppressWarnings("unchecked")
                    List<ServiceHistoryRecord> records =
                        (List<ServiceHistoryRecord>) result[1];

                    currentRecords = records;

                    if (summary == null && records.isEmpty()) {
                        clearSummaryLabels();
                        vehicleDetailLabel.setText(
                            "✘  No vehicle found for: " + regNo);
                        vehicleDetailLabel.setForeground(Color.RED);
                        setStatus("No records found for '" + regNo + "'.");
                        return;
                    }

                    // Populate summary header
                    if (summary != null) {
                        vehicleDetailLabel.setText(
                            summary.make + " " + summary.model +
                            "  (" + summary.yearOfMfr + ")  —  " + summary.fuelType);
                        vehicleDetailLabel.setForeground(new Color(0, 100, 0));
                        ownerDetailLabel.setText(
                            summary.customerName + "  (Ph: " + summary.phone + ")");
                        ownerDetailLabel.setForeground(new Color(0, 80, 150));
                    }
                    totalServicesLabel.setText(records.size() + " service visit(s) on record");

                    tableModel.setData(records);
                    setStatus(records.size() + " record(s) found for '" + regNo + "'.");

                } catch (Exception ex) {
                    showError("Search failed.\n" + extractMessage(ex));
                    setStatus("Search failed.");
                }
            }
        }.execute();
    }

    /**
     * Exports the current history to a formatted text block shown in a dialog.
     */
    private void onExport() {
        if (currentRecords.isEmpty()) {
            showError("No history records to export. Run a search first.");
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("========================================\n");
        sb.append("  Vehicle Service History Report\n");
        sb.append("  Registration: ").append(regNoField.getText().trim()).append("\n");
        sb.append("========================================\n\n");

        for (ServiceHistoryRecord r : currentRecords) {
            sb.append(String.format("JC #%-4d | %s\n",
                r.jobCardId,
                r.appointmentDt != null ? r.appointmentDt.format(DT_FMT) : "—"));
            sb.append(String.format("  Service    : %s\n", r.serviceName));
            sb.append(String.format("  Technician : %s\n",
                r.technicianName != null ? r.technicianName : "—"));
            sb.append(String.format("  Status     : %s\n", r.status));
            sb.append(String.format("  Labour Hrs : %.2f\n", r.labourHours));
            if (r.grandTotal != null) {
                sb.append(String.format("  Invoice    : ₹%s  [%s]\n",
                    CURRENCY_FMT.format(r.grandTotal),
                    r.paymentStatus != null ? r.paymentStatus : "—"));
            } else {
                sb.append("  Invoice    : Not yet generated\n");
            }
            sb.append("----------------------------------------\n");
        }

        JTextArea textArea = new JTextArea(sb.toString(), 20, 60);
        textArea.setEditable(false);
        textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

        JOptionPane.showMessageDialog(
            this,
            new JScrollPane(textArea),
            "Exported History — " + regNoField.getText().trim(),
            JOptionPane.INFORMATION_MESSAGE
        );
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Clears all display fields back to the "no data" state. */
    private void clearDisplay() {
        regNoField.setText("");
        clearSummaryLabels();
        tableModel.setData(new ArrayList<>());
        currentRecords = new ArrayList<>();
        setStatus("  Enter a registration number and click \"Search History\".");
        regNoField.requestFocus();
    }

    /** Resets the summary label values to their placeholder state. */
    private void clearSummaryLabels() {
        vehicleDetailLabel.setText("—");
        vehicleDetailLabel.setForeground(Color.DARK_GRAY);
        ownerDetailLabel.setText("—");
        ownerDetailLabel.setForeground(Color.DARK_GRAY);
        totalServicesLabel.setText("—");
    }

    /** Updates the status bar. */
    private void setStatus(String msg) { statusLabel.setText("  " + msg); }

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

    // ── Inner: History Table Model ────────────────────────────────────────────

    /**
     * AbstractTableModel backing the service history JTable.
     * Columns: JC #, Date, Status, Service, Technician, Labour Hrs, Invoice Total, Payment.
     */
    private static class HistoryTableModel extends AbstractTableModel {

        private static final String[] COLUMNS = {
            "JC #", "Appointment", "Status", "Service",
            "Technician", "Labour Hrs", "Invoice Total", "Payment"
        };

        private List<ServiceHistoryRecord> data = new ArrayList<>();

        /**
         * Replaces current data and fires a full repaint notification.
         *
         * @param records the new list of history records
         */
        public void setData(List<ServiceHistoryRecord> records) {
            this.data = (records != null) ? records : new ArrayList<>();
            fireTableDataChanged();
        }

        @Override public int getRowCount()    { return data.size(); }
        @Override public int getColumnCount() { return COLUMNS.length; }
        @Override public String getColumnName(int col) { return COLUMNS[col]; }
        @Override public boolean isCellEditable(int r, int c) { return false; }

        @Override
        public Class<?> getColumnClass(int col) {
            return (col == 0) ? Integer.class : String.class;
        }

        @Override
        public Object getValueAt(int row, int col) {
            ServiceHistoryRecord r = data.get(row);
            return switch (col) {
                case 0 -> r.jobCardId;
                case 1 -> r.appointmentDt != null
                    ? r.appointmentDt.format(DT_FMT) : "—";
                case 2 -> r.status;
                case 3 -> r.serviceName;
                case 4 -> r.technicianName != null ? r.technicianName : "—";
                case 5 -> r.labourHours > 0
                    ? String.format("%.2f", r.labourHours) : "—";
                case 6 -> r.grandTotal != null
                    ? "₹ " + CURRENCY_FMT.format(r.grandTotal) : "Not invoiced";
                case 7 -> r.paymentStatus != null ? r.paymentStatus : "—";
                default -> "";
            };
        }
    }

    // ── Inner: Status Colour Renderer ─────────────────────────────────────────

    /**
     * Colour-codes the Status column in the history table.
     *
     * <ul>
     *   <li>BOOKED      → blue</li>
     *   <li>IN_PROGRESS → orange</li>
     *   <li>COMPLETED   → purple</li>
     *   <li>DELIVERED   → dark green</li>
     * </ul>
     */
    private static class StatusCellRenderer extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {

            super.getTableCellRendererComponent(
                table, value, isSelected, hasFocus, row, column);

            setHorizontalAlignment(SwingConstants.CENTER);
            setFont(getFont().deriveFont(Font.BOLD));

            if (!isSelected) {
                String status = (value != null) ? value.toString() : "";
                setForeground(switch (status) {
                    case "BOOKED"      -> new Color(0, 80, 180);
                    case "IN_PROGRESS" -> new Color(200, 100, 0);
                    case "COMPLETED"   -> new Color(100, 0, 160);
                    case "DELIVERED"   -> new Color(0, 130, 0);
                    default            -> table.getForeground();
                });
            } else {
                setForeground(table.getSelectionForeground());
            }
            return this;
        }
    }
}
