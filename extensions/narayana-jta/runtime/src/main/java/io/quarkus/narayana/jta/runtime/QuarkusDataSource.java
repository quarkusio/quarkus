package io.quarkus.narayana.jta.runtime;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Optional;
import java.util.logging.Logger;

import javax.sql.DataSource;

import jakarta.enterprise.inject.literal.NamedLiteral;

import io.quarkus.arc.Arc;

public class QuarkusDataSource implements DataSource {
    private final Optional<String> dsName;
    private volatile DataSource datasource;

    public QuarkusDataSource(Optional<String> dsName) {
        this.dsName = dsName;
    }

    private DataSource getDataSource() {
        if (datasource == null) {
            if (dsName.isEmpty()) {
                datasource = Arc.container().instance(DataSource.class).get();
            } else {
                datasource = Arc.container().instance(DataSource.class, NamedLiteral.of(dsName.get())).get();
            }
        }

        return datasource;
    }

    @Override
    public Connection getConnection() throws SQLException {
        return getDataSource().getConnection();
    }

    @Override
    public Connection getConnection(final String user, final String passwd) throws SQLException {
        return getDataSource().getConnection(user, passwd);
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return getDataSource().getLogWriter();
    }

    @Override
    public void setLogWriter(final PrintWriter writer) throws SQLException {
        getDataSource().setLogWriter(writer);
    }

    @Override
    public void setLoginTimeout(final int timeout) throws SQLException {
        getDataSource().setLoginTimeout(timeout);
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return getDataSource().getLoginTimeout();
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return getDataSource().getParentLogger();
    }

    @Override
    public <T> T unwrap(final Class<T> aClass) throws SQLException {
        return getDataSource().unwrap(aClass);
    }

    @Override
    public boolean isWrapperFor(final Class<?> aClass) throws SQLException {
        return getDataSource().isWrapperFor(aClass);
    }
}
