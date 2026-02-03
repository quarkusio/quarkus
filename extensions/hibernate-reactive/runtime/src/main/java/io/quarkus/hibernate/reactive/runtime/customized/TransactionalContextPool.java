package io.quarkus.hibernate.reactive.runtime.customized;

import static io.quarkus.reactive.transaction.TransactionalInterceptorBase.CURRENT_TRANSACTION_KEY;
import static io.quarkus.reactive.transaction.TransactionalInterceptorBase.TRANSACTIONAL_METHOD_KEY;

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
import io.vertx.sqlclient.Transaction;

/**
 * A connection pool that handles transaction based on Vert.x context set by the @Transactional interceptor.
 */
public class TransactionalContextPool implements Pool {

    private static final Logger LOG = Logger.getLogger(TransactionalContextPool.class);

    private final Pool delegate;

    public TransactionalContextPool(Pool delegate) {
        this.delegate = delegate;
    }

    @Override
    public void getConnection(Handler<AsyncResult<SqlConnection>> handler) {
        if (!shouldOpenTransaction()) {
            delegate.getConnection(handler);
        } else {
            delegate.getConnection(result -> {
                if (result.failed()) {
                    handler.handle(result);
                    return;
                }
                var connection = result.result();
                connection.begin()
                        .map(transaction -> {
                            storeTransactionInVertxContext(transaction);
                            return (SqlConnection) new TransactionalContextConnection(connection);
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
            LOG.tracef("Getting a new connection");
            return delegate.getConnection()
                    .compose(connection -> {
                        LOG.tracef("New connection, about to start transaction: %s", connection);
                        return connection.begin().map(t -> {
                            Transaction transaction = connection.transaction();
                            storeTransactionInVertxContext(transaction);
                            return new TransactionalContextConnection(connection);
                        });
                    });
        }
    }

    private static void storeTransactionInVertxContext(Transaction transaction) {
        LOG.tracef("Transaction started: %s", transaction);
        Vertx.currentContext().putLocal(CURRENT_TRANSACTION_KEY, transaction);
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
