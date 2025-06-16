package io.quarkus.it.jpa.mysql;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import com.mysql.cj.jdbc.MysqlXADataSource;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.agroal.api.AgroalDataSource;
import io.agroal.api.configuration.AgroalConnectionFactoryConfiguration;
import io.quarkus.agroal.DataSource;

@Path("/jpa-mysql/testxaconnection")
@Produces(MediaType.TEXT_PLAIN)
public class XaConnectionsEndpoint {

    @Inject
    @DataSource("samebutxa")
    AgroalDataSource xaDatasource;

    @GET
    public String test() throws IOException {

        // Test 1#
        // Verify that the connection can be obtained
        try (var ignored = xaDatasource.getConnection()) {
            //The main goal is to check that the connection could be opened
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        // Test 2#
        // Check it's of the expected configuration
        var connectionProviderClass = xaDatasource.getConfiguration().connectionPoolConfiguration()
                .connectionFactoryConfiguration().connectionProviderClass();
        return connectionProviderClass.equals(MysqlXADataSource.class)
                ? "OK"
                : "Unexpected Driver class: " + connectionProviderClass.getName();

    }

}
