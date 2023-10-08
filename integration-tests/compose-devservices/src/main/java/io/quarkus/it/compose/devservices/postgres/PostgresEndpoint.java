package io.quarkus.it.compose.devservices.postgres;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@Path("/postgres")
public class PostgresEndpoint {

    @ConfigProperty(name = "postgres.db.name")
    String dbName;

    @ConfigProperty(name = "postgres.db.port")
    String dbPort;

    @GET
    @Path("/name")
    public String dbName() {
        return dbName;
    }

    @GET
    @Path("/port")
    public String dbPort() {
        return dbPort;
    }

}
