package io.quarkus.reactive.mysql.client.runtime.health;

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
import io.vertx.mysqlclient.MySQLPool;

@Readiness
@ApplicationScoped
class ReactiveMySQLDataSourcesHealthCheck implements HealthCheck {

    private Map<String, MySQLPool> mySQLPools = new HashMap<>();

    @PostConstruct
    protected void init() {
        for (InstanceHandle<MySQLPool> handle : Arc.container().select(MySQLPool.class, Any.Literal.INSTANCE).handles()) {
            mySQLPools.put(getMySQLPoolName(handle.getBean()), handle.get());
        }
    }

    @Override
    public HealthCheckResponse call() {
        HealthCheckResponseBuilder builder = HealthCheckResponse.named("Reactive MySQL connections health check");
        builder.up();

        for (Entry<String, MySQLPool> mySQLPoolEntry : mySQLPools.entrySet()) {
            String dataSourceName = mySQLPoolEntry.getKey();
            MySQLPool mySQLPool = mySQLPoolEntry.getValue();
            try {
                CompletableFuture<Void> databaseConnectionAttempt = new CompletableFuture<>();
                mySQLPool.query("SELECT 1")
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

    private String getMySQLPoolName(Bean<?> bean) {
        for (Object qualifier : bean.getQualifiers()) {
            if (qualifier instanceof ReactiveDataSource) {
                return ((ReactiveDataSource) qualifier).value();
            }
        }
        return DataSourceUtil.DEFAULT_DATASOURCE_NAME;
    }
}
