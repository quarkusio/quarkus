package io.quarkus.reactive.pg.client.runtime.health;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.spi.Bean;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.eclipse.microprofile.health.Readiness;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.reactive.datasource.ReactiveDataSource;
import io.vertx.pgclient.PgPool;

@Readiness
@ApplicationScoped
class ReactivePgDataSourcesHealthCheck implements HealthCheck {

    private Map<String, PgPool> pgPools = new HashMap<>();

    @PostConstruct
    protected void init() {
        for (InstanceHandle<PgPool> handle : Arc.container().select(PgPool.class, Any.Literal.INSTANCE).handles()) {
            pgPools.put(getPgPoolName(handle.getBean()), handle.get());
        }
    }

    @Override
    public HealthCheckResponse call() {
        HealthCheckResponseBuilder builder = HealthCheckResponse.named("Reactive PostgreSQL connections health check");
        builder.up();

        for (Entry<String, PgPool> pgPoolEntry : pgPools.entrySet()) {
            String dataSourceName = pgPoolEntry.getKey();
            PgPool pgPool = pgPoolEntry.getValue();
            try {
                CompletableFuture<Void> databaseConnectionAttempt = new CompletableFuture<>();
                pgPool.query("SELECT 1")
                        .execute(ar -> {
                            if (ar.failed()) {
                                builder.down();
                                builder.withData(dataSourceName, "down - connection failed: " + ar.cause().getMessage());
                            }
                            databaseConnectionAttempt.complete(null);
                        });
                databaseConnectionAttempt.get(10, TimeUnit.SECONDS);
                builder.withData(dataSourceName, "up");
            } catch (Exception exception) {
                builder.down();
                builder.withData(dataSourceName, "down - connection failed: " + exception.getMessage());
            }
        }

        return builder.build();
    }

    private String getPgPoolName(Bean<?> bean) {
        for (Object qualifier : bean.getQualifiers()) {
            if (qualifier instanceof ReactiveDataSource) {
                return ((ReactiveDataSource) qualifier).value();
            }
        }
        return DataSourceUtil.DEFAULT_DATASOURCE_NAME;
    }
}
