package io.quarkus.reactive.oracle.client.runtime.health;

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
import io.vertx.oracleclient.OraclePool;

@Readiness
@ApplicationScoped
class ReactiveOracleDataSourcesHealthCheck extends ReactiveDatasourceHealthCheck {

    public ReactiveOracleDataSourcesHealthCheck() {
        super("Reactive Oracle connections health check", "SELECT 1 FROM DUAL");
    }

    @PostConstruct
    protected void init() {
        ArcContainer container = Arc.container();
        DataSourcesHealthSupport excluded = container.instance(DataSourcesHealthSupport.class).get();
        Set<String> excludedNames = excluded.getExcludedNames();
        for (InstanceHandle<OraclePool> handle : container.select(OraclePool.class, Any.Literal.INSTANCE).handles()) {
            String poolName = getPoolName(handle.getBean());
            if (!excludedNames.contains(poolName)) {
                addPool(poolName, handle.get());
            }
        }
    }

}
