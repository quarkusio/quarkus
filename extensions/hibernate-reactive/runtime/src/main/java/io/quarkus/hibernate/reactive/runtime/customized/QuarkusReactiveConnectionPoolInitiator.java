package io.quarkus.hibernate.reactive.runtime.customized;

import java.util.Map;
import java.util.concurrent.CompletionStage;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.jdbc.spi.SqlStatementLogger;
import org.hibernate.reactive.pool.ReactiveConnectionPool;
import org.hibernate.reactive.pool.impl.SqlClientPool;
import org.hibernate.reactive.util.impl.CompletionStages;
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
        SqlStatementLogger sqlStatementLogger = registry.getService(JdbcServices.class).getSqlStatementLogger();

        return new ExternalSqlClientPool(pool, sqlStatementLogger);
    }

    private static class ExternalSqlClientPool extends SqlClientPool {

        private final Pool pool;
        private final SqlStatementLogger sqlStatementLogger;

        public ExternalSqlClientPool(Pool pool, SqlStatementLogger sqlStatementLogger) {
            this.pool = pool;
            this.sqlStatementLogger = sqlStatementLogger;
        }

        @Override
        protected Pool getPool() {
            return pool;
        }

        @Override
        protected SqlStatementLogger getSqlStatementLogger() {
            return sqlStatementLogger;
        }

        /**
         * Since this Service implementation does not implement @{@link org.hibernate.service.spi.Stoppable}
         * and we're only adapting an externally provided pool, we will not actually close such provided pool
         * when Hibernate ORM is shutdown (it doesn't own the lifecycle of this external component).
         * Therefore, there is no need to wait for its shutdown and this method returns an already
         * successfully completed CompletionStage.
         *
         * @return
         */
        @Override
        public CompletionStage<Void> getCloseFuture() {
            return CompletionStages.voidFuture();
        }
    }
}
