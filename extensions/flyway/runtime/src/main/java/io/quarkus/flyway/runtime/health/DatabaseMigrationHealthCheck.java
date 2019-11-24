package io.quarkus.flyway.runtime.health;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.eclipse.microprofile.health.Readiness;
import org.flywaydb.core.Flyway;

import io.quarkus.arc.Arc;

@Readiness
@ApplicationScoped
public class DatabaseMigrationHealthCheck implements HealthCheck {

    private Flyway flyway;

    @PostConstruct
    protected void init() {
        flyway = Arc.container().instance(Flyway.class).get();
    }

    @Override
    public HealthCheckResponse call() {
        HealthCheckResponseBuilder builder = HealthCheckResponse.named("Flyway database migrations health check").up();
        int pendingLength = flyway.info().pending().length;
        if (pendingLength != 0) {
            String data = "Validation check failed with " + pendingLength + " pending migrations";
            String dsName = "quarkus-flyway";
            builder.down().withData(dsName, data);
        }
        return builder.build();
    }
}
