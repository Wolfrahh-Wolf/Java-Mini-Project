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
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
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
 *   <li>History JTable with Fluent colour-coded Status column</li>
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
        setLayout(new BorderLayout(0, 0));
        setBackground(FluentTheme.CANVAS);

        add(buildTopSection(),   BorderLayout.NORTH);
        add(buildHistoryTable(), BorderLayout.CENTER);
        add(buildStatusBar(),    BorderLayout.SOUTH);
    }

    // ── UI Builders ───────────────────────────────────────────────────────────

    /**
     * Builds the top section: search card + vehicle summary card stacked vertically.
     *
     * @return the assembled top panel
     */
    private JPanel buildTopSection() {
        JPanel wrapper = new JPanel(new BorderLayout(0, 0));
        wrapper.setBackground(FluentTheme.CANVAS);
        wrapper.add(buildSearchCard(),  BorderLayout.NORTH);
        wrapper.add(buildSummaryCard(), BorderLayout.CENTER);
        return wrapper;
    }

    /**
     * Builds the search input card.
     *
     * @return the search card
     */
    private JPanel buildSearchCard() {
        JPanel card = new JPanel(new BorderLayout(0, 0));
        card.setBackground(FluentTheme.SURFACE);
        card.setBorder(new MatteBorder(0, 0, 1, 0, FluentTheme.BORDER));

        card.add(FluentTheme.sectionHeader("Search Vehicle Service History"),
                 BorderLayout.NORTH);

        JPanel inner = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 10));
        inner.setBackground(FluentTheme.SURFACE);
        inner.setBorder(new EmptyBorder(0, FluentTheme.PADDING, 4, FluentTheme.PADDING));

        JLabel regLbl = new JLabel("Registration No:");
        regLbl.setFont(FluentTheme.FONT_BODY);
        regLbl.setForeground(FluentTheme.TEXT_MUTED);
        inner.add(regLbl);

        regNoField = new JTextField(16);
        FluentTheme.styleTextField(regNoField);
        regNoField.setToolTipText("Enter the vehicle number plate (e.g., TN09AB1234)");
        regNoField.addActionListener(e -> onSearch());
        inner.add(regNoField);

        JButton searchBtn = FluentTheme.accentButton("Search History");
        searchBtn.setMnemonic('S');
        searchBtn.addActionListener(e -> onSearch());
        inner.add(searchBtn);

        JButton exportBtn = FluentTheme.secondaryButton("Export to Text");
        exportBtn.setMnemonic('E');
        exportBtn.addActionListener(e -> onExport());
        inner.add(exportBtn);

        JButton clearBtn = FluentTheme.ghostButton("Clear");
        clearBtn.setMnemonic('C');
        clearBtn.addActionListener(e -> clearDisplay());
        inner.add(clearBtn);

        card.add(inner, BorderLayout.CENTER);
        return card;
    }

    /**
     * Builds the vehicle and owner summary card.
     *
     * @return the summary card
     */
    private JPanel buildSummaryCard() {
        JPanel card = new JPanel(new BorderLayout(0, 0));
        card.setBackground(FluentTheme.SURFACE_ALT);
        card.setBorder(new MatteBorder(0, 0, 1, 0, FluentTheme.BORDER));

        card.add(FluentTheme.sectionHeader("Vehicle & Owner Details"), BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridBagLayout());
        grid.setBackground(FluentTheme.SURFACE_ALT);
        grid.setBorder(new EmptyBorder(FluentTheme.PADDING_SM, FluentTheme.PADDING,
                                       FluentTheme.PADDING_SM, FluentTheme.PADDING));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 8, 4, 16);
        gbc.anchor = GridBagConstraints.WEST;

        vehicleDetailLabel = makeValueLabel("-");
        vehicleDetailLabel.setFont(FluentTheme.FONT_SEMIBOLD);
        ownerDetailLabel   = makeValueLabel("-");
        totalServicesLabel = makeValueLabel("-");
        totalServicesLabel.setForeground(FluentTheme.ACCENT);
        totalServicesLabel.setFont(FluentTheme.FONT_SEMIBOLD);

        Object[][] rows = {
            {"Vehicle:",       vehicleDetailLabel},
            {"Owner:",         ownerDetailLabel},
            {"Total Visits:",  totalServicesLabel}
        };

        for (int i = 0; i < rows.length; i++) {
            gbc.gridx = 0; gbc.gridy = i; gbc.weightx = 0;
            JLabel capLbl = new JLabel((String) rows[i][0]);
            capLbl.setFont(FluentTheme.FONT_BODY);
            capLbl.setForeground(FluentTheme.TEXT_MUTED);
            grid.add(capLbl, gbc);
            gbc.gridx = 1; gbc.weightx = 1.0;
            grid.add((JComponent) rows[i][1], gbc);
        }

        card.add(grid, BorderLayout.CENTER);
        return card;
    }

    /**
     * Builds the history table card.
     *
     * @return the history table panel
     */
    private JPanel buildHistoryTable() {
        JPanel card = new JPanel(new BorderLayout(0, 0));
        card.setBackground(FluentTheme.CANVAS);
        card.add(FluentTheme.sectionHeader("Service History"), BorderLayout.NORTH);

        tableModel = new HistoryTableModel();
        JTable table = new JTable(tableModel);
        FluentTheme.styleTable(table);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Colour-code the Status column (index 2)
        table.getColumnModel().getColumn(2).setCellRenderer(new StatusCellRenderer());

        // Padded renderer for other columns
        DefaultTableCellRenderer paddedRenderer = new DefaultTableCellRenderer();
        paddedRenderer.setBorder(new EmptyBorder(0, 12, 0, 12));
        for (int i = 0; i < tableModel.getColumnCount(); i++) {
            if (i != 2) table.getColumnModel().getColumn(i).setCellRenderer(paddedRenderer);
        }

        // Right-align Invoice Total column (index 6)
        DefaultTableCellRenderer rightAlign = new DefaultTableCellRenderer();
        rightAlign.setHorizontalAlignment(SwingConstants.RIGHT);
        rightAlign.setBorder(new EmptyBorder(0, 4, 0, 12));
        table.getColumnModel().getColumn(6).setCellRenderer(rightAlign);

        int[] widths = {55, 140, 110, 160, 120, 80, 110, 90};
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBackground(FluentTheme.SURFACE);
        scroll.getViewport().setBackground(FluentTheme.SURFACE);
        scroll.setBorder(BorderFactory.createLineBorder(FluentTheme.BORDER, 1));

        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    /** Builds the bottom status bar. */
    private JPanel buildStatusBar() {
        statusLabel = new JLabel("  Enter a registration number and click \"Search History\".");
        return FluentTheme.statusBar(statusLabel);
    }

    // ── Event Handlers ────────────────────────────────────────────────────────

    /**
     * Handles the Search button / Enter key.
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
                List<ServiceHistoryRecord> records = historyService.getServiceHistory(regNo);
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
                        vehicleDetailLabel.setText("✘  No vehicle found for: " + regNo);
                        vehicleDetailLabel.setForeground(FluentTheme.STATUS_ERROR);
                        setStatus("No records found for '" + regNo + "'.");
                        return;
                    }

                    if (summary != null) {
                        vehicleDetailLabel.setText(
                            summary.make + " " + summary.model +
                            "  (" + summary.yearOfMfr + ")  -  " + summary.fuelType);
                        vehicleDetailLabel.setForeground(FluentTheme.STATUS_SUCCESS);
                        ownerDetailLabel.setText(
                            summary.customerName + "  (Ph: " + summary.phone + ")");
                        ownerDetailLabel.setForeground(FluentTheme.STATUS_INFO);
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
                r.appointmentDt != null ? r.appointmentDt.format(DT_FMT) : "-"));
            sb.append(String.format("  Service    : %s\n", r.serviceName));
            sb.append(String.format("  Technician : %s\n",
                r.technicianName != null ? r.technicianName : "-"));
            sb.append(String.format("  Status     : %s\n", r.status));
            sb.append(String.format("  Labour Hrs : %.2f\n", r.labourHours));
            if (r.grandTotal != null) {
                sb.append(String.format("  Invoice    : ₹%s  [%s]\n",
                    CURRENCY_FMT.format(r.grandTotal),
                    r.paymentStatus != null ? r.paymentStatus : "-"));
            } else {
                sb.append("  Invoice    : Not yet generated\n");
            }
            sb.append("----------------------------------------\n");
        }

        JTextArea textArea = new JTextArea(sb.toString(), 20, 60);
        textArea.setEditable(false);
        textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        textArea.setBackground(FluentTheme.SURFACE);
        textArea.setForeground(FluentTheme.TEXT_PRIMARY);

        JOptionPane.showMessageDialog(
            this,
            new JScrollPane(textArea),
            "Exported History - " + regNoField.getText().trim(),
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

    /** Resets the summary label values to placeholder state. */
    private void clearSummaryLabels() {
        vehicleDetailLabel.setText("-");
        vehicleDetailLabel.setForeground(FluentTheme.TEXT_MUTED);
        ownerDetailLabel.setText("-");
        ownerDetailLabel.setForeground(FluentTheme.TEXT_MUTED);
        totalServicesLabel.setText("-");
        totalServicesLabel.setForeground(FluentTheme.TEXT_MUTED);
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

    /** Creates a styled primary-color value JLabel. */
    private JLabel makeValueLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(FluentTheme.FONT_BODY);
        lbl.setForeground(FluentTheme.TEXT_PRIMARY);
        return lbl;
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
                case 1 -> r.appointmentDt != null ? r.appointmentDt.format(DT_FMT) : "-";
                case 2 -> r.status;
                case 3 -> r.serviceName;
                case 4 -> r.technicianName != null ? r.technicianName : "-";
                case 5 -> r.labourHours > 0 ? String.format("%.2f", r.labourHours) : "-";
                case 6 -> r.grandTotal != null
                    ? "₹ " + CURRENCY_FMT.format(r.grandTotal) : "Not invoiced";
                case 7 -> r.paymentStatus != null ? r.paymentStatus : "-";
                default -> "";
            };
        }
    }

    // ── Inner: Status Colour Renderer ─────────────────────────────────────────

    /**
     * Fluent-styled colour-coded renderer for the Status column.
     *
     * <ul>
     *   <li>BOOKED      → Fluent Blue</li>
     *   <li>IN_PROGRESS → Fluent Amber</li>
     *   <li>COMPLETED   → Success Green</li>
     *   <li>DELIVERED   → Dark Green</li>
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
            setFont(FluentTheme.FONT_SEMIBOLD);
            setBorder(new EmptyBorder(0, 8, 0, 8));

            if (!isSelected) {
                String status = (value != null) ? value.toString() : "";
                setForeground(switch (status) {
                    case "BOOKED"      -> FluentTheme.STATUS_INFO;
                    case "IN_PROGRESS" -> FluentTheme.STATUS_WARNING;
                    case "COMPLETED"   -> FluentTheme.STATUS_SUCCESS;
                    case "DELIVERED"   -> new Color(0x2ECC71);
                    default            -> FluentTheme.TEXT_MUTED;
                });
                setBackground(FluentTheme.SURFACE);
            } else {
                setForeground(table.getSelectionForeground());
                setBackground(FluentTheme.SELECTION_BG);
            }
            return this;
        }
    }
}
