package io.quarkus.reactive.datasource.runtime;

import java.util.concurrent.atomic.AtomicBoolean;

import io.vertx.core.Future;
import io.vertx.sqlclient.PrepareOptions;
import io.vertx.sqlclient.PreparedQuery;
import io.vertx.sqlclient.Query;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.SqlConnection;

class TestPool implements TestPoolInterface {

    private final AtomicBoolean isClosed = new AtomicBoolean(false);

    @Override
    public Query<RowSet<Row>> query(String s) {
        return null;
    }

    @Override
    public PreparedQuery<RowSet<Row>> preparedQuery(String s) {
        return null;
    }

    @Override
    public PreparedQuery<RowSet<Row>> preparedQuery(String sql, PrepareOptions options) {
        return null;
    }

    @Override
    public Future<SqlConnection> getConnection() {
        return Future.failedFuture("not implemented");
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public Future<Void> close() {
        isClosed.set(true);
        return Future.succeededFuture();
    }

    @Override
    public boolean isClosed() {
        return isClosed.get();
    }

}
