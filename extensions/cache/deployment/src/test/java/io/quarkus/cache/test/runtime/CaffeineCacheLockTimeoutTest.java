package io.quarkus.cache.test.runtime;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.cache.CacheResult;
import io.quarkus.test.QuarkusUnitTest;

public class CaffeineCacheLockTimeoutTest {

    private static final Object TIMEOUT_KEY = new Object();
    private static final Object NO_TIMEOUT_KEY = new Object();

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest().setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
            .addAsResource(new StringAsset("quarkus.cache.type=caffeine"), "application.properties")
            .addClass(CachedService.class));

    @Inject
    CachedService cachedService;

    @Test
    public void testConcurrentCacheAccessWithLockTimeout() throws InterruptedException, ExecutionException {
        // This is required to make sure the CompletableFuture from this test are executed concurrently.
        ExecutorService executorService = Executors.newFixedThreadPool(2);

        CompletableFuture<Object> future1 = CompletableFuture.supplyAsync(() -> {
            try {
                return cachedService.cachedMethodWithLockTimeout(TIMEOUT_KEY);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }, executorService);

        CompletableFuture<Object> future2 = CompletableFuture.supplyAsync(() -> {
            try {
                return cachedService.cachedMethodWithLockTimeout(TIMEOUT_KEY);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }, executorService);

        CompletableFuture.allOf(future1, future2).get();

        // The following assertion checks that the objects references resulting from both futures executions are different,
        // which means the method was invoked twice because the lock timeout was triggered.
        assertTrue(future1.get() != future2.get());
    }

    @Test
    public void testConcurrentCacheAccessWithoutLockTimeout() throws InterruptedException, ExecutionException {
        // This is required to make sure the CompletableFuture from this test are executed concurrently.
        ExecutorService executorService = Executors.newFixedThreadPool(2);

        CompletableFuture<Object> future1 = CompletableFuture.supplyAsync(() -> {
            try {
                return cachedService.cachedMethodWithoutLockTimeout(NO_TIMEOUT_KEY);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }, executorService);

        CompletableFuture<Object> future2 = CompletableFuture.supplyAsync(() -> {
            try {
                return cachedService.cachedMethodWithoutLockTimeout(NO_TIMEOUT_KEY);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }, executorService);

        CompletableFuture.allOf(future1, future2).get();

        // The following assertion checks that the objects references resulting from both futures executions are the same,
        // which means the method was invoked once and the lock timeout was NOT triggered.
        assertTrue(future1.get() == future2.get());
    }

    @Singleton
    static class CachedService {

        private static final String CACHE_NAME = "test-cache";

        @CacheResult(cacheName = CACHE_NAME, lockTimeout = 500)
        public Object cachedMethodWithLockTimeout(Object key) throws InterruptedException {
            // The following sleep is longer than the @CacheResult lockTimeout parameter value, a timeout will be triggered if
            // two concurrent calls are made at the same time.
            Thread.sleep(1000);
            return new Object();
        }

        @CacheResult(cacheName = CACHE_NAME, lockTimeout = 1000)
        public Object cachedMethodWithoutLockTimeout(Object key) throws InterruptedException {
            // The following sleep is shorter than the @CacheResult lockTimeout parameter value, two concurrent calls made at
            // the same time won't trigger a timeout.
            Thread.sleep(500);
            return new Object();
        }
    }
}
