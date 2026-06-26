package io.quarkus.reactive.transaction.runtime.pool;

import org.jboss.logging.Logger;

import io.quarkus.reactive.transaction.runtime.ReactiveTransactionManager;
import io.quarkus.reactive.transaction.runtime.ReactiveTransactionResource;
import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PrepareOptions;
import io.vertx.sqlclient.PreparedQuery;
import io.vertx.sqlclient.Query;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.SqlConnection;

/**
 * A connection pool that handles transactions based on the {@link ReactiveTransactionManager}.
 * <p>
 * When a reactive transaction is active (via {@code @Transactional}), {@link #getConnection()} returns
 * a transaction-scoped connection. Otherwise, it delegates to the raw pool.
 * <p>
 * {@link #withTransaction} always delegates to the raw pool, so manual transactions work
 * both inside and outside {@code @Transactional} scope.
 */
public class TransactionalContextPool implements Pool {

    private static final Logger LOG = Logger.getLogger(TransactionalContextPool.class);

    private final Pool delegate;
    private final ReactiveTransactionManager txManager;

    public TransactionalContextPool(Pool delegate, ReactiveTransactionManager txManager) {
        this.delegate = delegate;
        this.txManager = txManager;
    }

    @Override
    public Future<SqlConnection> getConnection() {
        if (!txManager.isActive()) {
            return delegate.getConnection();
        }

        ReactiveTransactionResource resource = txManager.getResource();
        ConnectionHolder holder;
        if (resource instanceof ConnectionHolder) {
            holder = (ConnectionHolder) resource;
        } else if (resource == null) {
            holder = new ConnectionHolder(delegate);
            txManager.enlistResource(holder);
        } else {
            // A different resource is already enlisted (different datasource)
            throw new IllegalStateException(
                    "Cannot enlist multiple resources in a reactive transaction (XA not supported). "
                            + "Use a single datasource per @Transactional method, "
                            + "or use pool.withTransaction() for manual transaction management.");
        }

        return holder.getConnection().map(conn -> (SqlConnection) conn);
    }

    @Override
    public Query<RowSet<Row>> query(String sql) {
        return delegate.query(sql);
    }

    @Override
    public PreparedQuery<RowSet<Row>> preparedQuery(String sql) {
        return delegate.preparedQuery(sql);
    }

    @Override
    public int size() {
        return delegate.size();
    }

    @Override
    public PreparedQuery<RowSet<Row>> preparedQuery(String sql, PrepareOptions options) {
        return delegate.preparedQuery(sql, options);
    }

    @Override
    public Future<Void> close() {
        return delegate.close();
    }
}
