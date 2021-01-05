package io.quarkus.reactive.mysql.client.runtime;

import io.quarkus.reactive.datasource.runtime.ThreadLocalPool;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.mysqlclient.MySQLConnectOptions;
import io.vertx.mysqlclient.MySQLPool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.PreparedQuery;
import io.vertx.sqlclient.Query;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.SqlConnection;
import io.vertx.sqlclient.Transaction;

public class ThreadLocalMySQLPool extends ThreadLocalPool<MySQLPool> implements MySQLPool {

    private final MySQLConnectOptions mySQLConnectOptions;

    public ThreadLocalMySQLPool(Vertx vertx, MySQLConnectOptions mySQLConnectOptions, PoolOptions poolOptions) {
        super(vertx, poolOptions);
        this.mySQLConnectOptions = mySQLConnectOptions;
    }

    @Override
    protected MySQLPool createThreadLocalPool() {
        return new MySQLPoolWrapper(MySQLPool.pool(vertx, mySQLConnectOptions, poolOptions));
    }

    private class MySQLPoolWrapper implements MySQLPool {

        private final MySQLPool delegate;
        private boolean open = true;

        private MySQLPoolWrapper(MySQLPool delegate) {
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
                ThreadLocalMySQLPool.this.removeSelfFromTracking(this);
            }
        }
    }

}
