package io.quarkus.reactive.transaction.runtime.pool;

import static io.quarkus.reactive.transaction.runtime.TransactionalInterceptorBase.TRANSACTIONAL_METHOD_KEY;

import org.jboss.logging.Logger;

import io.smallrye.common.vertx.ContextLocals;
import io.smallrye.common.vertx.VertxContext;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PrepareOptions;
import io.vertx.sqlclient.PreparedQuery;
import io.vertx.sqlclient.Query;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.SqlConnection;

/**
 * A connection pool that handles transaction based on Vert.x context set by the @Transactional interceptor.
 */
public class TransactionalContextPool implements Pool {

    private static final Logger LOG = Logger.getLogger(TransactionalContextPool.class);

    // Key to store the connection holder for reuse by multiple sessions
    private static final String CURRENT_CONNECTION_KEY = "reactive.transaction.currentConnection";

    private final Pool delegate;

    public TransactionalContextPool(Pool delegate) {
        this.delegate = delegate;
    }

    @Override
    public Future<SqlConnection> getConnection() {
        if (!shouldOpenTransaction()) {
            return delegate.getConnection();
        } else {
            return getOrCreateConnectionHolder()
                    .getConnection()
                    .map(conn -> (SqlConnection) conn);
        }
    }

    /**
     * Gets or creates the ConnectionHolder for the current context.
     * The holder ensures only one connection is created even with concurrent access.
     */
    private ConnectionHolder getOrCreateConnectionHolder() {
        ConnectionHolder holder = ContextLocals.get(CURRENT_CONNECTION_KEY, null);
        if (holder == null) {
            holder = new ConnectionHolder(delegate);
            ContextLocals.put(CURRENT_CONNECTION_KEY, holder);
        }
        return holder;
    }

    public static Future<? extends SqlConnection> getCurrentConnectionFromVertxContext() {
        Context context = Vertx.currentContext();
        if (context == null) {
            return null;
        }
        ConnectionHolder holder = ContextLocals.get(CURRENT_CONNECTION_KEY, null);
        if (holder == null) {
            return null;
        }
        return holder.connectionPromise.future();
    }

    /**
     * Closes the current connection and clears it from the Vertx context.
     * This should be called by TransactionalInterceptorBase at the end of the transaction.
     *
     * @return a Future that completes when the connection is closed, or null if no connection exists
     */
    public static Future<Void> closeAndClearCurrentConnection() {
        Context context = Vertx.currentContext();
        if (context == null) {
            return null;
        }
        ConnectionHolder holder = ContextLocals.get(CURRENT_CONNECTION_KEY, null);
        if (holder == null) {
            return null;
        }
        // Wait for the connection to be available, then close it
        return holder.connectionPromise.future()
                .compose(wrappedConnection -> {
                    SqlConnection delegateConnection = wrappedConnection.getDelegate();
                    return delegateConnection.close();
                })
                .andThen(ar -> ContextLocals.remove(CURRENT_CONNECTION_KEY));
    }

    private boolean shouldOpenTransaction() {

        Context context = Vertx.currentContext();

        // Vert.x context during DB Validation in startup is null
        // When using reactive in a @Transactional method, the context is surely duplicated
        if (VertxContext.isOnDuplicatedContext()) {
            Object createTransaction = ContextLocals.get(TRANSACTIONAL_METHOD_KEY, null);
            return createTransaction != null && (boolean) createTransaction;
        } else {
            LOG.tracef("Vert.x context is either null or non duplicated, won't create a new transaction");
            return false;
        }
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
