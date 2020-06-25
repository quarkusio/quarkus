package io.quarkus.reactive.datasource.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.StampedLock;

import org.jboss.logging.Logger;

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

    private static final Logger log = Logger.getLogger(ThreadLocalPool.class);

    private final AtomicReference<ThreadLocalPoolSet> poolset = new AtomicReference<>(new ThreadLocalPoolSet());

    protected final PoolOptions poolOptions;
    protected final Vertx vertx;

    public ThreadLocalPool(Vertx vertx, PoolOptions poolOptions) {
        this.vertx = vertx;
        this.poolOptions = poolOptions;
    }

    private PoolType pool() {
        //We re-try to be nice on an extremely unlikely race condition.
        //3 attempts should be more than enough:
        //especially consider that if this race is triggered, then someone is trying to use the pool on shutdown,
        //which is inherently a broken plan.
        for (int i = 0; i < 3; i++) {
            final ThreadLocalPoolSet currentConnections = poolset.get();
            PoolType p = currentConnections.getPool();
            if (p != null)
                return p;
        }
        throw new IllegalStateException("Multiple attempts to reopen a new pool on a closed instance: aborting");
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

    /**
     * This is a bit weird because it works on all ThreadLocal pools, but it's only
     * called from a single thread, when doing shutdown, and needs to close all the
     * pools and reinitialise the thread local so that all newly created pools after
     * the restart will start with an empty thread local instead of a closed one.
     * N.B. while we take care of the pool to behave as best as we can,
     * it's responsibility of the user of the returned pools to not use them
     * while a close is being requested.
     */
    @Override
    public void close() {
        // close all the thread-local pools, then discard the current ThreadLocal pool.
        // Atomically set a new pool to be used: useful for live-reloading.
        final ThreadLocalPoolSet previousPool = poolset.getAndSet(new ThreadLocalPoolSet());
        previousPool.close();
    }

    private class ThreadLocalPoolSet {
        final List<Pool> threadLocalPools = new ArrayList<>();
        final ThreadLocal<PoolType> threadLocal = new ThreadLocal<>();
        final StampedLock stampedLock = new StampedLock();
        boolean isOpen = true;

        public PoolType getPool() {
            final long optimisticRead = stampedLock.tryOptimisticRead();
            if (isOpen == false) {
                //Let the caller re-try on a different instance
                return null;
            }
            PoolType ret = threadLocal.get();
            if (ret != null) {
                if (stampedLock.validate(optimisticRead)) {
                    return ret;
                } else {
                    //On invalid optimisticRead stamp, it means this pool instance was closed:
                    //let the caller re-try on a different instance
                    return null;
                }
            } else {
                //Now acquire an exclusive readlock:
                final long readLock = stampedLock.tryConvertToReadLock(optimisticRead);
                //Again, on failure the pool was closed, return null in such case.
                if (readLock == 0)
                    return null;
                //else, we own the exclusive read lock and can now enter our slow path:
                try {
                    log.debugf("Making pool for thread: %s", Thread.currentThread());
                    ret = createThreadLocalPool();
                    synchronized (threadLocalPools) {
                        threadLocalPools.add(ret);
                    }
                    threadLocal.set(ret);
                    return ret;
                } finally {
                    stampedLock.unlockRead(readLock);
                }
            }
        }

        public void close() {
            final long lock = stampedLock.writeLock();
            try {
                isOpen = false;
                //While this synchronized block might take a while as we have to close all
                //pool instances, it shouldn't block the getPool method as contention is
                //prevented by the exclusive stamped lock.
                synchronized (threadLocalPools) {
                    for (Pool pool : threadLocalPools) {
                        log.debugf("Closing pool: %s", pool);
                        pool.close();
                    }
                }
            } finally {
                stampedLock.unlockWrite(lock);
            }
        }
    }

}
