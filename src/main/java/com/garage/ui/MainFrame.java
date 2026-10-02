/**
 * MainFrame — Root application window containing a JTabbedPane.
 * Each tab hosts a dedicated panel for one functional module.
 *
 * Course  : UIT3361 OOP Java / UIT3311 Database Technology Lab
 * Project : Vehicle Service Management System
 */
package com.garage.ui;

import javax.swing.*;
import java.awt.*;

/**
 * The main application window.
 *
 * <p>Uses a {@link JTabbedPane} as its root container.
 * All six module tabs are instantiated here:
 * <ol>
 *   <li>Customers   — Phase 1</li>
 *   <li>Vehicles    — Phase 1</li>
 *   <li>Booking     — Phase 2</li>
 *   <li>Job Cards   — Phase 3</li>
 *   <li>Billing     — Phase 4</li>
 *   <li>History     — Phase 5</li>
 * </ol>
 *
 * <p>Must only be instantiated on the Event Dispatch Thread.
 */
public class MainFrame extends JFrame {

    /** The central tabbed container. */
    private final JTabbedPane tabbedPane;

    /**
     * Constructs and configures the main application window.
     * Must be called from the EDT only.
     */
    public MainFrame() {
        // ── Phase 5: updated window title per WORK_SPLIT.md Step 5 ───────────
        super("Vehicle Service Management System — Garage Pro");

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // ── Phase 5: enforce minimum size per WORK_SPLIT.md Step 5 ───────────
        setMinimumSize(new Dimension(900, 650));
        setPreferredSize(new Dimension(1150, 740));

        // ── Root layout ───────────────────────────────────────────────────────
        setLayout(new BorderLayout());

        // ── Header label — Phase 5: bold app title per polish spec ───────────
        JLabel header = new JLabel(
            "  \uD83D\uDD27  Garage Pro — Vehicle Service Management System");
        header.setFont(new Font(Font.DIALOG, Font.BOLD, 14));
        header.setForeground(new Color(30, 30, 80));
        header.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(200, 200, 220)),
            BorderFactory.createEmptyBorder(8, 8, 8, 8)
        ));
        add(header, BorderLayout.NORTH);

        // ── Tabbed pane ───────────────────────────────────────────────────────
        tabbedPane = new JTabbedPane(JTabbedPane.TOP);
        tabbedPane.setFont(new Font(Font.DIALOG, Font.PLAIN, 12));

        // Phase 1
        tabbedPane.addTab("Customers",       new CustomerPanel());
        tabbedPane.addTab("Vehicles",        new VehiclePanel());

        // Phase 2
        tabbedPane.addTab("Booking",         new BookingPanel());

        // Phase 3
        tabbedPane.addTab("Job Cards",       new JobCardPanel());

        // Phase 4
        tabbedPane.addTab("Billing",         new BillingPanel());

        // Phase 5 — Service History (final tab)
        tabbedPane.addTab("Service History", new ServiceHistoryPanel());

        add(tabbedPane, BorderLayout.CENTER);

        // ── Status bar ────────────────────────────────────────────────────────
        JLabel statusBar = new JLabel("  Ready — Vehicle Service Management System");
        statusBar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY),
            BorderFactory.createEmptyBorder(3, 6, 3, 6)
        ));
        statusBar.setFont(new Font(Font.DIALOG, Font.PLAIN, 11));
        add(statusBar, BorderLayout.SOUTH);

        // ── Phase 5: centre on screen per WORK_SPLIT.md Step 5 ───────────────
        pack();
        setLocationRelativeTo(null);
    }
}
