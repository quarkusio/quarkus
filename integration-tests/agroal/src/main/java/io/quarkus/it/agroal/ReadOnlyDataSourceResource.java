package io.quarkus.it.agroal;

import java.sql.Connection;
import java.sql.SQLException;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.agroal.api.AgroalDataSource;
import io.agroal.api.configuration.AgroalConnectionFactoryConfiguration;
import io.quarkus.agroal.DataSource;

/**
 * Exposes Agroal and JDBC read-only status over HTTP so the same checks can run
 * under {@code @QuarkusTest} and {@code @QuarkusIntegrationTest} (native).
 */
@Path("/agroal/read-only-test")
public class ReadOnlyDataSourceResource {

    @Inject
    AgroalDataSource defaultDataSource;

    @Inject
    @DataSource("readonly")
    AgroalDataSource readOnlyDataSource;

    @GET
    @Path("/default")
    @Produces(MediaType.TEXT_PLAIN)
    public String defaultStatus() throws SQLException {
        return status(defaultDataSource);
    }

    @GET
    @Path("/readonly")
    @Produces(MediaType.TEXT_PLAIN)
    public String readOnlyStatus() throws SQLException {
        return status(readOnlyDataSource);
    }

    /**
     * Format: {@code agroalReadOnly,connectionReadOnly}
     */
    private static String status(AgroalDataSource dataSource) throws SQLException {
        AgroalConnectionFactoryConfiguration factory = dataSource.getConfiguration()
                .connectionPoolConfiguration()
                .connectionFactoryConfiguration();
        boolean agroalReadOnly = factory.readOnly();
        boolean connectionReadOnly;
        try (Connection connection = dataSource.getConnection()) {
            connectionReadOnly = connection.isReadOnly();
        }
        return agroalReadOnly + "," + connectionReadOnly;
    }
}
