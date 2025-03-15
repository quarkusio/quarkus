package io.quarkus.hibernate.orm.runtime.customized;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.service.UnknownUnwrapTypeException;

import io.agroal.api.AgroalDataSource;

public class QuarkusConnectionProvider implements ConnectionProvider {

    private final AgroalDataSource dataSource;

    public QuarkusConnectionProvider(final AgroalDataSource dataSource) {
        this.dataSource = dataSource;
    }

    public AgroalDataSource getDataSource() {
        return dataSource;
    }

    @Override
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public void closeConnection(final Connection connection) throws SQLException {
        connection.close();
    }

    @Override
    public boolean supportsAggressiveRelease() {
        return true;
    }

    @Override
    public boolean isUnwrappableAs(final Class unwrapType) {
        return ConnectionProvider.class.equals(unwrapType) ||
                QuarkusConnectionProvider.class.isAssignableFrom(unwrapType) ||
                DataSource.class.isAssignableFrom(unwrapType) ||
                AgroalDataSource.class.isAssignableFrom(unwrapType);
    }

    @Override
    public <T> T unwrap(final Class<T> unwrapType) {
        if (ConnectionProvider.class.equals(unwrapType) ||
                QuarkusConnectionProvider.class.isAssignableFrom(unwrapType)) {
            return (T) this;
        } else if (DataSource.class.isAssignableFrom(unwrapType) || AgroalDataSource.class.isAssignableFrom(unwrapType)) {
            return (T) dataSource;
        } else {
            throw new UnknownUnwrapTypeException(unwrapType);
        }
    }
}
