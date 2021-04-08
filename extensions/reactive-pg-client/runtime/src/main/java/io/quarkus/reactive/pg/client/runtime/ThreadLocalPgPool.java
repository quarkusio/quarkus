package io.quarkus.reactive.pg.client.runtime;

import io.quarkus.reactive.datasource.runtime.ThreadLocalPool;
import io.vertx.core.*;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.*;

public class ThreadLocalPgPool extends ThreadLocalPool<PgPool> implements PgPool {

    private final PgConnectOptions pgConnectOptions;

    public ThreadLocalPgPool(Vertx vertx, PgConnectOptions pgConnectOptions, PoolOptions poolOptions) {
        super(vertx, poolOptions);
        this.pgConnectOptions = pgConnectOptions;
    }

    @Override
    protected PgPool createThreadLocalPool() {
        return new PgPoolWrapper(PgPool.pool(vertx, pgConnectOptions, poolOptions));
    }

    private class PgPoolWrapper implements PgPool {

        private final PgPool delegate;

        PgPoolWrapper(PgPool delegate) {
            this.delegate = delegate;
        }

        @Override
        public void getConnection(Handler<AsyncResult<SqlConnection>> handler) {
            delegate.getConnection(handler);
        }

        @Override
        public Future<SqlConnection> getConnection() {
            return delegate.getConnection();
        }

        @Override
        public Query<RowSet<Row>> query(String s) {
            return delegate.query(s);
        }

        @Override
        public PreparedQuery<RowSet<Row>> preparedQuery(String s) {
            return delegate.preparedQuery(s);
        }

        @Override
        public void close(Handler<AsyncResult<Void>> handler) {
            delegate.close()
                    .onComplete(x -> ThreadLocalPgPool.this.removeSelfFromTracking(this))
                    .onComplete(handler);
        }

        @Override
        public Future<Void> close() {
            Promise<Void> promise = Promise.promise();
            close(promise);
            return promise.future();
        }

    }
}
