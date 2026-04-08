package io.quarkus.reactive.transaction.runtime.pool;

import static io.quarkus.reactive.transaction.runtime.TransactionalInterceptorBase.CURRENT_CONNECTION_KEY;
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

    // Key to store the wrapped connection for reuse by multiple sessions
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
            // Check if a connection already exists in the context (from a previous session in the same transaction)
            TransactionalContextConnection existingConnection = getCurrentConnectionFromVertxContext();
            if (existingConnection != null) {
                LOG.tracef("Reusing existing wrapped connection from context: %s", existingConnection);
                handler.handle(Future.succeededFuture(existingConnection));
                return;
            }
            delegate.getConnection(result -> {
                if (result.failed()) {
                    handler.handle(result);
                    return;
                }
                var connection = result.result();
                connection.begin()
                        .map(transaction -> {
                            TransactionalContextConnection wrappedConnection = new TransactionalContextConnection(connection);
                            storeConnectionInVertxContext(connection, wrappedConnection);
                            return (SqlConnection) wrappedConnection;
                        })
                        .andThen(handler);
            });
        }
    }

    @Override
    public Future<SqlConnection> getConnection() {
        if (!shouldOpenTransaction()) {
            return delegate.getConnection();
        } else {
            // Check if a connection already exists in the context (from a previous session in the same transaction)
            TransactionalContextConnection existingConnection = getCurrentConnectionFromVertxContext();
            if (existingConnection != null) {
                LOG.tracef("Reusing existing wrapped connection from context: %s", existingConnection);
                return Future.succeededFuture(existingConnection);
            }
            return delegate.getConnection()
                    .compose(connection -> {
                        LOG.tracef("New connection, about to start transaction: %s", connection);
                        return connection.begin().map(t -> {
                            LOG.tracef("Transaction started: %s", connection);
                            TransactionalContextConnection wrappedConnection = new TransactionalContextConnection(connection);
                            storeConnectionInVertxContext(connection, wrappedConnection);
                            return (SqlConnection) wrappedConnection;
                        });
                    });
        }
    }

    private static void storeConnectionInVertxContext(SqlConnection rawConnection,
            TransactionalContextConnection wrappedConnection) {
        Context context = Vertx.currentContext();
        // Store wrapped connection for reuse by other sessions and to retrieve delegate for closing
        context.putLocal(CURRENT_CONNECTION_KEY, wrappedConnection);
    }

    public static TransactionalContextConnection getCurrentConnectionFromVertxContext() {
        Context context = Vertx.currentContext();
        return context != null ? context.getLocal(CURRENT_CONNECTION_KEY) : null;
    }

    /**
     * Closes the current connection and clears it from the Vertx context.
     * This should be called by TransactionalInterceptorBase at the end of the transaction.
     *
     * @return a Future that completes when the connection is closed, or null if no connection exists
     */
    public static Future<Void> closeAndClearCurrentConnection() {
        TransactionalContextConnection wrappedConnection = getCurrentConnectionFromVertxContext();
        if (wrappedConnection == null) {
            return null;
        }
        SqlConnection delegateConnection = wrappedConnection.getDelegate();
        return delegateConnection.close().andThen(ar -> {
            Context context = Vertx.currentContext();
            if (context != null) {
                context.removeLocal(CURRENT_CONNECTION_KEY);
            }
        });
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
