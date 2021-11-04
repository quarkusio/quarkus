package io.quarkus.cache.test.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.cache.CacheResult;
import io.quarkus.test.QuarkusUnitTest;

public class ConcurrencyTest {

    private static final Object TIMEOUT_KEY = new Object();
    private static final Object NO_TIMEOUT_KEY = new Object();

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClass(CachedService.class));

    @Inject
    CachedService cachedService;

    @Test
    public void testConcurrentCacheAccessWithLockTimeout() throws InterruptedException, ExecutionException {
        // This is required to make sure the CompletableFuture from this test are executed concurrently.
        ExecutorService executorService = Executors.newFixedThreadPool(2);

        AtomicReference<String> callingThreadName1 = new AtomicReference<>();
        CompletableFuture<Object> future1 = CompletableFuture.supplyAsync(() -> {
            callingThreadName1.set(Thread.currentThread().getName());
            try {
                return cachedService.cachedMethodWithLockTimeout(TIMEOUT_KEY);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }, executorService);

        AtomicReference<String> callingThreadName2 = new AtomicReference<>();
        CompletableFuture<Object> future2 = CompletableFuture.supplyAsync(() -> {
            callingThreadName2.set(Thread.currentThread().getName());
            try {
                return cachedService.cachedMethodWithLockTimeout(TIMEOUT_KEY);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }, executorService);

        // Let's wait for both futures to complete before running assertions.
        CompletableFuture.allOf(future1, future2).get();

        /*
         * A cache lock timeout should be triggered during the second cached method call, which means that both calls should
         * result in an execution of the value loader function. Since the calls are done from different threads, the resulting
         * values (threads names) should be different.
         */
        assertNotEquals(future1.get(), future2.get());

        // All value loader executions should be done synchronously on the calling thread.
        assertEquals(callingThreadName1.get(), future1.get());
        assertEquals(callingThreadName2.get(), future2.get());
    }

    @Test
    public void testConcurrentCacheAccessWithoutLockTimeout() throws InterruptedException, ExecutionException {
        // This is required to make sure the CompletableFuture from this test are executed concurrently.
        ExecutorService executorService = Executors.newFixedThreadPool(2);

        AtomicReference<String> callingThreadName1 = new AtomicReference<>();
        CompletableFuture<String> future1 = CompletableFuture.supplyAsync(() -> {
            callingThreadName1.set(Thread.currentThread().getName());
            try {
                return cachedService.cachedMethodWithoutLockTimeout(NO_TIMEOUT_KEY);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }, executorService);

        AtomicReference<String> callingThreadName2 = new AtomicReference<>();
        CompletableFuture<String> future2 = CompletableFuture.supplyAsync(() -> {
            callingThreadName2.set(Thread.currentThread().getName());
            try {
                return cachedService.cachedMethodWithoutLockTimeout(NO_TIMEOUT_KEY);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }, executorService);

        // Let's wait for both futures to complete before running assertions.
        CompletableFuture.allOf(future1, future2).get();

        /*
         * This test should NOT trigger any cache lock timeout so there should only be one value loader execution and the
         * resulting values from both cached methods calls should be the same.
         */
        assertEquals(future1.get(), future2.get());

        /*
         * The value loader execution should be done synchronously on the calling thread.
         * Both thread names need to be checked because there's no way to determine which future will be run first.
         */
        assertTrue(callingThreadName1.get().equals(future1.get()) || callingThreadName2.get().equals(future1.get()));
    }

    @Singleton
    static class CachedService {

        private static final String CACHE_NAME = "test-cache";

        @CacheResult(cacheName = CACHE_NAME, lockTimeout = 500)
        public String cachedMethodWithLockTimeout(Object key) throws InterruptedException {
            // The following sleep is longer than the @CacheResult lockTimeout parameter value, a timeout will be triggered if
            // two concurrent calls are made at the same time.
            Thread.sleep(1000);
            return Thread.currentThread().getName();
        }

        @CacheResult(cacheName = CACHE_NAME, lockTimeout = 1000)
        public String cachedMethodWithoutLockTimeout(Object key) throws InterruptedException {
            // The following sleep is shorter than the @CacheResult lockTimeout parameter value, two concurrent calls made at
            // the same time won't trigger a timeout.
            Thread.sleep(500);
            return Thread.currentThread().getName();
        }
    }
}
