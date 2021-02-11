package io.quarkus.reactive.pg.client.runtime.health;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;

import org.eclipse.microprofile.health.Readiness;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.reactive.datasource.runtime.ReactiveDatasourceHealthCheck;
import io.vertx.pgclient.PgPool;

@Readiness
@ApplicationScoped
class ReactivePgDataSourcesHealthCheck extends ReactiveDatasourceHealthCheck {

    public ReactivePgDataSourcesHealthCheck() {
        super("Reactive PostgreSQL connections health check", "SELECT 1");
    }

    @PostConstruct
    protected void init() {
        for (InstanceHandle<PgPool> handle : Arc.container().select(PgPool.class, Any.Literal.INSTANCE).handles()) {
            addPool(getPoolName(handle.getBean()), handle.get());
        }
    }

}
