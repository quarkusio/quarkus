package io.quarkus.reactive.datasource.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

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

    private final AtomicReference<ThreadLocal<PoolType>> pool = new AtomicReference<>(new ThreadLocal<>());
    private static final List<Pool> threadLocalPools = new ArrayList<>();

    protected final PoolOptions poolOptions;
    protected final Vertx vertx;

    public ThreadLocalPool(Vertx vertx, PoolOptions poolOptions) {
        this.vertx = vertx;
        this.poolOptions = poolOptions;
    }

    private PoolType pool() {
        ThreadLocal<PoolType> poolThreadLocal = pool.get();
        PoolType ret = poolThreadLocal.get();
        if (ret == null) {
            log.debugf("Making pool for thread: %s", Thread.currentThread());
            ret = createThreadLocalPool();
            synchronized (threadLocalPools) {
                threadLocalPools.add(ret);
            }
            poolThreadLocal.set(ret);
        }
        return ret;
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
     */
    @Override
    public void close() {
        // close all the thread-local pools
        synchronized (threadLocalPools) {
            for (Pool pool : threadLocalPools) {
                log.debugf("Closing pool: %s", pool);
                pool.close();
            }
            threadLocalPools.clear();
        }
        // discard the TL to clear them all
        pool.set(new ThreadLocal<PoolType>());
    }
}
