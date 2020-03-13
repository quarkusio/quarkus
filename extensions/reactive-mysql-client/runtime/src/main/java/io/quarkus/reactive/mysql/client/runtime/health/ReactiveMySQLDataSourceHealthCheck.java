package io.quarkus.reactive.mysql.client.runtime.health;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.eclipse.microprofile.health.Readiness;

import io.quarkus.arc.Arc;
import io.vertx.mysqlclient.MySQLPool;

@Readiness
@ApplicationScoped
public class ReactiveMySQLDataSourceHealthCheck implements HealthCheck {

    private MySQLPool mySQLPool;

    @PostConstruct
    protected void init() {
        mySQLPool = Arc.container().instance(MySQLPool.class).get();
    }

    @Override
    public HealthCheckResponse call() {
        HealthCheckResponseBuilder builder = HealthCheckResponse.named("Reactive MySQL connection health check").up();

        try {
            CompletableFuture<Void> databaseConnectionAttempt = new CompletableFuture<>();
            mySQLPool.query("SELECT 1")
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
