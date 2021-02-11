package io.quarkus.hibernate.reactive.runtime.customized;

import java.util.Map;

import org.hibernate.MultiTenancyStrategy;
import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.dialect.PostgreSQL9Dialect;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.jdbc.spi.SqlStatementLogger;
import org.hibernate.reactive.pool.ReactiveConnectionPool;
import org.hibernate.reactive.pool.impl.ExternalSqlClientPool;
import org.hibernate.service.spi.ServiceRegistryImplementor;

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
        boolean usePostgresStyleParameters = registry.getService(JdbcEnvironment.class)
                .getDialect() instanceof PostgreSQL9Dialect;

        SqlStatementLogger sqlStatementLogger = registry.getService(JdbcServices.class).getSqlStatementLogger();

        return new ExternalSqlClientPool(pool, sqlStatementLogger, usePostgresStyleParameters);
    }

}
