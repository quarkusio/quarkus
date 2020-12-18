package io.quarkus.reactive.mysql.client.runtime.health;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;

import org.eclipse.microprofile.health.Readiness;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
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
        for (InstanceHandle<MySQLPool> handle : Arc.container().select(MySQLPool.class, Any.Literal.INSTANCE).handles()) {
            addPool(getPoolName(handle.getBean()), handle.get());
        }
    }

}
