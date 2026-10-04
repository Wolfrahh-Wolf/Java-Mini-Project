/**
 * MainApp — Application entry point.
 * Installs FlatDarkLaf with Fluent UI 2 Dark token overrides, then bootstraps
 * the Swing UI on the EDT.
 */
package com.garage.ui;

import com.formdev.flatlaf.FlatDarkLaf;

import javax.swing.*;
import java.awt.Insets;

/**
 * Entry point for the Vehicle Service Management System.
 *
 * <p>Responsibilities:
 * <ol>
 *   <li>Install FlatDarkLaf (Fluent UI 2 dark mode) before any component is created.</li>
 *   <li>Apply {@link FluentTheme} global UIManager token overrides.</li>
 *   <li>Force DB connection initialisation to fail-fast on bad config.</li>
 *   <li>Hand off to the Swing Event Dispatch Thread via {@link SwingUtilities#invokeLater}.</li>
 * </ol>
 */
public class MainApp {

    /**
     * Application entry point.
     *
     * @param args command-line arguments (not used)
     */
    public static void main(String[] args) {

        // Step 1: Install FlatDarkLaf — must happen before any Swing component is created.
        try {
            FlatDarkLaf.setup();
            FluentTheme.applyGlobalDefaults();
            UIManager.put("Table.cellMargins", new Insets(4, 12, 4, 12));
        } catch (Exception e) {
            System.err.println("MainApp: Could not install FlatDarkLaf. " +
                               "Falling back to system L&F. Cause: " + e.getMessage());
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) { /* last resort: default cross-platform */ }
        }

        // Step 2: Touch DBConnection here (on the main thread) so that any fatal
        // connection error surfaces immediately with a dialog before the frame appears.
        try {
            com.garage.util.DBConnection.getConnection();
        } catch (ExceptionInInitializerError e) {
            JOptionPane.showMessageDialog(
                null,
                "Cannot connect to the Oracle database.\n\n" +
                "Please ensure:\n" +
                "  1. Docker container 'vsms_oracle' is running\n" +
                "  2. .env file exists with correct DB_URL, DB_USER, DB_PASSWORD\n\n" +
                "Error: " + (e.getCause() != null ? e.getCause().getMessage() : e.getMessage()),
                "Fatal Database Error",
                JOptionPane.ERROR_MESSAGE
            );
            System.exit(1);
        }

        // Step 3: Launch the main window on the Event Dispatch Thread.
        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }
}