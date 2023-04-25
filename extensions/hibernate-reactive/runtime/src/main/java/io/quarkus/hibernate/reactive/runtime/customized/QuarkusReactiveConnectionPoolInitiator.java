package io.quarkus.hibernate.reactive.runtime.customized;

import java.util.Map;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.reactive.pool.ReactiveConnectionPool;
import org.hibernate.reactive.pool.impl.ExternalSqlClientPool;
import org.hibernate.service.spi.ServiceRegistryImplementor;

import io.quarkus.hibernate.orm.runtime.migration.MultiTenancyStrategy;
import io.vertx.sqlclient.Pool;

public final class QuarkusReactiveConnectionPoolInitiator
        implements StandardServiceInitiator<ReactiveConnectionPool> {

    private final Pool pool;

    public QuarkusReactiveConnectionPoolInitiator(Pool pool) {
        this.pool = pool;
    }

    @Override
    public Class<ReactiveConnectionPool> getServiceInitiated() {
        return ReactiveConnectionPool.class;
    }

    @Override
    public ReactiveConnectionPool initiateService(Map configurationValues, ServiceRegistryImplementor registry) {
        //First, check that this setup won't need to deal with multi-tenancy at the connection pool level:
        final MultiTenancyStrategy strategy = MultiTenancyStrategy.determineMultiTenancyStrategy(configurationValues);
        if (strategy == MultiTenancyStrategy.DATABASE || strategy == MultiTenancyStrategy.SCHEMA) {
            // nothing to do, but given the separate hierarchies have to handle this here.
            return null;
        }
        final JdbcServices jdbcService = registry.getService(JdbcServices.class);
        return new ExternalSqlClientPool(pool, jdbcService.getSqlStatementLogger(), jdbcService.getSqlExceptionHelper());
    }

}
