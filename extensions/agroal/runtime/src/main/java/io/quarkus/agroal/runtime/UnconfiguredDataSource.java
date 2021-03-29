package io.quarkus.agroal.runtime;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import io.agroal.api.AgroalDataSource;
import io.agroal.api.AgroalDataSourceMetrics;
import io.agroal.api.AgroalPoolInterceptor;
import io.agroal.api.configuration.AgroalDataSourceConfiguration;
import io.quarkus.runtime.configuration.ConfigurationException;

public class UnconfiguredDataSource implements AgroalDataSource {

    private final String errorMessage;

    public UnconfiguredDataSource(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public void throwException() {
        throw new ConfigurationException(errorMessage);
    }

    @Override
    public AgroalDataSourceConfiguration getConfiguration() {
        throw new ConfigurationException(errorMessage);
    }

    @Override
    public AgroalDataSourceMetrics getMetrics() {
        throw new ConfigurationException(errorMessage);
    }

    @Override
    public void flush(FlushMode mode) {
        throw new ConfigurationException(errorMessage);
    }

    @Override
    public void setPoolInterceptors(Collection<? extends AgroalPoolInterceptor> arg0) {
        //noop
    }

    @Override
    public List<AgroalPoolInterceptor> getPoolInterceptors() {
        return Collections.emptyList();
    }

    @Override
    public void close() {

    }

    @Override
    public Connection getConnection() throws SQLException {
        throw new ConfigurationException(errorMessage);
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        throw new ConfigurationException(errorMessage);
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        throw new ConfigurationException(errorMessage);
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        throw new ConfigurationException(errorMessage);
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        throw new ConfigurationException(errorMessage);
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        throw new ConfigurationException(errorMessage);
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        throw new ConfigurationException(errorMessage);
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        throw new ConfigurationException(errorMessage);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return false;
    }
}
