package io.quarkus.reactive.db2.client.runtime.health;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.spi.Bean;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.eclipse.microprofile.health.Readiness;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.datasource.runtime.DataSourcesHealthSupport;
import io.quarkus.reactive.datasource.ReactiveDataSource;
import io.vertx.mutiny.db2client.DB2Pool;

@Readiness
@ApplicationScoped
/**
 * Implementation note: this healthcheck doesn't extend ReactiveDatasourceHealthCheck
 * as a DB2Pool is based on Mutiny: does not extend io.vertx.sqlclient.Pool
 */
class ReactiveDB2DataSourcesHealthCheck implements HealthCheck {

    private Map<String, DB2Pool> db2Pools = new HashMap<>();

    @PostConstruct
    protected void init() {
        ArcContainer container = Arc.container();
        DataSourcesHealthSupport excluded = container.instance(DataSourcesHealthSupport.class).get();
        Set<String> excludedNames = excluded.getExcludedNames();
        for (InstanceHandle<DB2Pool> handle : container.select(DB2Pool.class, Any.Literal.INSTANCE).handles()) {
            String db2PoolName = getDB2PoolName(handle.getBean());
            if (!excludedNames.contains(db2PoolName)) {
                db2Pools.put(db2PoolName, handle.get());
            }
        }
    }

    @Override
    public HealthCheckResponse call() {
        HealthCheckResponseBuilder builder = HealthCheckResponse.named("Reactive DB2 connections health check");
        builder.up();

        for (Entry<String, DB2Pool> db2PoolEntry : db2Pools.entrySet()) {
            String dataSourceName = db2PoolEntry.getKey();
            DB2Pool db2Pool = db2PoolEntry.getValue();
            try {
                db2Pool.query("SELECT 1 FROM SYSIBM.SYSDUMMY1")
                        .execute()
                        .await().atMost(Duration.ofSeconds(10));
                builder.withData(dataSourceName, "up");
            } catch (Exception exception) {
                builder.down();
                builder.withData(dataSourceName, "down - connection failed: " + exception.getMessage());
            }
        }

        return builder.build();
    }

    private String getDB2PoolName(Bean<?> bean) {
        for (Object qualifier : bean.getQualifiers()) {
            if (qualifier instanceof ReactiveDataSource) {
                return ((ReactiveDataSource) qualifier).value();
            }
        }
        return DataSourceUtil.DEFAULT_DATASOURCE_NAME;
    }
}
