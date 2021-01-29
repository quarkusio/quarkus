package io.quarkus.reactive.db2.client.runtime;

import io.quarkus.reactive.datasource.runtime.ThreadLocalPool;
import io.vertx.core.*;
import io.vertx.db2client.DB2ConnectOptions;
import io.vertx.db2client.DB2Pool;
import io.vertx.sqlclient.*;

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

        DB2PoolWrapper(DB2Pool delegate) {
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
                    .onComplete(x -> ThreadLocalDB2Pool.this.removeSelfFromTracking(this))
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
