package io.quarkus.hibernate.orm.runtime.service;

import java.sql.Connection;
import java.sql.SQLException;

import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.service.UnknownUnwrapTypeException;

public class QuarkusStaticInitConnectionProvider implements ConnectionProvider {
    @Override
    public Connection getConnection() throws SQLException {
        throw new UnsupportedOperationException(
                "Cannot retrieve a connection to the database during Quarkus' static initialization. Delay the connection retrieval until runtime.");
    }

    @Override
    public void closeConnection(Connection connection) throws SQLException {
        // Should not be called, since getting a connection is impossible.
    }

    @Override
    public boolean supportsAggressiveRelease() {
        return false;
    }

    @Override
    public boolean isUnwrappableAs(final Class<?> unwrapType) {
        return ConnectionProvider.class.equals(unwrapType);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T unwrap(final Class<T> unwrapType) {
        if (ConnectionProvider.class.equals(unwrapType)) {
            return (T) this;
        } else {
            throw new UnknownUnwrapTypeException(unwrapType);
        }
    }
}
