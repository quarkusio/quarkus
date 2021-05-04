package io.quarkus.reactive.datasource.runtime;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.PreparedQuery;
import io.vertx.sqlclient.Query;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.SqlConnection;

/**
 * This Pool implementation wraps the Vert.x Pool into thread-locals,
 * as it's otherwise not thread-safe to be exposed to all the threads
 * possibly accessing it in Quarkus.
 * There are two main drawbacks to this approach:
 * <p>
 * 1# We need to track each instance stored in a ThreadLocal
 * so to ensure we can close also the ones started in other threads.
 * </p>
 * <p>
 * 2# Having the actual number of Pools determined by the number
 * of threads requesting one makes this not honour the limit of
 * connections to the database.
 * </p>
 * <p>
 * In particular the second limitation will need to be addressed.
 *
 * @param <PoolType> useful for implementations to produce typed pools
 */
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
        ArrayList<PoolAndThread> garbage = new ArrayList<>();
        for (PoolAndThread pair : allConnections) {
            if (pair.isDead()) {
                garbage.add(pair);
            }
        }
        //This needs a second loop, as the close() operation
        //will otherwise trigger a concurrent modification on the iterator.
        for (PoolAndThread dead : garbage) {
            //This might potentially close the connection a second time,
            //so we need to ensure implementations allow it.
            dead.close();
        }
    }

    private void checkPoolIsOpen() {
        if (closed) {
            throw new IllegalStateException("This Pool has been closed");
        }
    }

    /**
     * We will need the created Pool instances to have an idempotent implementation
     * of close()
     */
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
    public Future<SqlConnection> getConnection() {
        return pool().getConnection();
    }

    @Override
    public void close(Handler<AsyncResult<Void>> handler) {
        handler.handle(close());
    }

    @Override
    public Future<Void> close() {
        synchronized (allConnections) {
            this.closed = true;
            ArrayList<CompletableFuture<Void>> tasks = new ArrayList<>(allConnections.size());
            for (PoolAndThread pair : allConnections) {
                tasks.add(pair.close().toCompletionStage().toCompletableFuture());
            }
            final CompletableFuture<Void> combinedCloseTask = CompletableFuture.allOf(tasks.toArray(new CompletableFuture[0]));
            allConnections.clear();
            threadLocal.remove();
            return Future.fromCompletionStage(combinedCloseTask);
        }
    }

    //Useful for testing mostly
    public int trackedSize() {
        synchronized (allConnections) {
            return allConnections.size();
        }
    }

    /**
     * Removes references to the instance without closing it.
     * This assumes the instance was created via this pool
     * and that it's now closed, so no longer needing tracking.
     *
     * @param instance
     */
    protected void removeSelfFromTracking(final PoolType instance) {
        synchronized (allConnections) {
            if (closed) {
                return;
            }
            for (PoolAndThread pair : allConnections) {
                if (pair.pool == instance) {
                    allConnections.remove(pair);
                    if (pair.isCurrentThread()) {
                        threadLocal.remove();
                    }
                    return;
                }
            }
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
         * 
         * @return
         */
        Future<Void> close() {
            return pool.close();
        }

        /**
         * @return if this is the pair referring to the current Thread
         */
        public boolean isCurrentThread() {
            return threadReference.get() == Thread.currentThread();
        }
    }

}
