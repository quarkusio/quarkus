package io.quarkus.agroal.runtime;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.ShardingKeyBuilder;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

import io.agroal.api.AgroalDataSource;
import io.agroal.api.AgroalDataSourceMetrics;
import io.agroal.api.AgroalPoolInterceptor;
import io.agroal.api.configuration.AgroalDataSourceConfiguration;
import io.opentelemetry.instrumentation.jdbc.datasource.OpenTelemetryDataSource;

/**
 * The {@link AgroalDataSource} wrapper that activates OpenTelemetry JDBC instrumentation.
 * <p>
 * Uses composition to wrap an {@link OpenTelemetryDataSource} (for instrumented connections)
 * while delegating {@link AgroalDataSource}-specific operations to the original data source.
 */
public class OpenTelemetryAgroalDataSource implements AgroalDataSource {

    private final AgroalDataSource delegate;
    private final OpenTelemetryDataSource otelDataSource;

    public OpenTelemetryAgroalDataSource(AgroalDataSource delegate, OpenTelemetryDataSource otelDataSource) {
        this.delegate = delegate;
        this.otelDataSource = otelDataSource;
    }

    @Override
    public Connection getConnection() throws SQLException {
        return otelDataSource.getConnection();
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return otelDataSource.getConnection(username, password);
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return delegate.getLogWriter();
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        delegate.setLogWriter(out);
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return delegate.getLoginTimeout();
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        delegate.setLoginTimeout(seconds);
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return delegate.getParentLogger();
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return delegate.unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return delegate.isWrapperFor(iface);
    }

    @Override
    public boolean isHealthy(boolean newConnection) throws SQLException {
        return delegate.isHealthy(newConnection);
    }

    @Override
    public Connection getReadOnlyConnection() throws SQLException {
        return delegate.getReadOnlyConnection();
    }

    @Override
    public AgroalDataSourceConfiguration getConfiguration() {
        return delegate.getConfiguration();
    }

    @Override
    public AgroalDataSourceMetrics getMetrics() {
        return delegate.getMetrics();
    }

    @Override
    public void flush(FlushMode mode) {
        delegate.flush(mode);
    }

    @Override
    public void setPoolInterceptors(Collection<? extends AgroalPoolInterceptor> interceptors) {
        delegate.setPoolInterceptors(interceptors);
    }

    @Override
    public List<AgroalPoolInterceptor> getPoolInterceptors() {
        return delegate.getPoolInterceptors();
    }

    @Override
    public ShardingKeyBuilder createShardingKeyBuilder() throws SQLException {
        return delegate.createShardingKeyBuilder();
    }

    @Override
    public void close() {
        delegate.close();
    }
}
