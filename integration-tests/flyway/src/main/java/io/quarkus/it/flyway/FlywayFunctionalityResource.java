package io.quarkus.it.flyway;

import java.util.Map;
import java.util.Objects;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;

import io.quarkus.flyway.FlywayDataSource;

@Path("/")
public class FlywayFunctionalityResource {
    @Inject
    Flyway flyway;

    @Inject
    @FlywayDataSource("second-datasource")
    Flyway flyway2;

    @Inject
    EntityManager entityManager;

    @GET
    @Path("migrate")
    public String doMigrateAuto() {
        flyway.migrate();
        MigrationVersion version = Objects.requireNonNull(flyway.info().current().getVersion(),
                "Version is null! Migration was not applied");
        return version.toString();
    }

    @GET
    @Path("title")
    public String returnTitle() {
        return entityManager.createQuery("select a from AppEntity a where a.id = 1", AppEntity.class)
                .getSingleResult().getName();
    }

    @GET
    @Path("multiple-flyway-migration")
    public String doMigrationOfSecondDataSource() {
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

    @GET
    @Path("create-schemas")
    public boolean returnCreateSchema() {
        return flyway.getConfiguration().isCreateSchemas();
    }

    @GET
    @Path("init-sql")
    public String returnInitSql() {
        return flyway.getConfiguration().getInitSql();
    }

    @GET
    @Path("init-sql-result")
    public Integer returnInitSqlResult() {
        return (Integer) entityManager.createNativeQuery("SELECT ONE_HUNDRED")
                .getSingleResult();
    }

}
