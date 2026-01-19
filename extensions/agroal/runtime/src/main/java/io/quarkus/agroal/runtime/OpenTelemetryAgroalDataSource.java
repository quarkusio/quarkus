package io.quarkus.agroal.runtime;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.ShardingKeyBuilder;
import java.util.Collection;
import java.util.List;

import io.agroal.api.AgroalDataSource;
import io.agroal.api.AgroalDataSourceMetrics;
import io.agroal.api.AgroalPoolInterceptor;
import io.agroal.api.configuration.AgroalDataSourceConfiguration;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.jdbc.datasource.OpenTelemetryDataSource;
import io.quarkus.arc.Arc;

/**
 * The {@link AgroalDataSource} wrapper that activates OpenTelemetry JDBC instrumentation.
 */
public class OpenTelemetryAgroalDataSource extends OpenTelemetryDataSource implements AgroalDataSource {

    private final AgroalDataSource delegate;

    public OpenTelemetryAgroalDataSource(AgroalDataSource delegate) {
        super(delegate, Arc.container().instance(OpenTelemetry.class).get());
        this.delegate = delegate;
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
