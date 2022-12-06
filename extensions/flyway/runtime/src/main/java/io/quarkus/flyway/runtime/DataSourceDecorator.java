package io.quarkus.flyway.runtime;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

import javax.sql.DataSource;

public class DataSourceDecorator implements DataSource {

    private DataSource dataSource;
    private String username;
    private String password;

    public DataSourceDecorator(DataSource datasource, String username, String password) {

        this.dataSource = datasource;
        this.username = username;
        this.password = password;

    }

    public PrintWriter getLogWriter() throws SQLException {
        return dataSource.getLogWriter();
    }

    public int getLoginTimeout() throws SQLException {
        return dataSource.getLoginTimeout();
    }

    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return dataSource.getParentLogger();
    }

    public void setLogWriter(PrintWriter out) throws SQLException {
        dataSource.setLogWriter(out);
    }

    public void setLoginTimeout(int loginTimeout) throws SQLException {
        dataSource.setLoginTimeout(loginTimeout);
    }

    public boolean isWrapperFor(Class<?> wrapperFor) throws SQLException {
        return dataSource.isWrapperFor(wrapperFor);
    }

    public <T> T unwrap(Class<T> unwrap) throws SQLException {
        return dataSource.unwrap(unwrap);
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection(username, password);
    }

    public Connection getConnection(String username, String password)
            throws SQLException {
        return dataSource.getConnection(username, password);
    }

}
