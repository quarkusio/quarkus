package io.quarkus.reactive.mssql.client.runtime.health;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;

import org.eclipse.microprofile.health.Readiness;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
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
        for (InstanceHandle<MSSQLPool> handle : Arc.container().select(MSSQLPool.class, Any.Literal.INSTANCE).handles()) {
            addPool(getPoolName(handle.getBean()), handle.get());
        }
    }

}
