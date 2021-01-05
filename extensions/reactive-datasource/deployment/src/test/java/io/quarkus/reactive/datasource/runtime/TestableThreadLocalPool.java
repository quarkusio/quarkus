package io.quarkus.reactive.datasource.runtime;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.sqlclient.PreparedQuery;
import io.vertx.sqlclient.Query;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.SqlConnection;
import io.vertx.sqlclient.Transaction;

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
                TestableThreadLocalPool.this.removeSelfFromTracking(this);
            }
        }

        @Override
        public boolean isClosed() {
            return delegate.isClosed();
        }
    }

}
