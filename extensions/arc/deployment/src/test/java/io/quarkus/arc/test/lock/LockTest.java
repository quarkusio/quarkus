package io.quarkus.arc.test.lock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Lock;
import io.quarkus.test.QuarkusUnitTest;

public class LockTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(SimpleBean.class));

    @Inject
    SimpleBean bean;

    @Test
    public void testLock() throws InterruptedException, ExecutionException {
        int count = 2;
        ExecutorService executor = Executors.newFixedThreadPool(count);
        try {
            // Submit the tasks
            List<Future<?>> results = new ArrayList<>();
            for (int i = 0; i < count; i++) {
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
            // Wait until the first method invocation is locked 
            assertTrue(SimpleBean.FIRST_INSIDE_LATCH.await(5, TimeUnit.SECONDS));
            // Verify that no invocation was completed but one started
            assertEquals(0, SimpleBean.COMPLETED.get());
            // Count down the "completed" latch -> finish invocation of the first
            SimpleBean.MAY_COMPLETE_LATCH.countDown();
            // Wait until all tasks are complete
            for (Future<?> future : results) {
                future.get();
            }
            assertEquals(2, SimpleBean.COMPLETED.get());
        } finally {
            executor.shutdownNow();
        }
    }

    @ApplicationScoped
    static class SimpleBean {

        static final CountDownLatch FIRST_INSIDE_LATCH = new CountDownLatch(1);
        static final CountDownLatch MAY_COMPLETE_LATCH = new CountDownLatch(1);
        static final AtomicInteger COMPLETED = new AtomicInteger();

        @Lock
        public void ping(int idx) throws InterruptedException {
            if (FIRST_INSIDE_LATCH.getCount() == 0 && COMPLETED.get() == 0) {
                fail("Locked method invocation not finished yet");
            }
            FIRST_INSIDE_LATCH.countDown();
            assertTrue(MAY_COMPLETE_LATCH.await(3, TimeUnit.SECONDS), idx + ":" + MAY_COMPLETE_LATCH.toString());
            COMPLETED.incrementAndGet();
        }

    }

}
