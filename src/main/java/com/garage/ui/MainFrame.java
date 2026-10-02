/**
 * MainFrame — Root application window containing a Fluent-styled JTabbedPane.
 * Each tab hosts a dedicated panel for one functional module.
 *
 * Course  : UIT3361 OOP Java / UIT3311 Database Technology Lab
 * Project : Vehicle Service Management System
 */
package com.garage.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
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
        super("Garage Pro — Vehicle Service Management System");

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1100, 720));
        setPreferredSize(new Dimension(1280, 800));

        getContentPane().setBackground(FluentTheme.CANVAS);
        setLayout(new BorderLayout());

        // ── App header bar ─────────────────────────────────────────────────────
        add(buildAppHeader(), BorderLayout.NORTH);

        // ── Tabbed content ─────────────────────────────────────────────────────
        tabbedPane = buildTabbedPane();
        add(tabbedPane, BorderLayout.CENTER);

        // ── Status bar ────────────────────────────────────────────────────────
        add(buildStatusBar(), BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(null);
    }

    // ── UI Builders ───────────────────────────────────────────────────────────

    /**
     * Builds the top application header: branding icon + title + subtitle.
     *
     * @return the header panel
     */
    private JPanel buildAppHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(FluentTheme.SURFACE_ALT);
        header.setBorder(new MatteBorder(0, 0, 1, 0, FluentTheme.BORDER));

        // Left: branding
        JPanel brand = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        brand.setBackground(FluentTheme.SURFACE_ALT);
        brand.setBorder(new EmptyBorder(12, 20, 12, 20));

        // Accent icon block
        JLabel iconBlock = new JLabel(" \uD83D\uDD27 ");
        iconBlock.setFont(new Font(FluentTheme.FONT_APP_TITLE.getFamily(), Font.PLAIN, 22));
        iconBlock.setForeground(FluentTheme.ACCENT);
        brand.add(iconBlock);

        JPanel titles = new JPanel();
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        titles.setBackground(FluentTheme.SURFACE_ALT);
        titles.setBorder(new EmptyBorder(0, 6, 0, 0));

        JLabel appTitle = new JLabel("Garage Pro");
        appTitle.setFont(FluentTheme.FONT_APP_TITLE);
        appTitle.setForeground(FluentTheme.TEXT_PRIMARY);

        JLabel appSub = new JLabel("Vehicle Service Management System");
        appSub.setFont(FluentTheme.FONT_CAPTION);
        appSub.setForeground(FluentTheme.TEXT_MUTED);

        titles.add(appTitle);
        titles.add(appSub);
        brand.add(titles);

        header.add(brand, BorderLayout.WEST);

        // Right: version / env badge
        JLabel badge = new JLabel("v1.0  |  Oracle 23c  ");
        badge.setFont(FluentTheme.FONT_CAPTION);
        badge.setForeground(FluentTheme.TEXT_MUTED);
        badge.setBorder(new EmptyBorder(0, 0, 0, 16));
        header.add(badge, BorderLayout.EAST);

        return header;
    }

    /**
     * Builds and configures the main tabbed pane with all module tabs.
     *
     * @return the configured JTabbedPane
     */
    private JTabbedPane buildTabbedPane() {
        JTabbedPane tp = new JTabbedPane(JTabbedPane.TOP);
        tp.setBackground(FluentTheme.CANVAS);
        tp.setForeground(FluentTheme.TEXT_MUTED);
        tp.setFont(FluentTheme.FONT_SEMIBOLD);

        // ── Tab definitions ────────────────────────────────────────────────────
        tp.addTab("  \uD83D\uDC64  Customers",        new CustomerPanel());
        tp.addTab("  \uD83D\uDE97  Vehicles",          new VehiclePanel());
        tp.addTab("  \uD83D\uDDD3\uFE0F  Booking",    new BookingPanel());
        tp.addTab("  \uD83D\uDCCB  Job Cards",         new JobCardPanel());
        tp.addTab("  \uD83D\uDCB0  Billing",           new BillingPanel());
        tp.addTab("  \uD83D\uDCC5  Service History",   new ServiceHistoryPanel());

        return tp;
    }

    /**
     * Builds the application status bar at the bottom.
     *
     * @return the status bar panel
     */
    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(new Color(0x1A1A1A));
        bar.setBorder(new MatteBorder(1, 0, 0, 0, FluentTheme.BORDER));

        JLabel left = new JLabel("  Ready — Garage Pro v1.0");
        left.setFont(FluentTheme.FONT_CAPTION);
        left.setForeground(FluentTheme.TEXT_MUTED);
        left.setBorder(new EmptyBorder(4, 8, 4, 0));
        bar.add(left, BorderLayout.WEST);

        JLabel right = new JLabel("UIT3361 OOP Java / UIT3311 Database Technology  ");
        right.setFont(FluentTheme.FONT_CAPTION);
        right.setForeground(new Color(0x555555));
        bar.add(right, BorderLayout.EAST);

        return bar;
    }
}
