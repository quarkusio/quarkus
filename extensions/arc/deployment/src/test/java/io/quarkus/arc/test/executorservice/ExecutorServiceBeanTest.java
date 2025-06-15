package io.quarkus.arc.test.executorservice;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.ClientProxy;
import io.quarkus.test.QuarkusUnitTest;

public class ExecutorServiceBeanTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest().withApplicationRoot(root -> {
    });

    @Inject
    ExecutorService executorService;

    @Inject
    Executor executor;

    @Inject
    ScheduledExecutorService scheduledExecutorService;

    @Test
    public void testExecutorServiceBean() throws InterruptedException {
        // unwrap the proxies first
        ExecutorService es = assertNonNullProxy(executorService);
        // all injection points should be satisifed by the same application scoped bean
        assertTrue(es == assertNonNullProxy(executor));
        assertTrue(es == assertNonNullProxy(scheduledExecutorService));
        // verify execution
        CountDownLatch latch = new CountDownLatch(3);
        executorService.submit(() -> latch.countDown());
        executor.execute(() -> latch.countDown());
        ScheduledFuture<?> sf = scheduledExecutorService.scheduleWithFixedDelay(() -> latch.countDown(), 0, 50,
                TimeUnit.MILLISECONDS);
        assertTrue(latch.await(3, TimeUnit.SECONDS));
        sf.cancel(true);
    }

    private <T> T assertNonNullProxy(T instance) {
        assertNotNull(instance);
        assertTrue(instance instanceof ClientProxy);
        return ClientProxy.unwrap(instance);
    }

}
