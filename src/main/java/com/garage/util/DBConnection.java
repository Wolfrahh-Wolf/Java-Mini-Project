/**
 * DBConnection — Thread-safe singleton providing the shared JDBC Connection.
 * Credentials are loaded exclusively from the project's .env file via dotenv-java.
 * Connection is initialised once at class-load time (static initialiser block).
 */
package com.garage.util;

import io.github.cdimascio.dotenv.Dotenv;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Provides a single shared {@link Connection} to the Oracle database.
 *
 * <p>The connection is created once when the class is first loaded.
 * This is safe for single-user desktop use.
 * All DAO methods receive this connection as a parameter to support
 * service-layer transaction demarcation (setAutoCommit / commit / rollback).
 */
public final class DBConnection {

    /** The single shared connection instance. */
    private static final Connection INSTANCE;

    static {
        Dotenv env = Dotenv.configure()
                           .ignoreIfMissing()
                           .load();

        String url      = env.get("DB_URL",      "jdbc:oracle:thin:@localhost:1521/FREEPDB1");
        String user     = env.get("DB_USER",     "garage_user");
        String password = env.get("DB_PASSWORD", "");

        try {
            INSTANCE = DriverManager.getConnection(url, user, password);
            // Default to auto-commit ON; service methods disable it when needed.
            INSTANCE.setAutoCommit(true);
        } catch (SQLException e) {
            throw new ExceptionInInitializerError(
                "DBConnection: Failed to connect to Oracle database. " +
                "Check .env file and ensure the Docker container is running. " +
                "Cause: " + e.getMessage()
            );
        }
    }

    /** Private constructor — prevents instantiation. */
    private DBConnection() {}

    /**
     * Returns the shared JDBC {@link Connection}.
     *
     * @return the singleton Connection instance
     */
    public static Connection getConnection() {
        return INSTANCE;
    }
}
