package io.quarkus.hibernate.reactive.runtime.customized;

import static io.quarkus.hibernate.reactive.runtime.HibernateReactiveRecorder.TRANSACTIONAL_METHOD_KEY;

import java.util.function.Function;

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
 * A pool that handles transaction based on Vert.x context set by the @Transactional interceptor.
 */
public class TransactionalContextPool implements Pool {

    // Used in this class and in TransactionalInterceptor to get the lazily created Transaction
    public static final String CURRENT_TRANSACTION_KEY = "hibernate.reactive.currentTransaction";

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
                }
                var connection = result.result();
                connection.begin()
                        // Ignore the returned transaction; the caller expects a SqlConnection,
                        // and the Transaction can be accessed through connection.transaction() anyway.
                        .map(ignored -> connection)
                        .andThen(handler);
            });
        }
    }

    @Override
    public Future<SqlConnection> getConnection() {
        if (!shouldOpenTransaction()) {
            return delegate.getConnection();
        } else {
            return delegate.getConnection()
                    .compose(connection -> {
                        return connection.begin().map(t -> {
                            Vertx.currentContext().putLocal(CURRENT_TRANSACTION_KEY, connection.transaction());
                            return new TransactionalContextConnection(connection);
                        });
                    });
        }
    }

    private boolean shouldOpenTransaction() {

        Context context = Vertx.currentContext();

        // Vert.x context during DB Validation in startup is null
        // When using reactive in a @Transactional method, the context is surely duplicated
        if (context != null && ((ContextInternal) context).isDuplicate()) {
            Object createTransaction = context.getLocal(TRANSACTIONAL_METHOD_KEY);
            return createTransaction != null && (boolean) createTransaction;
        } else {
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
