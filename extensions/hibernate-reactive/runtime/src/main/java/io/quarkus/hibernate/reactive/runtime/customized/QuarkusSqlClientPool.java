package io.quarkus.hibernate.reactive.runtime.customized;

import java.util.concurrent.CompletionStage;

import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.jdbc.spi.SqlExceptionHelper;
import org.hibernate.engine.jdbc.spi.SqlStatementLogger;
import org.hibernate.reactive.pool.impl.SqlClientPool;
import org.hibernate.reactive.util.impl.CompletionStages;
import org.hibernate.service.spi.ServiceRegistryAwareService;
import org.hibernate.service.spi.ServiceRegistryImplementor;

import io.vertx.sqlclient.Pool;

/**
 * An alternative implementation of {@link org.hibernate.reactive.pool.impl.ExternalSqlClientPool}
 * which retrieves SQL loggers / exception handlers lazily,
 * to avoid a circular dependency JdbcEnvironment => pool => JdbcServices => JdbcEnvironment.
 */
public class QuarkusSqlClientPool extends SqlClientPool
        implements ServiceRegistryAwareService {

    private final Pool pool;
    private SqlStatementLogger sqlStatementLogger;
    private SqlExceptionHelper sqlExceptionHelper;
    private ServiceRegistryImplementor serviceRegistry;

    public QuarkusSqlClientPool(Pool pool) {
        this.pool = pool;
    }

    @Override
    public void injectServices(ServiceRegistryImplementor serviceRegistry) {
        this.serviceRegistry = serviceRegistry;
        this.sqlStatementLogger = serviceRegistry.getService(SqlStatementLogger.class);
    }

    @Override
    protected Pool getPool() {
        return pool;
    }

    @Override
    protected SqlStatementLogger getSqlStatementLogger() {
        return sqlStatementLogger;
    }

    @Override
    public SqlExceptionHelper getSqlExceptionHelper() {
        if (sqlExceptionHelper == null) {
            sqlExceptionHelper = serviceRegistry
                    .getService(JdbcServices.class).getSqlExceptionHelper();
        }
        return sqlExceptionHelper;
    }

    @Override
    public CompletionStage<Void> getCloseFuture() {
        // Closing is handled by Quarkus.
        return CompletionStages.voidFuture();
    }
}
