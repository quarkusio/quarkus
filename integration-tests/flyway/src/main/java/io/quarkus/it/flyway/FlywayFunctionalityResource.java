package io.quarkus.it.flyway;

import java.util.Map;
import java.util.Objects;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;

import io.quarkus.flyway.FlywayDataSource;

@Path("/")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class FlywayFunctionalityResource {
    @Inject
    Flyway flyway;

    @Inject
    @FlywayDataSource("second-datasource")
    Flyway flyway2;

    @GET
    @Path("migrate")
    public String doMigrateAuto() {
        flyway.migrate();
        MigrationVersion version = Objects.requireNonNull(flyway.info().current().getVersion(),
                "Version is null! Migration was not applied");
        return version.toString();
    }

    @GET
    @Path("multiple-flyway-migratation")
    public String doMigratationOfSecondDataSource() {
        flyway2.migrate();
        MigrationVersion version = Objects.requireNonNull(flyway2.info().current().getVersion(),
                "Version is null! Migration was not applied for second datasource");
        return version.toString();
    }

    @GET
    @Path("placeholders")
    public Map<String, String> returnPlaceholders() {
        return flyway.getConfiguration().getPlaceholders();
    }

}
