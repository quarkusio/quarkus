package io.quarkus.reactive.transaction.runtime.pool;

import static io.quarkus.reactive.transaction.runtime.TransactionalInterceptorBase.TRANSACTIONAL_METHOD_KEY;

import java.util.function.Function;

import org.jboss.logging.Logger;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.impl.ContextInternal;
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
    public void getConnection(Handler<AsyncResult<SqlConnection>> handler) {
        if (!shouldOpenTransaction()) {
            delegate.getConnection(handler);
        } else {
            getOrCreateConnectionHolder()
                    .getConnection()
                    .map(conn -> (SqlConnection) conn)
                    .onComplete(handler);
        }
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
        Context context = Vertx.currentContext();
        ConnectionHolder holder = context.getLocal(CURRENT_CONNECTION_KEY);
        if (holder == null) {
            holder = new ConnectionHolder(delegate);
            context.putLocal(CURRENT_CONNECTION_KEY, holder);
        }
        return holder;
    }

    public static Future<? extends SqlConnection> getCurrentConnectionFromVertxContext() {
        Context context = Vertx.currentContext();
        if (context == null) {
            return null;
        }
        ConnectionHolder holder = context.getLocal(CURRENT_CONNECTION_KEY);
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
        ConnectionHolder holder = context.getLocal(CURRENT_CONNECTION_KEY);
        if (holder == null) {
            return null;
        }
        // Wait for the connection to be available, then close it
        return holder.connectionPromise.future()
                .compose(wrappedConnection -> {
                    SqlConnection delegateConnection = wrappedConnection.getDelegate();
                    return delegateConnection.close();
                })
                .andThen(ar -> context.removeLocal(CURRENT_CONNECTION_KEY));
    }

    private boolean shouldOpenTransaction() {

        Context context = Vertx.currentContext();

        // Vert.x context during DB Validation in startup is null
        // When using reactive in a @Transactional method, the context is surely duplicated
        if (context != null && ((ContextInternal) context).isDuplicate()) {
            Object createTransaction = context.getLocal(TRANSACTIONAL_METHOD_KEY);
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
    public void close(Handler<AsyncResult<Void>> handler) {
        delegate.close(handler);
    }

    @Override
    @Deprecated
    public Pool connectHandler(Handler<SqlConnection> handler) {
        // Deprecated, and not needed by Reactive, and no idea how it affects auto-transactions.
        throw new UnsupportedOperationException("This operation is not supported");
    }

    @Override
    @Deprecated
    public Pool connectionProvider(Function<Context, Future<SqlConnection>> provider) {
        // Deprecated, and not needed by Reactive, and no idea how it affects auto-transactions.
        throw new UnsupportedOperationException("This operation is not supported");
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
