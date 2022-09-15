package io.quarkus.arc.test.lock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import io.quarkus.arc.Arc;
import io.quarkus.arc.Lock;
import io.quarkus.arc.Lock.Type;
import io.quarkus.arc.impl.LockInterceptor;
import io.quarkus.arc.test.ArcTestContainer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class LockInterceptorTest {

    private static int POOL_SIZE = 4;

    static ExecutorService executor;

    @BeforeAll
    static void initExecutor() {
        executor = Executors.newFixedThreadPool(POOL_SIZE);
    }

    @AfterAll
    static void shutdownExecutor() {
        executor.shutdownNow();
    }

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(SimpleSingletonBean.class, SimpleDependentBean.class,
            SimpleApplicationScopedBean.class, Lock.class,
            LockInterceptor.class);

    @Test
    public void testSingletonBean() throws Exception {
        assertConcurrentAccess(SimpleSingletonBean.class);
    }

    @Test
    public void testDependentBean() throws Exception {
        assertConcurrentAccess(SimpleDependentBean.class);
    }

    @Test
    public void testApplicationScopedBean() throws Exception {
        assertConcurrentAccess(SimpleApplicationScopedBean.class);
    }

    private <T extends Ping> void assertConcurrentAccess(Class<T> pingClass)
            throws InterruptedException, ExecutionException, TimeoutException {
        Ping bean = Arc.container().instance(pingClass).get();
        // Reset latches
        Ping.reset();
        int numberOfTasks = POOL_SIZE;
        List<Future<?>> results = new ArrayList<>();
        for (int i = 0; i < numberOfTasks; i++) {
            int idx = i;
            results.add(executor.submit(() -> {
                try {
                    bean.ping(idx);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                }
            }));
        }
        // Wait until the first method invocation starts
        assertTrue(Ping.FIRST_INSIDE_LATCH.await(5, TimeUnit.SECONDS));
        // At this time all tasks should be blocked
        assertEquals(0, Ping.COMPLETED.get());
        // Count down the completed latch and wait for results
        Ping.MAY_COMPLETE_LATCH.countDown();
        for (Future<?> future : results) {
            future.get(5, TimeUnit.SECONDS);
        }
        assertEquals(numberOfTasks, Ping.COMPLETED.get());
    }

    static abstract class Ping {

        static CountDownLatch FIRST_INSIDE_LATCH;
        static CountDownLatch MAY_COMPLETE_LATCH;
        static AtomicInteger COMPLETED;

        static void reset() {
            FIRST_INSIDE_LATCH = new CountDownLatch(1);
            MAY_COMPLETE_LATCH = new CountDownLatch(1);
            COMPLETED = new AtomicInteger();
        }

        void ping(int idx) throws InterruptedException {
            if (FIRST_INSIDE_LATCH.getCount() == 0 && COMPLETED.get() == 0) {
                fail("Locked method invocation not finished yet");
            }
            FIRST_INSIDE_LATCH.countDown();
            assertTrue(MAY_COMPLETE_LATCH.await(5, TimeUnit.SECONDS), MAY_COMPLETE_LATCH.toString());
            COMPLETED.incrementAndGet();
        }

    }

    @Lock
    @Singleton
    static class SimpleSingletonBean extends Ping {

    }

    @Dependent
    static class SimpleDependentBean extends Ping {

        @Lock
        void ping(int idx) throws InterruptedException {
            super.ping(idx);
        }

    }

    @Lock(Type.READ)
    @ApplicationScoped
    static class SimpleApplicationScopedBean extends Ping {

        @Lock(Type.WRITE)
        void ping(int idx) throws InterruptedException {
            super.ping(idx);
        }
    }
}
