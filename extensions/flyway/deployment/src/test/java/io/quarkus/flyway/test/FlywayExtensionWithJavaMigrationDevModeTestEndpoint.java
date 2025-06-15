package io.quarkus.flyway.test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.flywaydb.core.Flyway;

import io.agroal.api.AgroalDataSource;

@Path("/fly")
public class FlywayExtensionWithJavaMigrationDevModeTestEndpoint {

    @Inject
    AgroalDataSource defaultDataSource;

    @Inject
    Flyway flyway;

    @GET
    public String result() throws Exception {
        int count = 0;
        try (Connection connection = defaultDataSource.getConnection();
                Statement stat = connection.createStatement()) {
            try (ResultSet countQuery = stat.executeQuery("select count(1) from quarked_flyway")) {
                countQuery.first();
                count = countQuery.getInt(1);
            }
        }
        String currentVersion = flyway.info().current().getVersion().toString();

        return count + "/" + currentVersion;
    }
}
