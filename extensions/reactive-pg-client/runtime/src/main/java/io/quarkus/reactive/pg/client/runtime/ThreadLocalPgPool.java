package io.quarkus.reactive.pg.client.runtime;

import io.quarkus.reactive.datasource.runtime.ThreadLocalPool;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.PreparedQuery;
import io.vertx.sqlclient.Query;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.SqlConnection;
import io.vertx.sqlclient.Transaction;

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
        private boolean open = true;

        private PgPoolWrapper(PgPool delegate) {
            this.delegate = delegate;
        }

        @Override
        public void getConnection(Handler<AsyncResult<SqlConnection>> handler) {
            delegate.getConnection(handler);
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
        public void begin(Handler<AsyncResult<Transaction>> handler) {
            delegate.begin(handler);
        }

        @Override
        public void close() {
            if (open) {
                delegate.close();
                ThreadLocalPgPool.this.removeSelfFromTracking(this);
            }
        }
    }
}
