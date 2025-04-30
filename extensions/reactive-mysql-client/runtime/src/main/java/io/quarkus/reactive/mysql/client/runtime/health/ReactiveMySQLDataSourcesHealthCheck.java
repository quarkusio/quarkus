package io.quarkus.reactive.mysql.client.runtime.health;

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
import io.quarkus.reactive.mysql.client.runtime.MySQLPoolSupport;
import io.vertx.sqlclient.Pool;

@Readiness
@ApplicationScoped
class ReactiveMySQLDataSourcesHealthCheck extends ReactiveDatasourceHealthCheck {

    public ReactiveMySQLDataSourcesHealthCheck() {
        super("Reactive MySQL connections health check", "SELECT 1");
    }

    @PostConstruct
    protected void init() {
        ArcContainer container = Arc.container();
        DataSourceSupport dataSourceSupport = container.instance(DataSourceSupport.class).get();
        Set<String> excludedNames = dataSourceSupport.getHealthCheckExcludedNames();
        MySQLPoolSupport mySQLPoolSupport = container.instance(MySQLPoolSupport.class).get();
        Set<String> mySQLPoolNames = mySQLPoolSupport.getMySQLPoolNames();
        for (InstanceHandle<Pool> handle : container.select(Pool.class, Any.Literal.INSTANCE).handles()) {
            if (!handle.getBean().isActive()) {
                continue;
            }
            String poolName = ReactiveDataSourceUtil.dataSourceName(handle.getBean());
            if (!mySQLPoolNames.contains(poolName) || excludedNames.contains(poolName)) {
                continue;
            }
            addPool(poolName, handle.get());
        }
    }

}
