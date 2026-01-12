package io.quarkus.hibernate.reactive.runtime.customized;

import static io.quarkus.reactive.transaction.TransactionalInterceptorBase.CURRENT_TRANSACTION_KEY;

import java.util.Optional;

import org.jboss.logging.Logger;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.sqlclient.PrepareOptions;
import io.vertx.sqlclient.PreparedQuery;
import io.vertx.sqlclient.PreparedStatement;
import io.vertx.sqlclient.Query;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.SqlConnection;
import io.vertx.sqlclient.Transaction;
import io.vertx.sqlclient.spi.DatabaseMetadata;

/**
 * This is a delegate for a simple SqlConnection that avoids closing the connection before
 * the transaction is committed. See Future<Void> close()
 **/
public class TransactionalContextConnection implements SqlConnection {

    private static final Logger LOG = Logger.getLogger(TransactionalContextConnection.class);

    private final SqlConnection connection;

    public TransactionalContextConnection(SqlConnection connection) {
        this.connection = connection;
    }

    @Fluent
    @Override
    public SqlConnection prepare(String sql, Handler<AsyncResult<PreparedStatement>> handler) {
        return connection.prepare(sql, handler);
    }

    @Override
    public Future<PreparedStatement> prepare(String sql) {
        return connection.prepare(sql);
    }

    @Fluent
    @Override
    public SqlConnection prepare(String sql, PrepareOptions options, Handler<AsyncResult<PreparedStatement>> handler) {
        return connection.prepare(sql, options, handler);
    }

    @Override
    public Future<PreparedStatement> prepare(String sql, PrepareOptions options) {
        return connection.prepare(sql, options);
    }

    @Fluent
    @Override
    public SqlConnection exceptionHandler(Handler<Throwable> handler) {
        return connection.exceptionHandler(handler);
    }

    @Fluent
    @Override
    public SqlConnection closeHandler(Handler<Void> handler) {
        return connection.closeHandler(handler);
    }

    @Override
    public void begin(Handler<AsyncResult<Transaction>> handler) {
        connection.begin(handler);
    }

    @Override
    public Future<Transaction> begin() {
        return connection.begin();
    }

    @Override
    public Transaction transaction() {
        return connection.transaction();
    }

    @Override
    public boolean isSSL() {
        return connection.isSSL();
    }

    @Override
    public void close(Handler<AsyncResult<Void>> handler) {
        connection.close(handler);
    }

    @Override
    public DatabaseMetadata databaseMetadata() {
        return connection.databaseMetadata();
    }

    @Override
    public Query<RowSet<Row>> query(String sql) {
        return connection.query(sql);
    }

    @Override
    public PreparedQuery<RowSet<Row>> preparedQuery(String sql) {
        return connection.preparedQuery(sql);
    }

    @Override
    public PreparedQuery<RowSet<Row>> preparedQuery(String sql, PrepareOptions options) {
        return connection.preparedQuery(sql, options);
    }

    @Override
    public Future<Void> close() {

        LOG.tracef("Calling close on TransactionalContextConnection");

        // Register closing this connection after TransactionalInterceptor commit the transaction
        Optional<Transaction> optTransaction = getTransactionFromVertxContext();
        optTransaction.ifPresent(t -> {
            LOG.tracef("Found a transaction %s in the context, registering the handling of closing", t);
            t.completion().onComplete(h -> {
                LOG.tracef("Found a %s transaction %s in the context, closing the connection %s", h.succeeded() ? "succeded" : "failed", t, connection);
                connection.close();
            });
        });

        // Do not close the connection here
        return Future.succeededFuture();
    }

    Optional<Transaction> getTransactionFromVertxContext() {
        return Optional.ofNullable(Vertx.currentContext().getLocal(CURRENT_TRANSACTION_KEY));
    }

}
