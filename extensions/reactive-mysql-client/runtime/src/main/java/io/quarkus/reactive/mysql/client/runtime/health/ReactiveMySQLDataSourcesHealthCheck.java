package io.quarkus.reactive.mysql.client.runtime.health;

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
import io.vertx.mysqlclient.MySQLPool;

@Readiness
@ApplicationScoped
class ReactiveMySQLDataSourcesHealthCheck extends ReactiveDatasourceHealthCheck {

    public ReactiveMySQLDataSourcesHealthCheck() {
        super("Reactive MySQL connections health check", "SELECT 1");
    }

    @PostConstruct
    protected void init() {
        ArcContainer container = Arc.container();
        DataSourcesHealthSupport excluded = container.instance(DataSourcesHealthSupport.class).get();
        Set<String> excludedNames = excluded.getExcludedNames();
        for (InstanceHandle<MySQLPool> handle : container.select(MySQLPool.class, Any.Literal.INSTANCE).handles()) {
            String poolName = getPoolName(handle.getBean());
            if (!excludedNames.contains(poolName)) {
                addPool(poolName, handle.get());
            }
        }
    }

}
