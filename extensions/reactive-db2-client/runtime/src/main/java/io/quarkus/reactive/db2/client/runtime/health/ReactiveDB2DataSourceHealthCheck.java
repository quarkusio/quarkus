package io.quarkus.reactive.db2.client.runtime.health;

import java.time.Duration;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.eclipse.microprofile.health.Readiness;

import io.vertx.mutiny.db2client.DB2Pool;

@Readiness
@ApplicationScoped
public class ReactiveDB2DataSourceHealthCheck implements HealthCheck {

    @Inject
    DB2Pool db2Pool;

    @Override
    public HealthCheckResponse call() {
        HealthCheckResponseBuilder builder = HealthCheckResponse.named("Reactive DB2 connection health check").up();

        try {
            db2Pool.query("SELECT 1 FROM SYSIBM.SYSDUMMY1")
                    .execute()
                    .await().atMost(Duration.ofSeconds(10));
        } catch (Exception exception) {
            builder.down();
        }

        return builder.build();
    }
}
