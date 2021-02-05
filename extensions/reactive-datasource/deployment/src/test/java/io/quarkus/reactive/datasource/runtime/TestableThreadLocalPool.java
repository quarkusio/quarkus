package io.quarkus.reactive.datasource.runtime;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.sqlclient.PreparedQuery;
import io.vertx.sqlclient.Query;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.SqlConnection;

final class TestableThreadLocalPool extends ThreadLocalPool<TestPoolInterface> {

    public TestableThreadLocalPool() {
        super(null, null);
    }

    @Override
    protected TestPoolInterface createThreadLocalPool() {
        return new TestPoolWrapper(new TestPool());
    }

    private class TestPoolWrapper implements TestPoolInterface {

        private final TestPool delegate;
        private boolean open = true;

        private TestPoolWrapper(TestPool delegate) {
            this.delegate = delegate;
        }

        @Override
        public void getConnection(Handler<AsyncResult<SqlConnection>> handler) {
            delegate.getConnection(handler);
        }

        @Override
        public Future<SqlConnection> getConnection() {
            Promise<SqlConnection> promise = Promise.promise();
            getConnection(promise);
            return promise.future();
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
            if (open) {
                delegate.close();
                TestableThreadLocalPool.this.removeSelfFromTracking(this);
            }
            handler.handle(Future.succeededFuture());
        }

        @Override
        public Future<Void> close() {
            if (open) {
                delegate.close();
                TestableThreadLocalPool.this.removeSelfFromTracking(this);
            }
            return Future.succeededFuture();
        }

        @Override
        public boolean isClosed() {
            return delegate.isClosed();
        }
    }

}
