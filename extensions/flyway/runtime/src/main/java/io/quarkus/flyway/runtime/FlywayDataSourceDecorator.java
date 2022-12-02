package io.quarkus.flyway.runtime;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

import javax.sql.DataSource;

public class FlywayDataSourceDecorator implements DataSource {

    private DataSource dataSource;
    private FlywayDataSourceRuntimeConfig flywayRuntimeConfig;

    public FlywayDataSourceDecorator(DataSource datasource, FlywayDataSourceRuntimeConfig flywayRuntimeConfig) {

        this.dataSource = datasource;
        this.flywayRuntimeConfig = flywayRuntimeConfig;

    }

    public PrintWriter getLogWriter() throws SQLException {
        // TODO Auto-generated method stub
        return dataSource.getLogWriter();
    }

    public int getLoginTimeout() throws SQLException {
        // TODO Auto-generated method stub
        return dataSource.getLoginTimeout();
    }

    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        // TODO Auto-generated method stub
        return dataSource.getParentLogger();
    }

    public void setLogWriter(PrintWriter arg0) throws SQLException {
        // TODO Auto-generated method stub
        dataSource.setLogWriter(arg0);
    }

    public void setLoginTimeout(int arg0) throws SQLException {
        // TODO Auto-generated method stub
        dataSource.setLoginTimeout(arg0);
    }

    public boolean isWrapperFor(Class<?> arg0) throws SQLException {
        // TODO Auto-generated method stub
        return dataSource.isWrapperFor(arg0);
    }

    public <T> T unwrap(Class<T> arg0) throws SQLException {
        // TODO Auto-generated method stub
        return dataSource.unwrap(arg0);
    }

    public Connection getConnection() throws SQLException {
        // TODO Auto-generated method stub
        if (flywayRuntimeConfig.username.isPresent() && flywayRuntimeConfig.password.isPresent()) {
            return dataSource.getConnection(flywayRuntimeConfig.username.get(), flywayRuntimeConfig.password.get());
        }

        return dataSource.getConnection();
    }

    public Connection getConnection(String username, String password)
            throws SQLException {
        // TODO Auto-generated method stub
        return dataSource.getConnection(username, password);
    }

}
