package io.quarkus.reactive.pg.client.runtime.health;

import java.util.Set;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;

import org.eclipse.microprofile.health.Readiness;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.datasource.runtime.DataSourcesHealthSupport;
import io.quarkus.reactive.datasource.runtime.ReactiveDatasourceHealthCheck;
import io.vertx.pgclient.PgPool;

@Readiness
@ApplicationScoped
class ReactivePgDataSourcesHealthCheck extends ReactiveDatasourceHealthCheck {

    public ReactivePgDataSourcesHealthCheck() {
        super("Reactive PostgreSQL connections health check", "SELECT 1");
    }

    @PostConstruct
    protected void init() {
        ArcContainer container = Arc.container();
        DataSourcesHealthSupport excluded = container.instance(DataSourcesHealthSupport.class).get();
        Set<String> excludedNames = excluded.getExcludedNames();
        for (InstanceHandle<PgPool> handle : container.select(PgPool.class, Any.Literal.INSTANCE).handles()) {
            String poolName = getPoolName(handle.getBean());
            if (!excludedNames.contains(poolName)) {
                addPool(poolName, handle.get());
            }
        }
    }

}
