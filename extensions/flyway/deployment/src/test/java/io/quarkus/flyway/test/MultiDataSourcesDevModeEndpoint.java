package io.quarkus.flyway.test;

import java.util.Arrays;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.Location;
import org.flywaydb.core.api.configuration.Configuration;

import io.quarkus.flyway.FlywayDataSource;

@Path("/fly")
public class MultiDataSourcesDevModeEndpoint {

    @Inject
    Flyway flywayDefault;

    @Inject
    @FlywayDataSource("users")
    Flyway flywayUsers;

    @Inject
    @FlywayDataSource("inventory")
    Flyway flywayInventory;

    @GET
    @Produces("text/plain")
    public String locations(@QueryParam("name") @DefaultValue("default") String name) {
        Configuration configuration;
        if ("default".equals(name)) {
            configuration = flywayDefault.getConfiguration();
        } else if ("users".equals(name)) {
            configuration = flywayUsers.getConfiguration();
        } else if ("inventory".equals(name)) {
            configuration = flywayInventory.getConfiguration();
        } else {
            throw new RuntimeException("Flyway " + name + " not found");
        }

        return Arrays.stream(configuration.getLocations()).map(Location::getPath).collect(Collectors.joining(","));
    }

}
