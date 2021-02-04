package io.quarkus.reactive.datasource.runtime;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;
import org.wildfly.common.Assert;

/**
 * Regression tests for some basic sanity semantics of ThreadLocalPool
 */
public class ConnectionPoolsClosedTest {

    /**
     * When a thread is terminated, we don't immediately clean up
     * any related Pool instances.
     * But we do look for abandoned connections when we scale up again,
     * so test for that specifically.
     * Connections will be closed more aggressively in practice as implementors
     * of ThreadLocalPool register a callback on close; so assuming clients
     * actually close it and threads aren't terminated abruptly this
     * should be unnecessary (still useful, since reality is often different).
     */
    @Test
    public void connectionsFromOtherThreadsGetClosed() throws ExecutionException, InterruptedException {
        TestableThreadLocalPool globalPool = new TestableThreadLocalPool();
        Assert.assertTrue(globalPool.trackedSize() == 0);
        final TestPoolInterface p1 = grabPoolFromOtherThread(globalPool);
        Assert.assertFalse(p1.isClosed());
        Assert.assertTrue(globalPool.trackedSize() == 1);
        final TestPoolInterface p2 = grabPoolFromOtherThread(globalPool);
        Assert.assertTrue(p1.isClosed());
        Assert.assertFalse(p2.isClosed());
        Assert.assertTrue(globalPool.trackedSize() == 1);
        globalPool.close();
        Assert.assertTrue(p2.isClosed());
        Assert.assertTrue(globalPool.trackedSize() == 0);
    }

    /**
     * This test makes sure that when the closed connection is not the last one in the list
     * of closed connections, no exception is thrown.
     */
    @Test
    public void connectionsThatAreNotTheLastGetClosedSuccessfully()
            throws ExecutionException, InterruptedException {
        ExecutorService e = Executors.newFixedThreadPool(3);
        Set<Thread> threads = new HashSet<>();
        try {
            TestableThreadLocalPool globalPool = new TestableThreadLocalPool();
            Assert.assertTrue(globalPool.trackedSize() == 0);
            final TestPoolInterface p1 = grabPoolFromLongLivingThread(globalPool, e, threads);
            Assert.assertFalse(p1.isClosed());
            Assert.assertTrue(globalPool.trackedSize() == 1);
            final TestPoolInterface p2 = grabPoolFromLongLivingThread(globalPool, e, threads);
            Assert.assertFalse(p1.isClosed());
            Assert.assertFalse(p2.isClosed());
            Assert.assertFalse(p1.isClosed());
            Assert.assertFalse(p2.isClosed());
            Assert.assertTrue(globalPool.trackedSize() == 2);
            e.shutdown();
            while (!e.isTerminated()) {
                Thread.sleep(1);
            }
            for (Thread thread : threads) {
                waitForThreadStop(thread);
            }
            final TestPoolInterface p3 = grabPoolFromOtherThread(globalPool);
            Assert.assertTrue(p1.isClosed());
            Assert.assertTrue(p2.isClosed());
            Assert.assertFalse(p3.isClosed());
            Assert.assertTrue(globalPool.trackedSize() == 1);
            globalPool.close();
            Assert.assertTrue(p1.isClosed());
            Assert.assertTrue(p2.isClosed());
            Assert.assertTrue(p3.isClosed());
            Assert.assertTrue(globalPool.trackedSize() == 0);
        } finally {
            e.shutdown();
        }
    }

    /**
     * Here we check that when explicit close of a thread-local
     * specific pool is closed, we also de-reference it.
     * And when closing the global pool, all thread-local
     * specific pools are closed and de-referenced.
     */
    @Test
    public void plainClose() {
        TestableThreadLocalPool globalPool = new TestableThreadLocalPool();
        Assert.assertTrue(globalPool.trackedSize() == 0);
        final TestPoolInterface p1 = globalPool.pool();
        Assert.assertTrue(globalPool.trackedSize() == 1);
        Assert.assertFalse(p1.isClosed());
        p1.close();
        Assert.assertTrue(p1.isClosed());
        Assert.assertTrue(globalPool.trackedSize() == 0);
        final TestPoolInterface p2 = globalPool.pool();
        Assert.assertTrue(globalPool.trackedSize() == 1);
        Assert.assertFalse(p2.isClosed());
        Assert.assertTrue(p1.isClosed());
        globalPool.close();
        Assert.assertTrue(p2.isClosed());
        Assert.assertTrue(globalPool.trackedSize() == 0);
    }

    private TestPoolInterface grabPoolFromOtherThread(TestableThreadLocalPool globalPool)
            throws ExecutionException, InterruptedException {
        final ExecutorService e = Executors.newSingleThreadExecutor();
        CompletableFuture<Thread> thread = new CompletableFuture<>();
        final Future<TestPoolInterface> creation = e.submit(() -> {
            thread.complete(Thread.currentThread());
            return globalPool.pool();
        });
        final TestPoolInterface poolInstance = creation.get();
        e.shutdown();
        waitForThreadStop(thread.get());
        return poolInstance;
    }

    private void waitForThreadStop(Thread thread) throws InterruptedException {
        while (thread.isAlive()) {
            //even after the pool is shutdown the thread may not actually report itself
            //as being dead yet, which can lead to race conditions
            //so we wait just to be sure
            Thread.sleep(1);
        }
    }

    private TestPoolInterface grabPoolFromLongLivingThread(TestableThreadLocalPool globalPool,
            ExecutorService e, Set<Thread> allThreads)
            throws InterruptedException, ExecutionException {
        CompletableFuture<Thread> thread = new CompletableFuture<>();
        final Future<TestPoolInterface> creation = e.submit(() -> {
            thread.complete(Thread.currentThread());
            return globalPool.pool();
        });
        allThreads.add(thread.get());
        return creation.get();
    }

}
