/**
 * JobCardPanel — Swing dashboard for job card status tracking.
 * Shows all job cards with status filter buttons, supports advancing the
 * lifecycle, assigning technicians, and recording labour hours.
 * All DB calls happen inside SwingWorker to avoid blocking the EDT.
 *
 * Course  : UIT3361 OOP Java / UIT3311 Database Technology Lab
 * Project : Vehicle Service Management System
 */
package com.garage.ui;

import com.garage.dao.CustomerDAO;
import com.garage.dao.ServiceDAO;
import com.garage.dao.VehicleDAO;
import com.garage.model.Customer;
import com.garage.model.JobCard;
import com.garage.model.Service;
import com.garage.model.Vehicle;
import com.garage.service.JobCardService;
import com.garage.util.DBConnection;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Operational dashboard for garage staff providing:
 * <ul>
 *   <li>Status filter row (ALL / BOOKED / IN_PROGRESS / COMPLETED / DELIVERED).</li>
 *   <li>JTable backed by a flat {@link JobCardDisplayRow} DTO joining all entity info.</li>
 *   <li>Detail panel showing full remarks for the selected row.</li>
 *   <li>Action buttons: Advance Status, Assign Technician, Record Hours, Refresh.</li>
 * </ul>
 *
 * <p>All database operations execute in {@link SwingWorker#doInBackground()}.
 */
public class JobCardPanel extends JPanel {

    // ── Services / DAOs ───────────────────────────────────────────────────────
    private final JobCardService jobCardService = new JobCardService();

    // ── Filter state ──────────────────────────────────────────────────────────
    /** Current filter: null = ALL, otherwise one of the status strings. */
    private String currentFilter = null;

    // ── Table ─────────────────────────────────────────────────────────────────
    private JobCardTableModel tableModel;
    private JTable            jobCardTable;

    // ── Detail panel ──────────────────────────────────────────────────────────
    private JTextArea remarksArea;
    private JLabel    detailLabel;

    // ── Action buttons (enabled only when a row is selected) ──────────────────
    private JButton advanceBtn;
    private JButton technicianBtn;
    private JButton hoursBtn;

    // ── Status ────────────────────────────────────────────────────────────────
    private JLabel statusLabel;

    // ── Date formatter ────────────────────────────────────────────────────────
    private static final DateTimeFormatter DT_FMT =
        DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm");

    // ── Constructor ───────────────────────────────────────────────────────────

    /**
     * Constructs the JobCardPanel, builds the UI, and loads all job cards.
     */
    public JobCardPanel() {
        setLayout(new BorderLayout(0, 0));
        setBackground(FluentTheme.CANVAS);

        add(buildFilterBar(),     BorderLayout.NORTH);
        add(buildCentreSection(), BorderLayout.CENTER);
        add(buildStatusBar(),     BorderLayout.SOUTH);

        loadJobCards();
    }

    // ── UI Builders ───────────────────────────────────────────────────────────

    /**
     * Builds the Fluent-styled filter bar and action button row.
     *
     * @return the filter bar panel
     */
    private JPanel buildFilterBar() {
        JPanel outer = new JPanel(new BorderLayout(0, 0));
        outer.setBackground(FluentTheme.SURFACE);
        outer.setBorder(new MatteBorder(0, 0, 1, 0, FluentTheme.BORDER));

        // ── Filter radio buttons ───────────────────────────────────────────────
        JPanel filterRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 10));
        filterRow.setBackground(FluentTheme.SURFACE);
        filterRow.setBorder(new EmptyBorder(0, FluentTheme.PADDING, 0, 0));

        JLabel filterLbl = new JLabel("Filter:");
        filterLbl.setFont(FluentTheme.FONT_SEMIBOLD);
        filterLbl.setForeground(FluentTheme.TEXT_MUTED);
        filterRow.add(filterLbl);

        ButtonGroup group = new ButtonGroup();
        String[] filters = {"ALL", "BOOKED", "IN_PROGRESS", "COMPLETED", "DELIVERED"};

        for (String f : filters) {
            JToggleButton tb = buildFilterToggle(f.replace("_", " "));
            tb.setSelected("ALL".equals(f));
            tb.addActionListener(e -> {
                currentFilter = "ALL".equals(f) ? null : f;
                loadJobCards();
            });
            group.add(tb);
            filterRow.add(tb);
        }

        outer.add(filterRow, BorderLayout.WEST);

        // ── Action buttons ─────────────────────────────────────────────────────
        JPanel actionRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 8));
        actionRow.setBackground(FluentTheme.SURFACE);
        actionRow.setBorder(new EmptyBorder(0, 0, 0, FluentTheme.PADDING));

        JButton refreshBtn = FluentTheme.secondaryButton("⟳  Refresh");
        refreshBtn.setMnemonic('R');
        refreshBtn.addActionListener(e -> loadJobCards());
        actionRow.add(refreshBtn);

        advanceBtn = FluentTheme.accentButton("Advance Status  ▶");
        advanceBtn.setEnabled(false);
        advanceBtn.setMnemonic('A');
        advanceBtn.addActionListener(e -> onAdvanceStatus());
        actionRow.add(advanceBtn);

        technicianBtn = FluentTheme.secondaryButton("Assign Technician");
        technicianBtn.setEnabled(false);
        technicianBtn.setMnemonic('T');
        technicianBtn.addActionListener(e -> onAssignTechnician());
        actionRow.add(technicianBtn);

        hoursBtn = FluentTheme.secondaryButton("Record Hours");
        hoursBtn.setEnabled(false);
        hoursBtn.setMnemonic('H');
        hoursBtn.addActionListener(e -> onRecordHours());
        actionRow.add(hoursBtn);

        outer.add(actionRow, BorderLayout.EAST);
        return outer;
    }

    /**
     * Creates a styled Fluent filter toggle button.
     *
     * @param label the button text
     * @return the configured JToggleButton
     */
    private JToggleButton buildFilterToggle(String label) {
        JToggleButton tb = new JToggleButton(label) {
            @Override
            protected void paintComponent(Graphics g) {
                if (isSelected()) {
                    g.setColor(FluentTheme.ACCENT_MUTED);
                    g.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                }
                super.paintComponent(g);
            }
        };
        tb.setFont(FluentTheme.FONT_BODY);
        tb.setForeground(FluentTheme.TEXT_MUTED);
        tb.setBackground(FluentTheme.SURFACE);
        tb.setFocusPainted(false);
        tb.setBorderPainted(false);
        tb.setContentAreaFilled(false);
        tb.setOpaque(false);
        tb.setBorder(new EmptyBorder(5, 12, 5, 12));
        tb.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        tb.addChangeListener(e -> {
            if (tb.isSelected()) {
                tb.setForeground(FluentTheme.ACCENT);
                tb.setFont(FluentTheme.FONT_SEMIBOLD);
            } else {
                tb.setForeground(FluentTheme.TEXT_MUTED);
                tb.setFont(FluentTheme.FONT_BODY);
            }
        });
        return tb;
    }

    /**
     * Builds the centre section: job card table above, detail card below.
     *
     * @return a JSplitPane with table and detail areas
     */
    private JComponent buildCentreSection() {
        // ── Table card ────────────────────────────────────────────────────────
        JPanel tableCard = new JPanel(new BorderLayout(0, 0));
        tableCard.setBackground(FluentTheme.CANVAS);
        tableCard.add(FluentTheme.sectionHeader("Job Cards"), BorderLayout.NORTH);

        tableModel   = new JobCardTableModel();
        jobCardTable = new JTable(tableModel);
        FluentTheme.styleTable(jobCardTable);
        jobCardTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Padded renderer for all columns
        DefaultTableCellRenderer paddedRenderer = new DefaultTableCellRenderer();
        paddedRenderer.setBorder(new EmptyBorder(0, 12, 0, 12));
        for (int i = 0; i < tableModel.getColumnCount(); i++) {
            if (i != 6) jobCardTable.getColumnModel().getColumn(i).setCellRenderer(paddedRenderer);
        }

        // Custom renderer for Status column (index 6) with Fluent colours + badge
        jobCardTable.getColumnModel().getColumn(6).setCellRenderer(new StatusColumnRenderer());

        // Column widths
        int[] widths = {55, 110, 150, 160, 130, 95, 110, 120};
        for (int i = 0; i < widths.length; i++) {
            jobCardTable.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }

        // Selection listener
        jobCardTable.getSelectionModel().addListSelectionListener(
            (ListSelectionEvent e) -> {
                if (!e.getValueIsAdjusting()) {
                    boolean hasSelection = jobCardTable.getSelectedRow() >= 0;
                    advanceBtn.setEnabled(hasSelection);
                    technicianBtn.setEnabled(hasSelection);
                    hoursBtn.setEnabled(hasSelection);
                    if (hasSelection) showSelectedDetail();
                }
            }
        );

        JScrollPane scroll = new JScrollPane(jobCardTable);
        scroll.setBackground(FluentTheme.SURFACE);
        scroll.getViewport().setBackground(FluentTheme.SURFACE);
        scroll.setBorder(BorderFactory.createLineBorder(FluentTheme.BORDER, 1));
        tableCard.add(scroll, BorderLayout.CENTER);

        // ── Detail card ───────────────────────────────────────────────────────
        JPanel detailCard = new JPanel(new BorderLayout(0, 0));
        detailCard.setBackground(FluentTheme.SURFACE);
        detailCard.add(FluentTheme.sectionHeader("Selected Job Card — Details"),
                       BorderLayout.NORTH);

        JPanel detailInner = new JPanel(new BorderLayout(4, 4));
        detailInner.setBackground(FluentTheme.SURFACE);
        detailInner.setBorder(new EmptyBorder(FluentTheme.PADDING_SM, FluentTheme.PADDING,
                                               FluentTheme.PADDING_SM, FluentTheme.PADDING));

        detailLabel = new JLabel("  Select a row to view details.");
        detailLabel.setFont(FluentTheme.FONT_CAPTION);
        detailLabel.setForeground(FluentTheme.TEXT_MUTED);
        detailInner.add(detailLabel, BorderLayout.NORTH);

        remarksArea = new JTextArea(3, 40);
        remarksArea.setEditable(false);
        remarksArea.setLineWrap(true);
        remarksArea.setWrapStyleWord(true);
        FluentTheme.styleTextArea(remarksArea);
        remarksArea.setBackground(FluentTheme.INPUT_BG);

        JScrollPane remarksScroll = new JScrollPane(remarksArea);
        remarksScroll.setBorder(BorderFactory.createLineBorder(FluentTheme.BORDER, 1));
        remarksScroll.getViewport().setBackground(FluentTheme.INPUT_BG);
        detailInner.add(remarksScroll, BorderLayout.CENTER);

        detailCard.add(detailInner, BorderLayout.CENTER);

        // Split: table ~72%, detail ~28%
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tableCard, detailCard);
        split.setResizeWeight(0.72);
        split.setOneTouchExpandable(true);
        split.setBackground(FluentTheme.CANVAS);
        split.setBorder(null);
        split.setDividerSize(6);
        return split;
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
     * Handles "Advance Status" — advances the selected job card one step forward.
     */
    private void onAdvanceStatus() {
        JobCardDisplayRow row = getSelectedRow();
        if (row == null) return;

        int confirm = JOptionPane.showConfirmDialog(
            this,
            "Advance Job Card #" + row.jobCardId + " from  \"" + row.status + "\"?\n" +
            "This action cannot be undone.",
            "Confirm Status Advance",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE
        );
        if (confirm != JOptionPane.YES_OPTION) return;

        setStatus("Advancing status for Job Card #" + row.jobCardId + "...");

        new SwingWorker<JobCard, Void>() {
            @Override
            protected JobCard doInBackground() throws Exception {
                return jobCardService.advanceStatus(row.jobCardId);
            }

            @Override
            protected void done() {
                try {
                    JobCard updated = get();
                    setStatus("Job Card #" + updated.getJobCardId() +
                              " advanced to " + updated.getStatus() + ".");
                    loadJobCards();
                } catch (Exception ex) {
                    showError("Failed to advance status.\n" + extractMessage(ex));
                    setStatus("Status advance failed.");
                }
            }
        }.execute();
    }

    /**
     * Handles "Assign Technician" — prompts for a name and persists it.
     */
    private void onAssignTechnician() {
        JobCardDisplayRow row = getSelectedRow();
        if (row == null) return;

        String current = (row.technicianName != null) ? row.technicianName : "";
        String name = JOptionPane.showInputDialog(
            this,
            "Enter technician name for Job Card #" + row.jobCardId + ":",
            current
        );

        if (name == null || name.trim().isEmpty()) return;

        setStatus("Assigning technician...");

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                jobCardService.assignTechnician(row.jobCardId, name.trim());
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    setStatus("Technician '" + name.trim() +
                              "' assigned to Job Card #" + row.jobCardId + ".");
                    loadJobCards();
                } catch (Exception ex) {
                    showError("Failed to assign technician.\n" + extractMessage(ex));
                    setStatus("Assign failed.");
                }
            }
        }.execute();
    }

    /**
     * Handles "Record Hours" — prompts for labour hours with numeric validation.
     */
    private void onRecordHours() {
        JobCardDisplayRow row = getSelectedRow();
        if (row == null) return;

        String input = JOptionPane.showInputDialog(
            this,
            "Enter labour hours for Job Card #" + row.jobCardId + ":\n" +
            "(Current: " + row.labourHours + " hrs)",
            row.labourHours > 0 ? String.valueOf(row.labourHours) : ""
        );

        if (input == null || input.trim().isEmpty()) return;

        double hours;
        try {
            hours = Double.parseDouble(input.trim());
        } catch (NumberFormatException ex) {
            showError("Invalid input: '" + input + "'\nPlease enter a numeric value (e.g., 2.5).");
            return;
        }

        if (hours < 0) {
            showError("Labour hours must be zero or a positive number.");
            return;
        }

        setStatus("Recording labour hours...");
        final double finalHours = hours;

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                jobCardService.recordLabourHours(row.jobCardId, finalHours);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    setStatus("Recorded " + finalHours + " hrs for Job Card #" + row.jobCardId + ".");
                    loadJobCards();
                } catch (Exception ex) {
                    showError("Failed to record hours.\n" + extractMessage(ex));
                    setStatus("Record failed.");
                }
            }
        }.execute();
    }

    // ── Data Loading ──────────────────────────────────────────────────────────

    /**
     * Loads (or re-loads) job cards according to the current filter.
     * Fetches the full list from DB and enriches each row with vehicle,
     * customer, and service names. Runs entirely in a SwingWorker.
     */
    private void loadJobCards() {
        String filterLabel = (currentFilter != null) ? currentFilter : "ALL";
        setStatus("Loading job cards [filter: " + filterLabel + "]...");
        advanceBtn.setEnabled(false);
        technicianBtn.setEnabled(false);
        hoursBtn.setEnabled(false);

        new SwingWorker<List<JobCardDisplayRow>, Void>() {
            @Override
            protected List<JobCardDisplayRow> doInBackground() throws Exception {
                List<JobCard> cards = (currentFilter == null)
                    ? jobCardService.getAllJobCards()
                    : jobCardService.getByStatus(currentFilter);
                return enrichRows(cards);
            }

            @Override
            protected void done() {
                try {
                    List<JobCardDisplayRow> rows = get();
                    tableModel.setData(rows);
                    detailLabel.setText("  Select a row to view details.");
                    remarksArea.setText("");
                    setStatus(rows.size() + " job card(s) [filter: " + filterLabel + "].");
                } catch (Exception ex) {
                    showError("Could not load job cards.\n" + extractMessage(ex));
                    setStatus("Load failed.");
                }
            }
        }.execute();
    }

    /**
     * Enriches a list of raw {@link JobCard} objects into flat {@link JobCardDisplayRow}
     * DTOs by fetching associated vehicle, customer, and service names from the DB.
     * Uses in-memory caches to avoid redundant DB calls within one load batch.
     *
     * @param cards the list of raw job cards
     * @return list of display rows ready for the table model
     * @throws SQLException if any lookup fails
     */
    private List<JobCardDisplayRow> enrichRows(List<JobCard> cards) throws SQLException {
        Connection conn = DBConnection.getConnection();
        VehicleDAO  vehicleDao  = new VehicleDAO();
        CustomerDAO customerDao = new CustomerDAO();
        ServiceDAO  serviceDao  = new ServiceDAO();

        Map<Integer, Vehicle>  vehicleCache  = new ConcurrentHashMap<>();
        Map<Integer, Customer> customerCache = new ConcurrentHashMap<>();
        Map<Integer, Service>  serviceCache  = new ConcurrentHashMap<>();

        List<JobCardDisplayRow> rows = new ArrayList<>();

        for (JobCard jc : cards) {
            Vehicle v = vehicleCache.computeIfAbsent(jc.getVehicleId(), id -> {
                try { return vehicleDao.findById(conn, id).orElse(null); }
                catch (SQLException e) { return null; }
            });
            String regNo = (v != null) ? v.getRegistrationNo() : "ID:" + jc.getVehicleId();

            String customerName = "—";
            if (v != null) {
                Customer c = customerCache.computeIfAbsent(v.getCustomerId(), id -> {
                    try { return customerDao.findById(conn, id).orElse(null); }
                    catch (SQLException e) { return null; }
                });
                if (c != null) customerName = c.getCustomerName();
            }

            Service s = serviceCache.computeIfAbsent(jc.getServiceId(), id -> {
                try { return serviceDao.findById(conn, id).orElse(null); }
                catch (SQLException e) { return null; }
            });
            String serviceName = (s != null) ? s.getServiceName() : "ID:" + jc.getServiceId();

            rows.add(new JobCardDisplayRow(jc, regNo, customerName, serviceName));
        }

        return rows;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Returns the {@link JobCardDisplayRow} for the currently selected table row.
     *
     * @return the selected row or null
     */
    private JobCardDisplayRow getSelectedRow() {
        int selectedRow = jobCardTable.getSelectedRow();
        if (selectedRow < 0) return null;
        return tableModel.getRowAt(selectedRow);
    }

    /**
     * Populates the detail panel from the selected table row.
     */
    private void showSelectedDetail() {
        JobCardDisplayRow row = getSelectedRow();
        if (row == null) return;

        String apptStr  = (row.appointmentDt != null) ? row.appointmentDt.format(DT_FMT) : "—";
        String startStr = (row.startDt != null) ? row.startDt.format(DT_FMT) : "—";
        String doneStr  = (row.completionDt != null) ? row.completionDt.format(DT_FMT) : "—";

        detailLabel.setText(
            String.format("  Job Card #%d  |  %s  |  %s  |  Appt: %s  |  Start: %s  |  Done: %s",
                row.jobCardId, row.vehicleRegNo, row.status,
                apptStr, startStr, doneStr));

        remarksArea.setText(
            (row.remarks != null && !row.remarks.isBlank()) ? row.remarks : "(no remarks)");
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
     * @return the cause message if available, otherwise the exception's own message
     */
    private String extractMessage(Exception ex) {
        Throwable cause = ex.getCause();
        return (cause != null) ? cause.getMessage() : ex.getMessage();
    }

    // ── Inner: Flat DTO ───────────────────────────────────────────────────────

    /**
     * Flat display DTO joining JobCard + Vehicle + Customer + Service data.
     * Used only as table model backing objects — not persisted to DB.
     */
    static final class JobCardDisplayRow {

        final int    jobCardId;
        final String vehicleRegNo;
        final String customerName;
        final String serviceName;
        final String technicianName;
        final String status;
        final java.time.LocalDateTime appointmentDt;
        final java.time.LocalDateTime startDt;
        final java.time.LocalDateTime completionDt;
        final double labourHours;
        final String remarks;

        /**
         * Constructs a display row from a raw job card and enriched lookup data.
         *
         * @param jc           the raw JobCard
         * @param vehicleRegNo the vehicle registration number
         * @param customerName the owner's name
         * @param serviceName  the service catalogue name
         */
        JobCardDisplayRow(JobCard jc, String vehicleRegNo,
                          String customerName, String serviceName) {
            this.jobCardId      = jc.getJobCardId();
            this.vehicleRegNo   = vehicleRegNo;
            this.customerName   = customerName;
            this.serviceName    = serviceName;
            this.technicianName = jc.getTechnicianName();
            this.status         = jc.getStatus();
            this.appointmentDt  = jc.getAppointmentDt();
            this.startDt        = jc.getStartDt();
            this.completionDt   = jc.getCompletionDt();
            this.labourHours    = jc.getLabourHours();
            this.remarks        = jc.getRemarks();
        }
    }

    // ── Inner: Table Model ────────────────────────────────────────────────────

    /**
     * AbstractTableModel backing the job card JTable.
     * Columns: ID, Vehicle Reg, Customer, Service, Technician, Labour Hrs, Status, Appointment.
     */
    private static class JobCardTableModel extends AbstractTableModel {

        private static final String[] COLUMNS = {
            "JC #", "Vehicle Reg", "Customer", "Service",
            "Technician", "Labour Hrs", "Status", "Appointment"
        };

        private List<JobCardDisplayRow> data = new ArrayList<>();

        /**
         * Replaces current data and fires a full repaint notification.
         *
         * @param rows the new list of display rows
         */
        public void setData(List<JobCardDisplayRow> rows) {
            this.data = (rows != null) ? rows : new ArrayList<>();
            fireTableDataChanged();
        }

        /**
         * Returns the display row at the given table index.
         *
         * @param rowIndex table row index
         * @return the {@link JobCardDisplayRow} at that position
         */
        public JobCardDisplayRow getRowAt(int rowIndex) { return data.get(rowIndex); }

        @Override public int getRowCount()    { return data.size(); }
        @Override public int getColumnCount() { return COLUMNS.length; }
        @Override public String getColumnName(int col) { return COLUMNS[col]; }
        @Override public boolean isCellEditable(int r, int c) { return false; }

        @Override
        public Class<?> getColumnClass(int col) {
            // Column 5 (Labour Hrs) must be String.class because getValueAt()
            // returns "—" (a String) when labourHours == 0.  Declaring it as
            // Double.class would cause JTable$DoubleRenderer to call
            // DecimalFormat.format("—") → IllegalArgumentException.
            return switch (col) {
                case 0 -> Integer.class;
                default -> String.class;
            };
        }

        @Override
        public Object getValueAt(int row, int col) {
            JobCardDisplayRow r = data.get(row);
            return switch (col) {
                case 0 -> r.jobCardId;
                case 1 -> r.vehicleRegNo;
                case 2 -> r.customerName;
                case 3 -> r.serviceName;
                case 4 -> Objects.requireNonNullElse(r.technicianName, "—");
                case 5 -> r.labourHours > 0
                    ? String.format("%.2f hrs", r.labourHours) : "—";
                case 6 -> r.status;
                case 7 -> (r.appointmentDt != null)
                    ? r.appointmentDt.format(DT_FMT) : "—";
                default -> "";
            };
        }
    }

    // ── Inner: Status Column Renderer ─────────────────────────────────────────

    /**
     * Custom cell renderer that renders the Status column as a Fluent-styled
     * colour-coded badge text.
     *
     * <ul>
     *   <li>BOOKED      → Fluent Blue</li>
     *   <li>IN_PROGRESS → Fluent Amber</li>
     *   <li>COMPLETED   → Success Green</li>
     *   <li>DELIVERED   → Neutral Grey</li>
     * </ul>
     */
    private static class StatusColumnRenderer extends DefaultTableCellRenderer {

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
                    case "DELIVERED"   -> FluentTheme.STATUS_NEUTRAL;
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
