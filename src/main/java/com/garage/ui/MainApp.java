/**
 * MainApp — Application entry point.
 * Sets the system Look and Feel, then bootstraps the Swing UI on the EDT.
 *
 * Course  : UIT3361 OOP Java / UIT3311 Database Technology Lab
 * Project : Vehicle Service Management System
 */
package com.garage.ui;

import javax.swing.*;

/**
 * Entry point for the Vehicle Service Management System.
 *
 * <p>Responsibilities:
 * <ol>
 *   <li>Install the system native Look and Feel before any component is created.</li>
 *   <li>Force DB connection initialisation via DBConnection (fails early on bad config).</li>
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

        // Step 1: Set system Look and Feel — must happen before any Swing component is created.
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // Non-fatal: fall back to default cross-platform L&F
            System.err.println("MainApp: Could not set system Look and Feel. " +
                               "Falling back to default. Cause: " + e.getMessage());
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
