package io.quarkus.reactive.db2.client.runtime.health;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.eclipse.microprofile.health.Readiness;

import io.quarkus.arc.Arc;
import io.vertx.db2client.DB2Pool;

@Readiness
@ApplicationScoped
public class ReactiveDB2DataSourceHealthCheck implements HealthCheck {
    private DB2Pool db2Pool;

    @PostConstruct
    protected void init() {
        db2Pool = Arc.container().instance(DB2Pool.class).get();
    }

    @Override
    public HealthCheckResponse call() {
        HealthCheckResponseBuilder builder = HealthCheckResponse.named("Reactive DB2 connection health check").up();

        try {
            CompletableFuture<Void> databaseConnectionAttempt = new CompletableFuture<>();
            db2Pool.query("SELECT 1 FROM SYSIBM.SYSDUMMY1")
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
