package io.quarkus.cache.test.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.cache.CacheResult;
import io.quarkus.test.QuarkusUnitTest;

public class ThrowExecutionExceptionCauseTest {

    private static final String FORCED_EXCEPTION_MESSAGE = "Forced exception";

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class).addClass(CachedService.class));

    @Inject
    CachedService cachedService;

    @Test
    public void testRuntimeExceptionThrowDuringCacheComputation() {
        NumberFormatException e = assertThrows(NumberFormatException.class, () -> {
            cachedService.throwRuntimeExceptionDuringCacheComputation();
        });
        assertEquals(FORCED_EXCEPTION_MESSAGE, e.getMessage());
        // Let's check we didn't put an uncompleted future in the cache because of the previous exception.
        assertThrows(NumberFormatException.class, () -> {
            cachedService.throwRuntimeExceptionDuringCacheComputation();
        });
    }

    @Test
    public void testCheckedExceptionThrowDuringCacheComputation() {
        IOException e = assertThrows(IOException.class, () -> {
            cachedService.throwCheckedExceptionDuringCacheComputation();
        });
        assertEquals(FORCED_EXCEPTION_MESSAGE, e.getMessage());
    }

    @Test
    public void testRuntimeExceptionThrowDuringCacheComputationWithLockTimeout() {
        UnsupportedOperationException e = assertThrows(UnsupportedOperationException.class, () -> {
            cachedService.throwRuntimeExceptionDuringCacheComputationWithLockTimeout();
        });
        assertEquals(FORCED_EXCEPTION_MESSAGE, e.getMessage());
    }

    @Test
    public void testBothLockTimeoutCodeBranches() throws InterruptedException {
        /*
         * In this test, two CompletableFutures are executed concurrently. Each of them performs a call to a method annotated
         * with @CacheResult with the `lockTimeout` parameter set. The cached method always throws an
         * UnsupportedOperationException after a fixed delay. The combination of this delay and the timeout value guarantees
         * that only one CompletableFuture will invoke the cached method (and put the result into the cache) while the other
         * CompletableFuture will wait for the result coming from the cache. This is necessary to ensure both branches of the
         * lock timeout code are covered by tests.
         */

        // This is required to make sure the CompletableFuture from this test are executed concurrently.
        ExecutorService executorService = Executors.newFixedThreadPool(2);

        AtomicBoolean future1UnsupportedOperationException = new AtomicBoolean();
        CompletableFuture<Object> future1 = CompletableFuture
                .supplyAsync(() -> {
                    try {
                        return cachedService.throwRuntimeExceptionDuringCacheComputationWithLockTimeout();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }, executorService)
                .whenComplete((result, throwable) -> {
                    if (throwable instanceof CompletionException
                            && throwable.getCause() instanceof UnsupportedOperationException
                            && FORCED_EXCEPTION_MESSAGE.equals(throwable.getCause().getMessage())) {
                        future1UnsupportedOperationException.set(true);
                    }
                });

        AtomicBoolean future2UnsupportedOperationException = new AtomicBoolean();
        CompletableFuture<Object> future2 = CompletableFuture
                .supplyAsync(() -> {
                    try {
                        return cachedService.throwRuntimeExceptionDuringCacheComputationWithLockTimeout();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }, executorService)
                .whenComplete((result, throwable) -> {
                    if (throwable instanceof CompletionException
                            && throwable.getCause() instanceof UnsupportedOperationException
                            && FORCED_EXCEPTION_MESSAGE.equals(throwable.getCause().getMessage())) {
                        future2UnsupportedOperationException.set(true);
                    }
                });

        try {
            // Let's wait until both CompletableFutures are complete.
            CompletableFuture.allOf(future1, future2).get();
        } catch (ExecutionException e) {
            // This exception should always be thrown since both CompletableFutures should complete exceptionally.
            // We don't need to deal with it for this test though.
        }

        // This is where we make sure both branches of the lock timeout code were tested here.
        assertTrue(future1UnsupportedOperationException.get());
        assertTrue(future2UnsupportedOperationException.get());
    }

    @ApplicationScoped
    static class CachedService {

        @CacheResult(cacheName = "runtime-exception-cache")
        public String throwRuntimeExceptionDuringCacheComputation() {
            throw new NumberFormatException(FORCED_EXCEPTION_MESSAGE);
        }

        @CacheResult(cacheName = "checked-exception-cache")
        public String throwCheckedExceptionDuringCacheComputation() throws IOException {
            throw new IOException(FORCED_EXCEPTION_MESSAGE);
        }

        @CacheResult(cacheName = "lock-timeout-cache", lockTimeout = 1000)
        public Object throwRuntimeExceptionDuringCacheComputationWithLockTimeout() throws InterruptedException {
            Thread.sleep(500);
            throw new UnsupportedOperationException(FORCED_EXCEPTION_MESSAGE);
        }
    }
}
