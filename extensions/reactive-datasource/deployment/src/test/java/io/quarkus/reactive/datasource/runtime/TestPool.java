package io.quarkus.reactive.datasource.runtime;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PreparedQuery;
import io.vertx.sqlclient.Query;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.SqlConnection;

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
    public Future<SqlConnection> getConnection() {
        Promise<SqlConnection> promise = Promise.promise();
        getConnection(promise);
        return promise.future();
    }

    @Override
    public void close(Handler<AsyncResult<Void>> handler) {
        isClosed.set(true);
        handler.handle(Future.succeededFuture());
    }

    @Override
    public Pool connectHandler(Handler<SqlConnection> handler) {
        return null;
    }

    @Override
    public Pool connectionProvider(Function<Context, Future<SqlConnection>> provider) {
        return null;
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
