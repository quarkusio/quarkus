package io.quarkus.it.flyway;

import java.util.Objects;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;

@Path("/")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class FlywayFunctionalityResource {
    @Inject
    Flyway flyway;

    @GET
    @Path("migrate")
    public String doMigrateAuto() {
        flyway.migrate();
        MigrationVersion version = Objects.requireNonNull(flyway.info().current().getVersion(),
                "Version is null! Migration was not applied");
        return version.toString();
    }

}