package io.quarkus.reactive.datasource.runtime;

import java.util.Map;
import java.util.Set;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;

import org.eclipse.microprofile.health.Readiness;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.datasource.runtime.DataSourceSupport;
import io.vertx.sqlclient.Pool;

@Readiness
@ApplicationScoped
class ReactiveDataSourcesHealthCheck extends ReactiveDatasourceHealthCheck {

    public ReactiveDataSourcesHealthCheck() {
        super("Reactive datasource connections health check", "SELECT 1");
    }

    @PostConstruct
    protected void init() {
        ArcContainer container = Arc.container();
        DataSourceSupport dataSourceSupport = container.instance(DataSourceSupport.class).get();
        Set<String> excludedNames = dataSourceSupport.getHealthCheckExcludedNames();
        ReactivePoolsHealthConfig healthConfig = container.instance(ReactivePoolsHealthConfig.class).get();
        Map<String, String> healthSqlMap = healthConfig.getHealthCheckSqlByDatasource();
        for (InstanceHandle<Pool> handle : container.select(Pool.class, Any.Literal.INSTANCE).handles()) {
            if (!handle.getBean().isActive()) {
                continue;
            }
            String poolName = ReactiveDataSourceUtil.dataSourceName(handle.getBean());
            if (excludedNames.contains(poolName) || !healthSqlMap.containsKey(poolName)) {
                continue;
            }
            addPool(poolName, handle.get(), healthSqlMap.get(poolName));
        }
    }
}
