package io.quarkus.reactive.pg.client.runtime.health;

import java.util.Set;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;

import org.eclipse.microprofile.health.Readiness;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.datasource.runtime.DataSourceSupport;
import io.quarkus.reactive.datasource.runtime.ReactiveDataSourceUtil;
import io.quarkus.reactive.datasource.runtime.ReactiveDatasourceHealthCheck;
import io.quarkus.reactive.pg.client.runtime.PgPoolSupport;
import io.vertx.sqlclient.Pool;

@Readiness
@ApplicationScoped
class ReactivePgDataSourcesHealthCheck extends ReactiveDatasourceHealthCheck {

    public ReactivePgDataSourcesHealthCheck() {
        super("Reactive PostgreSQL connections health check", "SELECT 1");
    }

    @PostConstruct
    protected void init() {
        ArcContainer container = Arc.container();
        DataSourceSupport dataSourceSupport = container.instance(DataSourceSupport.class).get();
        Set<String> excludedNames = dataSourceSupport.getHealthCheckExcludedNames();
        PgPoolSupport pgPoolSupport = container.instance(PgPoolSupport.class).get();
        Set<String> pgPoolNames = pgPoolSupport.getPgPoolNames();
        for (InstanceHandle<Pool> handle : container.select(Pool.class, Any.Literal.INSTANCE).handles()) {
            if (!handle.getBean().isActive()) {
                continue;
            }
            String poolName = ReactiveDataSourceUtil.dataSourceName(handle.getBean());
            if (!pgPoolNames.contains(poolName) || excludedNames.contains(poolName)) {
                continue;
            }
            addPool(poolName, handle.get());
        }
    }

}
