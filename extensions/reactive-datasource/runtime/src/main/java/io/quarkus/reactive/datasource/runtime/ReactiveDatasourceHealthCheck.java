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

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;

public abstract class ReactiveDatasourceHealthCheck implements HealthCheck {

    private static final Logger log = Logger.getLogger(ReactiveDatasourceHealthCheck.class);

    private final Map<String, PoolHealthEntry> pools = new ConcurrentHashMap<>();
    private final String healthCheckResponseName;
    private final String defaultHealthCheckSQL;

    protected ReactiveDatasourceHealthCheck(String healthCheckResponseName, String defaultHealthCheckSQL) {
        this.healthCheckResponseName = healthCheckResponseName;
        this.defaultHealthCheckSQL = defaultHealthCheckSQL;
    }

    protected void addPool(String name, Pool pool) {
        addPool(name, pool, defaultHealthCheckSQL);
    }

    protected void addPool(String name, Pool pool, String healthCheckSQL) {
        final PoolHealthEntry previous = pools.put(name, new PoolHealthEntry(pool, healthCheckSQL));
        if (previous != null) {
            throw new IllegalStateException("Duplicate pool name: " + name);
        }
    }

    @Override
    public HealthCheckResponse call() {
        HealthCheckResponseBuilder builder = HealthCheckResponse.named(healthCheckResponseName);
        builder.up();

        for (Map.Entry<String, PoolHealthEntry> poolEntry : pools.entrySet()) {
            final String dataSourceName = poolEntry.getKey();
            final PoolHealthEntry entry = poolEntry.getValue();
            try {
                CompletableFuture<Void> databaseConnectionAttempt = new CompletableFuture<>();
                Context context = Vertx.currentContext();
                if (context != null) {
                    log.debug("Run health check on the current Vert.x context");
                    context.runOnContext(v -> {
                        entry.pool.query(entry.healthCheckSQL)
                                .execute().onComplete(ar -> {
                                    checkFailure(ar, builder, dataSourceName);
                                    databaseConnectionAttempt.complete(null);
                                });
                    });
                } else {
                    log.warn("Vert.x context unavailable to perform health check of reactive datasource `" + dataSourceName
                            + "`. This is unlikely to work correctly.");
                    entry.pool.query(entry.healthCheckSQL)
                            .execute().onComplete(ar -> {
                                checkFailure(ar, builder, dataSourceName);
                                databaseConnectionAttempt.complete(null);
                            });
                }

                //20 seconds is rather high, but using just 10 is often not enough on slow CI
                //systems, especially if the connections have to be established for the first time.
                databaseConnectionAttempt.get(20, TimeUnit.SECONDS);
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
        } else {
            builder.withData(dataSourceName, "UP");
        }
    }

    /**
     * @deprecated Use {@link ReactiveDataSourceUtil#dataSourceName(Bean)} instead.
     */
    @Deprecated
    protected String getPoolName(Bean<? extends Pool> bean) {
        return ReactiveDataSourceUtil.dataSourceName(bean);
    }

    private static class PoolHealthEntry {
        final Pool pool;
        final String healthCheckSQL;

        PoolHealthEntry(Pool pool, String healthCheckSQL) {
            this.pool = pool;
            this.healthCheckSQL = healthCheckSQL;
        }
    }
}
