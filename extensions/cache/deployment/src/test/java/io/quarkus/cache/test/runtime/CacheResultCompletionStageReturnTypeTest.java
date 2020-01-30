package io.quarkus.cache.test.runtime;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.cache.CacheResult;
import io.quarkus.test.QuarkusUnitTest;

public class CacheResultCompletionStageReturnTypeTest {

    private static final Object KEY_1 = new Object();
    private static final Object KEY_2 = new Object();

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class).addClass(CachedService.class));

    @Inject
    CachedService cachedService;

    @Test
    public void testAllCacheAnnotations() throws InterruptedException, ExecutionException {
        // STEP 1
        // Action: @CacheResult-annotated method call.
        // Expected effect: method invoked and result cached.
        // Verified by: STEP 2.
        CompletionStage<Object> completionStage1 = cachedService.cachedMethod(KEY_1);

        // STEP 2
        // Action: same call as STEP 1.
        // Expected effect: method not invoked and result coming from the cache.
        // Verified by: same object reference between STEPS 1 and 2 results.
        CompletionStage<Object> completionStage2 = cachedService.cachedMethod(KEY_1);
        assertTrue(completionStage1 == completionStage2);

        // STEP 3
        // Action: same call as STEP 2 with a new key.
        // Expected effect: method invoked and result cached.
        // Verified by: different objects references between STEPS 2 and 3 results.
        CompletionStage<Object> completionStage3 = cachedService.cachedMethod(KEY_2);
        assertTrue(completionStage2 != completionStage3);

        // We need all of the futures to complete at this point.
        CompletableFuture.allOf(completionStage1.toCompletableFuture(), completionStage2.toCompletableFuture(),
                completionStage3.toCompletableFuture()).get();

        Object value1 = completionStage1.toCompletableFuture().get();
        Object value2 = completionStage2.toCompletableFuture().get();
        Object value3 = completionStage3.toCompletableFuture().get();

        // Values objects references resulting from STEPS 1 and 2 should be equal since the same cache key was used.
        assertTrue(value1 == value2);

        // Values objects references resulting from STEPS 2 and 3 should be different since a different cache key was used.
        assertTrue(value2 != value3);
    }

    @ApplicationScoped
    static class CachedService {

        // This is required to make sure the CompletableFuture from the tests are executed concurrently.
        private ExecutorService executorService = Executors.newFixedThreadPool(3);

        @CacheResult(cacheName = "test-cache")
        public CompletionStage<Object> cachedMethod(Object key) {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    // This is another requirement for concurrent CompletableFuture executions.
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                return new Object();
            }, executorService);
        }
    }
}
