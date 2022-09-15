package io.quarkus.arc.test.lock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import io.quarkus.arc.Arc;
import io.quarkus.arc.Lock;
import io.quarkus.arc.LockException;
import io.quarkus.arc.impl.LockInterceptor;
import io.quarkus.arc.test.ArcTestContainer;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class LockWaitTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(SimpleApplicationScopedBean.class, Lock.class,
            LockInterceptor.class);

    @Test
    public void testLockWait() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(3);
        try {
            SimpleApplicationScopedBean bean = Arc.container().instance(SimpleApplicationScopedBean.class).get();

            // First invocation
            Future<?> firstResult = executor.submit(() -> {
                try {
                    bean.ping();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                }
            });
            // Wait until the first method invocation starts
            assertTrue(SimpleApplicationScopedBean.FIRST_INSIDE_LATCH.await(50, TimeUnit.SECONDS));

            // Second invocation - should be blocked and fail after 100ms
            Future<?> secondResult = executor.submit(() -> {
                try {
                    bean.ping();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                }
            });

            try {
                secondResult.get();
                fail();
            } catch (ExecutionException expected) {
                assertNotNull(expected.getCause());
                assertEquals(LockException.class, expected.getCause().getClass());
                assertTrue(expected.getCause().getMessage().contains("Write lock not acquired in"));
            }

            SimpleApplicationScopedBean.MAY_COMPLETE_LATCH.countDown();

            firstResult.get();
            assertEquals(1, SimpleApplicationScopedBean.COMPLETED.get());

        } finally {
            executor.shutdownNow();
        }

    }

    @ApplicationScoped
    static class SimpleApplicationScopedBean {

        static final CountDownLatch FIRST_INSIDE_LATCH = new CountDownLatch(1);
        static final CountDownLatch MAY_COMPLETE_LATCH = new CountDownLatch(1);
        static final AtomicInteger COMPLETED = new AtomicInteger();

        @Lock(time = 100)
        void ping() throws InterruptedException {
            if (FIRST_INSIDE_LATCH.getCount() == 0 && COMPLETED.get() == 0) {
                fail("Locked method invocation not finished yet");
            }
            FIRST_INSIDE_LATCH.countDown();
            assertTrue(MAY_COMPLETE_LATCH.await(50, TimeUnit.SECONDS), MAY_COMPLETE_LATCH.toString());
            COMPLETED.incrementAndGet();
        }
    }
}
