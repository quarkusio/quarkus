package io.quarkus.jdbc.runtime.runtime;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverPropertyInfo;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Build-time placeholder driver for datasources of database kind {@link RuntimeJdbc#DB_KIND}.
 * <p>
 * It only exists to satisfy the build-time validation and recording of the Agroal extension:
 * before the pool is created, {@link RuntimeJdbcDriverResolver} substitutes it with the driver
 * class configured with {@code quarkus.datasource[."datasource-name"].jdbc-runtime.driver},
 * so this class is never instantiated.
 */
public final class RuntimeJdbcDriverPlaceholder implements Driver {

    @Override
    public Connection connect(final String url, final Properties info) {
        throw placeholderUsed();
    }

    @Override
    public boolean acceptsURL(final String url) {
        throw placeholderUsed();
    }

    @Override
    public DriverPropertyInfo[] getPropertyInfo(final String url, final Properties info) {
        throw placeholderUsed();
    }

    @Override
    public int getMajorVersion() {
        throw placeholderUsed();
    }

    @Override
    public int getMinorVersion() {
        throw placeholderUsed();
    }

    @Override
    public boolean jdbcCompliant() {
        throw placeholderUsed();
    }

    @Override
    public Logger getParentLogger() {
        throw placeholderUsed();
    }

    private static IllegalStateException placeholderUsed() {
        return new IllegalStateException(RuntimeJdbcDriverPlaceholder.class.getName()
                + " is a build-time placeholder and should never be used at runtime."
                + " The actual JDBC driver must be set with the"
                + " 'quarkus.datasource[.\"datasource-name\"].jdbc-runtime.driver' configuration property.");
    }
}
