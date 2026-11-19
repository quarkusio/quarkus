package io.quarkus.devservices.datasource.common;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.wait.strategy.AbstractWaitStrategy;

/**
 * Waits until a valid JDBC connection to the database of a {@link JdbcDatabaseContainer} can be opened.
 * <p>
 * Unlike waiting for log messages, this does not depend on what the image logs,
 * so it also works with images that log differently from the official ones,
 * for example because they use another entrypoint or another locale.
 */
public class JdbcConnectionWaitStrategy extends AbstractWaitStrategy {

    private static final int VALIDATION_TIMEOUT_SECONDS = 5;
    private static final long RETRY_INTERVAL_MILLIS = 100L;

    @Override
    protected void waitUntilReady() {
        if (!(waitStrategyTarget instanceof JdbcDatabaseContainer<?> container)) {
            throw new IllegalStateException(
                    "Waiting for a JDBC connection requires a JdbcDatabaseContainer, got " + waitStrategyTarget);
        }

        long deadline = System.nanoTime() + startupTimeout.toNanos();
        SQLException lastFailure = null;
        while (System.nanoTime() < deadline) {
            try {
                if (isConnectionValid(container)) {
                    return;
                }
            } catch (SQLException e) {
                lastFailure = e;
            }
            try {
                Thread.sleep(RETRY_INTERVAL_MILLIS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ContainerLaunchException("Interrupted while waiting for a connection to the database", e);
            }
        }
        throw new ContainerLaunchException("Timed out after " + startupTimeout.toSeconds()
                + " seconds waiting for a connection to the database at " + container.getJdbcUrl(), lastFailure);
    }

    private static boolean isConnectionValid(JdbcDatabaseContainer<?> container) throws SQLException {
        Properties properties = new Properties();
        properties.put("user", container.getUsername());
        properties.put("password", container.getPassword());
        try (Connection connection = container.getJdbcDriverInstance().connect(container.getJdbcUrl(), properties)) {
            return connection != null && connection.isValid(VALIDATION_TIMEOUT_SECONDS);
        }
    }
}
