package io.quarkus.flyway.runtime;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Optional;
import java.util.logging.Logger;

import javax.sql.DataSource;

public class DataSourceDecorator implements DataSource {

    private DataSource dataSource;
    private Optional<String> username;
    private Optional<String> password;

    public DataSourceDecorator(DataSource datasource, Optional<String> username, Optional<String> password) {

        this.dataSource = datasource;
        this.username = username;
        this.password = password;

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
        return dataSource.getConnection(username.get(), password.get());
    }

    public Connection getConnection(String username, String password)
            throws SQLException {
        // TODO Auto-generated method stub
        return dataSource.getConnection(username, password);
    }

}
