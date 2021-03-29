package io.quarkus.reactive.db2.client.runtime;

import io.quarkus.reactive.datasource.runtime.ThreadLocalPool;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.db2client.DB2ConnectOptions;
import io.vertx.db2client.DB2Pool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.PreparedQuery;
import io.vertx.sqlclient.Query;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.SqlConnection;
import io.vertx.sqlclient.Transaction;

public class ThreadLocalDB2Pool extends ThreadLocalPool<DB2Pool> implements DB2Pool {

    private final DB2ConnectOptions db2ConnectOptions;

    public ThreadLocalDB2Pool(Vertx vertx, DB2ConnectOptions db2ConnectOptions, PoolOptions poolOptions) {
        super(vertx, poolOptions);
        this.db2ConnectOptions = db2ConnectOptions;
    }

    @Override
    protected DB2Pool createThreadLocalPool() {
        return new DB2PoolWrapper(DB2Pool.pool(vertx, db2ConnectOptions, poolOptions));
    }

    private class DB2PoolWrapper implements DB2Pool {

        private final DB2Pool delegate;
        private boolean open = true;

        private DB2PoolWrapper(DB2Pool delegate) {
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
                ThreadLocalDB2Pool.this.removeSelfFromTracking(this);
            }
        }
    }
}
