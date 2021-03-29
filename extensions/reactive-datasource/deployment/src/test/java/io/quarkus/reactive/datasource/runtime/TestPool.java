package io.quarkus.reactive.datasource.runtime;

import java.util.concurrent.atomic.AtomicBoolean;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.sqlclient.PreparedQuery;
import io.vertx.sqlclient.Query;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.SqlConnection;
import io.vertx.sqlclient.Transaction;

class TestPool implements TestPoolInterface {

    private final AtomicBoolean isClosed = new AtomicBoolean(false);

    @Override
    public void getConnection(Handler<AsyncResult<SqlConnection>> handler) {
    }

    @Override
    public Query<RowSet<Row>> query(String s) {
        return null;
    }

    @Override
    public PreparedQuery<RowSet<Row>> preparedQuery(String s) {
        return null;
    }

    @Override
    public void begin(Handler<AsyncResult<Transaction>> handler) {
    }

    @Override
    public void close() {
        isClosed.set(true);
    }

    @Override
    public boolean isClosed() {
        return isClosed.get();
    }
}
