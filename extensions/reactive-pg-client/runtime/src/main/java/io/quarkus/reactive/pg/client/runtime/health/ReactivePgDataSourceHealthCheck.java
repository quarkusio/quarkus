package io.quarkus.reactive.pg.client.runtime.health;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.eclipse.microprofile.health.Readiness;

import io.quarkus.arc.Arc;
import io.vertx.pgclient.PgPool;

@Readiness
@ApplicationScoped
public class ReactivePgDataSourceHealthCheck implements HealthCheck {
    private PgPool pgPool;

    @PostConstruct
    protected void init() {
        pgPool = Arc.container().instance(PgPool.class).get();
    }

    @Override
    public HealthCheckResponse call() {
        HealthCheckResponseBuilder builder = HealthCheckResponse.named("Reactive PostgreSQL connection health check").up();

        try {
            CompletableFuture<Void> databaseConnectionAttempt = new CompletableFuture<>();
            pgPool.query("SELECT 1")
                    .execute(ar -> {
                        if (ar.failed()) {
                            builder.down();
                        }
                        databaseConnectionAttempt.complete(null);
                    });
            databaseConnectionAttempt.get(10, TimeUnit.SECONDS);
        } catch (Exception exception) {
            builder.down();
        }

        return builder.build();
    }
}
