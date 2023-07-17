package io.quarkus.reactive.datasource.runtime;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import jakarta.enterprise.inject.spi.Bean;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.jboss.logging.Logger;

import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.reactive.datasource.ReactiveDataSource;
import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;

public abstract class ReactiveDatasourceHealthCheck implements HealthCheck {

    private static final Logger log = Logger.getLogger(ReactiveDatasourceHealthCheck.class);

    private final Map<String, Pool> pools = new ConcurrentHashMap<>();
    private final String healthCheckResponseName;
    private final String healthCheckSQL;

    protected ReactiveDatasourceHealthCheck(String healthCheckResponseName, String healthCheckSQL) {
        this.healthCheckResponseName = healthCheckResponseName;
        this.healthCheckSQL = healthCheckSQL;
    }

    protected void addPool(String name, Pool p) {
        final Pool previous = pools.put(name, p);
        if (previous != null) {
            throw new IllegalStateException("Duplicate pool name: " + name);
        }
    }

    @Override
    public HealthCheckResponse call() {
        HealthCheckResponseBuilder builder = HealthCheckResponse.named(healthCheckResponseName);
        builder.up();

        for (Map.Entry<String, Pool> pgPoolEntry : pools.entrySet()) {
            final String dataSourceName = pgPoolEntry.getKey();
            final Pool pgPool = pgPoolEntry.getValue();
            try {
                CompletableFuture<Void> databaseConnectionAttempt = new CompletableFuture<>();
                Context context = Vertx.currentContext();
                if (context != null) {
                    log.debug("Run health check on the current Vert.x context");
                    context.runOnContext(v -> {
                        pgPool.query(healthCheckSQL)
                                .execute(ar -> {
                                    checkFailure(ar, builder, dataSourceName);
                                    databaseConnectionAttempt.complete(null);
                                });
                    });
                } else {
                    log.warn("Vert.x context unavailable to perform health check of reactive datasource `" + dataSourceName
                            + "`. This is unlikely to work correctly.");
                    pgPool.query(healthCheckSQL)
                            .execute(ar -> {
                                checkFailure(ar, builder, dataSourceName);
                                databaseConnectionAttempt.complete(null);
                            });
                }

                //20 seconds is rather high, but using just 10 is often not enough on slow CI
                //systems, especially if the connections have to be established for the first time.
                databaseConnectionAttempt.get(20, TimeUnit.SECONDS);
                builder.withData(dataSourceName, "UP");
            } catch (RuntimeException | ExecutionException exception) {
                operationsError(dataSourceName, exception);
                builder.down();
                builder.withData(dataSourceName, "down - connection failed: " + exception.getMessage());
            } catch (InterruptedException e) {
                log.warn("Interrupted while obtaining database connection for healthcheck of datasource " + dataSourceName);
                Thread.currentThread().interrupt();
                return builder.build();
            } catch (TimeoutException e) {
                log.warn("Timed out while waiting for an available connection to perform healthcheck of datasource "
                        + dataSourceName);
                builder.down();
                builder.withData(dataSourceName, "timed out, unable to obtain connection to perform healthcheck of datasource");
            }
        }

        return builder.build();
    }

    private void operationsError(final String datasourceName, final Throwable cause) {
        log.warn("Error obtaining database connection for healthcheck of datasource '" + datasourceName + '\'', cause);
    }

    private void checkFailure(AsyncResult<RowSet<Row>> ar, HealthCheckResponseBuilder builder, String dataSourceName) {
        if (ar.failed()) {
            operationsError(dataSourceName, ar.cause());
            builder.down();
            builder.withData(dataSourceName, "down - connection failed: " + ar.cause().getMessage());
        }
    }

    protected String getPoolName(Bean<?> bean) {
        for (Object qualifier : bean.getQualifiers()) {
            if (qualifier instanceof ReactiveDataSource) {
                return ((ReactiveDataSource) qualifier).value();
            }
        }
        return DataSourceUtil.DEFAULT_DATASOURCE_NAME;
    }

}
