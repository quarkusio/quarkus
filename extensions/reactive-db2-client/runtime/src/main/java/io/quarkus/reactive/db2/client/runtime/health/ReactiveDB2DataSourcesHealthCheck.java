package io.quarkus.reactive.db2.client.runtime.health;

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
import io.quarkus.reactive.db2.client.runtime.DB2PoolSupport;
import io.vertx.sqlclient.Pool;

@Readiness
@ApplicationScoped
class ReactiveDB2DataSourcesHealthCheck extends ReactiveDatasourceHealthCheck {

    public ReactiveDB2DataSourcesHealthCheck() {
        super("Reactive DB2 connections health check", "SELECT 1 FROM SYSIBM.SYSDUMMY1");
    }

    @PostConstruct
    protected void init() {
        ArcContainer container = Arc.container();
        DataSourceSupport dataSourceSupport = container.instance(DataSourceSupport.class).get();
        Set<String> excludedNames = dataSourceSupport.getHealthCheckExcludedNames();
        DB2PoolSupport db2PoolSupport = container.instance(DB2PoolSupport.class).get();
        Set<String> db2PoolNames = db2PoolSupport.getDB2PoolNames();
        for (InstanceHandle<Pool> handle : container.select(Pool.class, Any.Literal.INSTANCE).handles()) {
            if (!handle.getBean().isActive()) {
                continue;
            }
            String poolName = ReactiveDataSourceUtil.dataSourceName(handle.getBean());
            if (!db2PoolNames.contains(poolName) || excludedNames.contains(poolName)) {
                continue;
            }
            addPool(poolName, handle.get());
        }
    }

}
