package io.quarkus.reactive.datasource.runtime;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.PreparedQuery;
import io.vertx.sqlclient.Query;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.SqlConnection;
import io.vertx.sqlclient.Transaction;

public abstract class ThreadLocalPool<PoolType extends Pool> implements Pool {

    //List of all opened pools. Access requires synchronization on the list instance.
    private final List<PoolAndThread> allConnections = new ArrayList<>();

    //The pool instance for the current thread
    private final ThreadLocal<PoolType> threadLocal = new ThreadLocal<>();

    //Used by subclasses to create new pool instances
    protected final PoolOptions poolOptions;

    //Used by subclasses to create new pool instances
    protected final Vertx vertx;

    private volatile boolean closed = false;

    public ThreadLocalPool(Vertx vertx, PoolOptions poolOptions) {
        this.vertx = vertx;
        this.poolOptions = poolOptions;
    }

    PoolType pool() {
        checkPoolIsOpen();
        PoolType pool = threadLocal.get();
        if (pool == null) {
            synchronized (allConnections) {
                checkPoolIsOpen();
                pool = createThreadLocalPool();
                allConnections.add(new PoolAndThread(pool));
                threadLocal.set(pool);
                scanForAbandonedConnections();
            }
        }
        return pool;
    }

    private final void scanForAbandonedConnections() {
        for (PoolAndThread pair : allConnections) {
            if (pair.isDead()) {
                pair.close();
                allConnections.remove(pair);
            }
        }
    }

    private void checkPoolIsOpen() {
        if (closed) {
            throw new IllegalStateException("This Pool has been closed");
        }
    }

    protected abstract PoolType createThreadLocalPool();

    @Override
    public void getConnection(Handler<AsyncResult<SqlConnection>> handler) {
        pool().getConnection(handler);
    }

    @Override
    public Query<RowSet<Row>> query(String sql) {
        return pool().query(sql);
    }

    @Override
    public PreparedQuery<RowSet<Row>> preparedQuery(String sql) {
        return pool().preparedQuery(sql);
    }

    @Override
    public void begin(Handler<AsyncResult<Transaction>> handler) {
        pool().begin(handler);
    }

    @Override
    public void close() {
        synchronized (allConnections) {
            this.closed = true;
            for (PoolAndThread pair : allConnections) {
                pair.close();
            }
            allConnections.clear();
            threadLocal.remove();
        }
    }

    public int trackedSize() {
        synchronized (allConnections) {
            return allConnections.size();
        }
    }

    private static class PoolAndThread {
        private final Pool pool;
        private final WeakReference<Thread> threadReference;

        private PoolAndThread(Pool pool) {
            this.pool = pool;
            this.threadReference = new WeakReference<>(Thread.currentThread());
        }

        /**
         * @return true if this pools is associated to a Thread which is no longer alive.
         */
        boolean isDead() {
            final Thread thread = threadReference.get();
            return thread == null || (!thread.isAlive());
        }

        /**
         * Closes the connection
         */
        void close() {
            pool.close();
        }

    }

}
