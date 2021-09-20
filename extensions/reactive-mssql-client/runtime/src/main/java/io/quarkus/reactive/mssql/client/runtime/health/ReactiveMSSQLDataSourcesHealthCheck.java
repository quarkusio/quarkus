package io.quarkus.reactive.mssql.client.runtime.health;

import java.util.Set;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;

import org.eclipse.microprofile.health.Readiness;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.datasource.runtime.DataSourcesExcludedFromHealthChecks;
import io.quarkus.reactive.datasource.runtime.ReactiveDatasourceHealthCheck;
import io.vertx.mssqlclient.MSSQLPool;

@Readiness
@ApplicationScoped
class ReactiveMSSQLDataSourcesHealthCheck extends ReactiveDatasourceHealthCheck {

    public ReactiveMSSQLDataSourcesHealthCheck() {
        super("Reactive MS SQL connections health check", "SELECT 1");
    }

    @PostConstruct
    protected void init() {
        ArcContainer container = Arc.container();
        DataSourcesExcludedFromHealthChecks excluded = container.instance(DataSourcesExcludedFromHealthChecks.class).get();
        Set<String> excludedNames = excluded.getExcludedNames();
        for (InstanceHandle<MSSQLPool> handle : container.select(MSSQLPool.class, Any.Literal.INSTANCE).handles()) {
            String poolName = getPoolName(handle.getBean());
            if (!excludedNames.contains(poolName)) {
                addPool(poolName, handle.get());
            }
        }
    }

}
