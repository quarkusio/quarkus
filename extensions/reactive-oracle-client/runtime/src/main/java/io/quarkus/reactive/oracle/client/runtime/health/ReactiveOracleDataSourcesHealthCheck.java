package io.quarkus.reactive.oracle.client.runtime.health;

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
import io.quarkus.reactive.oracle.client.runtime.OraclePoolSupport;
import io.vertx.sqlclient.Pool;

@Readiness
@ApplicationScoped
class ReactiveOracleDataSourcesHealthCheck extends ReactiveDatasourceHealthCheck {

    public ReactiveOracleDataSourcesHealthCheck() {
        super("Reactive Oracle connections health check", "SELECT 1 FROM DUAL");
    }

    @PostConstruct
    protected void init() {
        ArcContainer container = Arc.container();
        DataSourceSupport dataSourceSupport = container.instance(DataSourceSupport.class).get();
        Set<String> excludedNames = dataSourceSupport.getHealthCheckExcludedNames();
        OraclePoolSupport oraclePoolSupport = container.instance(OraclePoolSupport.class).get();
        Set<String> oraclePoolNames = oraclePoolSupport.getOraclePoolNames();
        for (InstanceHandle<Pool> handle : container.select(Pool.class, Any.Literal.INSTANCE).handles()) {
            if (!handle.getBean().isActive()) {
                continue;
            }
            String poolName = ReactiveDataSourceUtil.dataSourceName(handle.getBean());
            if (!oraclePoolNames.contains(poolName) || excludedNames.contains(poolName)) {
                continue;
            }
            addPool(poolName, handle.get());
        }
    }

}
