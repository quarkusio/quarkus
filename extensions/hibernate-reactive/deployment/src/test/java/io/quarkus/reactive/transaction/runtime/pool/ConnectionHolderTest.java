package io.quarkus.reactive.transaction.runtime.pool;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PrepareOptions;
import io.vertx.sqlclient.PreparedQuery;
import io.vertx.sqlclient.PreparedStatement;
import io.vertx.sqlclient.Query;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.SqlConnection;
import io.vertx.sqlclient.Transaction;
import io.vertx.sqlclient.spi.DatabaseMetadata;

class ConnectionHolderTest {
    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void failedBeginReleasesTheCheckedOutConnection(boolean closeFails) {
        final DelayedPool pool = new DelayedPool();
        final ConnectionHolder holder = new ConnectionHolder(pool);
        final FailingBeginConnection connection = new FailingBeginConnection();
        final RuntimeException beginFailure = new RuntimeException("begin failed");
        final RuntimeException closeFailure = new RuntimeException("close failed");
        final Future<TransactionalContextConnection> acquisition = holder.getConnection();
        pool.pending.complete(connection);
        connection.beginResult.fail(beginFailure);

        assertThat(connection.closes).isEqualTo(1);
        assertThat(acquisition.isComplete()).as("failed acquisition waits for release").isFalse();
        if (closeFails) {
            connection.closeResult.fail(closeFailure);
        } else {
            connection.closeResult.complete();
        }
        assertThat(acquisition.cause()).isSameAs(beginFailure);
        assertThat(holder.getConnection()).isSameAs(acquisition);
        assertThat(connection.closes).isEqualTo(1);
        assertThat(beginFailure.getSuppressed())
                .containsExactly(closeFails ? new Throwable[] { closeFailure } : new Throwable[0]);
    }

    // A real checkout followed by a controlled failed BEGIN; unlike a failed checkout, this needs release.
    private static class FailingBeginConnection implements SqlConnection {
        final Promise<Transaction> beginResult = Promise.promise();
        final Promise<Void> closeResult = Promise.promise();
        int closes;

        @Override
        public Future<Transaction> begin() {
            return beginResult.future();
        }

        @Override
        public Future<Void> close() {
            closes++;
            return closeResult.future();
        }

        @Override
        public Future<PreparedStatement> prepare(String sql) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Future<PreparedStatement> prepare(String sql, PrepareOptions options) {
            throw new UnsupportedOperationException();
        }

        @Override
        public SqlConnection exceptionHandler(Handler<Throwable> handler) {
            throw new UnsupportedOperationException();
        }

        @Override
        public SqlConnection closeHandler(Handler<Void> handler) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Transaction transaction() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isSSL() {
            throw new UnsupportedOperationException();
        }

        @Override
        public DatabaseMetadata databaseMetadata() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Query<RowSet<Row>> query(String sql) {
            throw new UnsupportedOperationException();
        }

        @Override
        public PreparedQuery<RowSet<Row>> preparedQuery(String sql) {
            throw new UnsupportedOperationException();
        }

        @Override
        public PreparedQuery<RowSet<Row>> preparedQuery(String sql, PrepareOptions options) {
            throw new UnsupportedOperationException();
        }
    }

    // A controlled asynchronous acquisition source; the database behavior is covered by the Hibernate Reactive tests.
    private static class DelayedPool implements Pool {
        final Promise<Void> requested = Promise.promise();
        final Promise<SqlConnection> pending = Promise.promise();
        int requests;

        @Override
        public Future<SqlConnection> getConnection() {
            requests++;
            requested.tryComplete();
            return pending.future();
        }

        @Override
        public Query<RowSet<Row>> query(String sql) {
            throw new UnsupportedOperationException();
        }

        @Override
        public PreparedQuery<RowSet<Row>> preparedQuery(String sql) {
            throw new UnsupportedOperationException();
        }

        @Override
        public PreparedQuery<RowSet<Row>> preparedQuery(String sql, PrepareOptions options) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int size() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Future<Void> close() {
            return Future.succeededFuture();
        }
    }
}
