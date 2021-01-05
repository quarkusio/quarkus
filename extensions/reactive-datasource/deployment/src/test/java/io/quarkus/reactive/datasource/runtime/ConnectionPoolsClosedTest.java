package io.quarkus.reactive.datasource.runtime;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.wildfly.common.Assert;

public class ConnectionPoolsClosedTest {

    @Test
    public void connectionsGetClosed() throws ExecutionException, InterruptedException {
        /**
         * When a thread is terminated, we don't immediately clean up
         * any related Pool instances.
         * But we do look for abandoned connections when we scale up again,
         * so test for that specifically.
         */
        TestableThreadLocalPool globalPool = new TestableThreadLocalPool();
        Assert.assertTrue(globalPool.trackedSize() == 0);
        final TestPool p1 = grabPoolFromOtherThread(globalPool);
        Assert.assertFalse(p1.isClosed());
        Assert.assertTrue(globalPool.trackedSize() == 1);
        final TestPool p2 = grabPoolFromOtherThread(globalPool);
        Assert.assertTrue(p1.isClosed());
        Assert.assertFalse(p2.isClosed());
        Assert.assertTrue(globalPool.trackedSize() == 1);
        globalPool.close();
        Assert.assertTrue(p2.isClosed());
        Assert.assertTrue(globalPool.trackedSize() == 0);
    }

    private TestPool grabPoolFromOtherThread(TestableThreadLocalPool globalPool)
            throws ExecutionException, InterruptedException {
        final ExecutorService e = Executors.newSingleThreadExecutor();
        final Future<TestPool> creation = e.submit(() -> globalPool.pool());
        final TestPool poolInstance = creation.get();
        e.shutdownNow();
        e.awaitTermination(1, TimeUnit.MILLISECONDS);
        return poolInstance;
    }

}
