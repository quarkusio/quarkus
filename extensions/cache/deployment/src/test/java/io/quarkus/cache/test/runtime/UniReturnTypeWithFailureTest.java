package io.quarkus.cache.test.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.cache.CacheResult;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.mutiny.Uni;
import io.vertx.core.impl.NoStackTraceException;

public class UniReturnTypeWithFailureTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClass(CachedService.class).addClass(ConcurrentFailureService.class)
                    .addClass(FailureCachingService.class));

    @Inject
    CachedService cachedService;

    @Inject
    ConcurrentFailureService concurrentFailureService;

    @Inject
    FailureCachingService failureCachingService;

    @Test
    void testCacheResult() {
        assertThrows(NoStackTraceException.class, () -> cachedService.cacheResult("k1").await().indefinitely());
        assertEquals(1, cachedService.getCacheResultInvocations());
        assertEquals("", cachedService.cacheResult("k1").await().indefinitely());
        assertEquals(2, cachedService.getCacheResultInvocations());
        assertEquals("", cachedService.cacheResult("k1").await().indefinitely());
        assertEquals(2, cachedService.getCacheResultInvocations());
    }

    /**
     * Reproducer for #39677: Failures are cached instead of being retried.
     */
    @Test
    void testFailureCaching() {
        String key = "failure-test-key";
        failureCachingService.resetCounter(key);

        assertThrows(NoStackTraceException.class,
                () -> failureCachingService.getUsernameById(key).await().indefinitely());
        assertEquals(1, failureCachingService.getInvocations(key));

        assertThrows(NoStackTraceException.class,
                () -> failureCachingService.getUsernameById(key).await().indefinitely());

        assertEquals(2, failureCachingService.getInvocations(key),
                "Failures should not be cached - method should be invoked again (issue #39677)");
    }

    /**
     * Reproducer for #39677: Timeout failures are cached instead of being retried.
     */
    @Test
    void testTimeoutFailureCaching() {
        String key = "timeout-test-key";
        failureCachingService.resetCounter(key);

        assertThrows(io.smallrye.mutiny.TimeoutException.class,
                () -> failureCachingService.getUsernameByIdWithTimeout(key).await()
                        .atMost(Duration.ofMillis(100)));
        assertEquals(1, failureCachingService.getInvocations(key));

        assertThrows(io.smallrye.mutiny.TimeoutException.class,
                () -> failureCachingService.getUsernameByIdWithTimeout(key).await()
                        .atMost(Duration.ofMillis(100)));

        assertEquals(2, failureCachingService.getInvocations(key),
                "Timeout failures should not be cached - method should be invoked again (issue #39677)");
    }

    @Test
    void testSuccessfulCachingAfterFailure() {
        String key = "recovery-test-key";
        failureCachingService.resetCounter(key);

        assertThrows(NoStackTraceException.class,
                () -> failureCachingService.getUsernameByIdWithRecovery(key).await().indefinitely());
        assertEquals(1, failureCachingService.getInvocations(key));

        assertThrows(NoStackTraceException.class,
                () -> failureCachingService.getUsernameByIdWithRecovery(key).await().indefinitely());
        assertEquals(2, failureCachingService.getInvocations(key));

        String result = failureCachingService.getUsernameByIdWithRecovery(key).await().indefinitely();
        assertEquals("username-" + key, result);
        assertEquals(3, failureCachingService.getInvocations(key));

        String cachedResult = failureCachingService.getUsernameByIdWithRecovery(key).await().indefinitely();
        assertEquals("username-" + key, cachedResult);
        assertEquals(3, failureCachingService.getInvocations(key),
                "Successful result should be cached - method should not be invoked again");
    }

    /**
     * Reproducer for #51928: After a failed Uni, several concurrent invocations
     * may result in multiple method invocations instead of being synchronized.
     */
    @Test
    void testConcurrentAccessAndRecovery() throws InterruptedException {
        String key = "retry-resource-" + System.currentTimeMillis();
        int numberOfConcurrentThreads = 30;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfConcurrentThreads);
        CyclicBarrier startBarrier = new CyclicBarrier(numberOfConcurrentThreads + 1);

        concurrentFailureService.resetCounter(key);

        for (int i = 0; i < numberOfConcurrentThreads; i++) {
            executor.submit(() -> {
                try {
                    startBarrier.await();
                    try {
                        concurrentFailureService.getSingleArgData(key).await().indefinitely();
                    } catch (NoStackTraceException e) {
                        try {
                            concurrentFailureService.getSingleArgData(key).await().indefinitely();
                        } catch (Exception e2) {
                            // Ignore
                        }
                    }
                } catch (Exception e) {
                    // Ignore
                }
            });
        }

        try {
            startBarrier.await(5, TimeUnit.SECONDS);
        } catch (BrokenBarrierException | TimeoutException e) {
            // Ignore
        }
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        int invocations = concurrentFailureService.getInvocations(key);
        if (invocations > 2) {
            assertTrue(false,
                    String.format("Expected <= 2, but was %d for key %s. " +
                            "This reproduces issue #51928 - multiple concurrent invocations after " +
                            "failed Uni are not properly synchronized.",
                            invocations, key));
        }
    }

    @ApplicationScoped
    static class CachedService {

        private volatile int cacheResultInvocations;

        @CacheResult(cacheName = "test-cache")
        public Uni<String> cacheResult(String key) {
            cacheResultInvocations++;
            if (cacheResultInvocations == 1) {
                return Uni.createFrom().failure(new NoStackTraceException("dummy"));
            }
            return Uni.createFrom().item(() -> new String());
        }

        public int getCacheResultInvocations() {
            return cacheResultInvocations;
        }
    }

    @ApplicationScoped
    static class ConcurrentFailureService {

        private final Map<String, AtomicInteger> counters = new ConcurrentHashMap<>();

        public void resetCounter(String key) {
            counters.put(key, new AtomicInteger(0));
        }

        public int getInvocations(String key) {
            return counters.getOrDefault(key, new AtomicInteger(0)).get();
        }

        @CacheResult(cacheName = "concurrent-test-cache")
        public Uni<String> getSingleArgData(String key) {
            AtomicInteger counter = counters.computeIfAbsent(key, k -> new AtomicInteger(0));
            int invocationNumber = counter.incrementAndGet();

            if (invocationNumber == 1) {
                return Uni.createFrom().item("delay")
                        .onItem().delayIt().by(Duration.ofMillis(200))
                        .onItem().transformToUni(s -> Uni.createFrom().failure(new NoStackTraceException("First call fails")));
            } else {
                return Uni.createFrom().item("success-" + key);
            }
        }
    }

    @ApplicationScoped
    static class FailureCachingService {

        private final Map<String, AtomicInteger> counters = new ConcurrentHashMap<>();

        public void resetCounter(String key) {
            counters.put(key, new AtomicInteger(0));
        }

        public int getInvocations(String key) {
            return counters.getOrDefault(key, new AtomicInteger(0)).get();
        }

        @CacheResult(cacheName = "failure-cache")
        public Uni<String> getUsernameById(String userId) {
            AtomicInteger counter = counters.computeIfAbsent(userId, k -> new AtomicInteger(0));
            counter.incrementAndGet();
            return Uni.createFrom().failure(new NoStackTraceException("Error when getUsername"));
        }

        @CacheResult(cacheName = "timeout-failure-cache")
        public Uni<String> getUsernameByIdWithTimeout(String userId) {
            AtomicInteger counter = counters.computeIfAbsent(userId, k -> new AtomicInteger(0));
            counter.incrementAndGet();
            return Uni.createFrom().item("delayed")
                    .onItem().delayIt().by(Duration.ofMillis(500))
                    .onItem().transform(s -> "username-" + userId);
        }

        @CacheResult(cacheName = "failure-cache")
        public Uni<String> getUsernameByIdWithRecovery(String userId) {
            AtomicInteger counter = counters.computeIfAbsent(userId, k -> new AtomicInteger(0));
            int invocationNumber = counter.incrementAndGet();
            if (invocationNumber <= 2) {
                return Uni.createFrom().failure(new NoStackTraceException("Error when getUsername"));
            }
            return Uni.createFrom().item("username-" + userId);
        }
    }
}
