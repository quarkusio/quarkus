package io.quarkus.cache.test.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.cache.CacheInvalidateAll;
import io.quarkus.cache.CacheResult;
import io.quarkus.test.QuarkusUnitTest;

/**
 * Test to reproduce issue #52143: Cache invalidation happens before method logic completes.
 */
public class CacheInvalidationConcurrencyTest {

    private static final String CACHE_NAME = "test-cache";
    private static final String ASYNC_CACHE_NAME = "async-test-cache";
    private static final String ENTITY_KEY = "entity-1";

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(EntityService.class, AsyncEntityService.class));

    @Inject
    EntityService entityService;

    @Inject
    AsyncEntityService asyncEntityService;

    @Test
    void testCacheInvalidationRaceCondition() throws Exception {
        ExecutorService executorService = Executors.newFixedThreadPool(2);

        // Load entity into cache
        Entity entity1 = entityService.getEntity(ENTITY_KEY);
        assertEquals(1, entityService.getRetrievalInvocations());
        assertEquals(ENTITY_KEY, entity1.getId());

        entityService.resetEntity();
        assertNotNull(entityService.getEntityFromDb(ENTITY_KEY), "Entity should exist before deletion starts");

        // Set up synchronization to control when deletion happens
        CountDownLatch invalidationComplete = new CountDownLatch(1);
        CountDownLatch deletionStart = new CountDownLatch(1);
        entityService.invalidationLatch = invalidationComplete;
        entityService.deletionStartLatch = deletionStart;

        // Start deletion in background - cache invalidation happens in interceptor BEFORE method body executes
        CompletableFuture<Void> deletionFuture = CompletableFuture.runAsync(() -> {
            try {
                entityService.deleteEntity(ENTITY_KEY);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        }, executorService);

        // Wait for cache invalidation to complete (happens before deleteEntity method body)
        invalidationComplete.await(5, TimeUnit.SECONDS);

        // Retrieve entity after invalidation but before deletion completes - entity gets cached here
        Entity entity2 = entityService.getEntity(ENTITY_KEY);
        assertNull(entity2, "Entity has been invalidated");

        // Allow deletion to proceed and complete
        deletionStart.countDown();
        deletionFuture.get(5, TimeUnit.SECONDS);
        assertEquals(1, entityService.getDeletionInvocations());
        assertNull(entityService.getEntityFromDb(ENTITY_KEY), "Entity should be deleted from DB");

        // BUG DEMONSTRATION: Retrieve entity after deletion
        // With bug: entity3 != null
        // Without bug: entity3 == null
        Entity entity3 = entityService.getEntity(ENTITY_KEY);
        assertNull(entity3, "Entity should not exist after deletion");

        executorService.shutdown();
    }

    @Test
    void testCacheInvalidateAllAsyncTiming() throws ExecutionException, InterruptedException {
        // Load entity into cache
        String value1 = asyncEntityService.getEntity(ENTITY_KEY).get();
        assertEquals(1, asyncEntityService.getRetrievalInvocations());

        // Start deletion - invalidation should happen AFTER method completes
        CompletableFuture<Void> invalidateFuture = asyncEntityService.deleteEntity(ENTITY_KEY);
        // Method should be invoked (CompletionStage is eager)
        assertEquals(1, asyncEntityService.getDeletionInvocations());

        invalidateFuture.get();
        String value2 = asyncEntityService.getEntity(ENTITY_KEY).get();

        // After invalidation completes, entity should be re-fetched (not from cache)
        // Verify by checking that method was invoked again (cache miss)
        assertEquals(2, asyncEntityService.getRetrievalInvocations(),
                "Method should be invoked again after invalidation - cache should be empty");
        assertNull(value2);
    }

    @ApplicationScoped
    static class EntityService {

        private final AtomicInteger retrievalInvocations = new AtomicInteger(0);
        private final AtomicInteger deletionInvocations = new AtomicInteger(0);
        private final AtomicBoolean entityExists = new AtomicBoolean(true);
        private volatile CountDownLatch invalidationLatch;
        private volatile CountDownLatch deletionStartLatch;

        @CacheResult(cacheName = CACHE_NAME)
        public Entity getEntity(String id) {
            retrievalInvocations.incrementAndGet();
            Entity entity = getEntityFromDb(id);
            return entity;
        }

        @CacheInvalidateAll(cacheName = CACHE_NAME)
        public void deleteEntity(String id) throws InterruptedException {
            deletionInvocations.incrementAndGet();
            // Signal that cache invalidation has completed (happens in interceptor before this method body)
            if (invalidationLatch != null) {
                Thread.sleep(10);
                invalidationLatch.countDown();
            }
            // Wait for test to retrieve entity in the race condition window
            if (deletionStartLatch != null) {
                deletionStartLatch.await(5, TimeUnit.SECONDS);
            }
            // Simulate DB transaction delay
            Thread.sleep(100);
            entityExists.set(false);
        }

        public Entity getEntityFromDb(String id) {
            if (entityExists.get()) {
                return new Entity(id);
            }
            return null;
        }

        public int getRetrievalInvocations() {
            return retrievalInvocations.get();
        }

        public int getDeletionInvocations() {
            return deletionInvocations.get();
        }

        public void resetEntity() {
            entityExists.set(true);
        }

        public boolean isEntityExists() {
            return entityExists.get();
        }
    }

    static class Entity {
        private final String id;

        public Entity(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }
    }

    @ApplicationScoped
    static class AsyncEntityService {

        private final AtomicInteger retrievalInvocations = new AtomicInteger(0);
        private final AtomicInteger deletionInvocations = new AtomicInteger(0);
        private final AtomicBoolean entityExists = new AtomicBoolean(true);

        @CacheResult(cacheName = ASYNC_CACHE_NAME)
        public CompletableFuture<String> getEntity(String id) {
            retrievalInvocations.incrementAndGet();
            if (entityExists.get()) {
                return CompletableFuture.completedFuture(id + "-value");
            }
            return CompletableFuture.completedFuture(null);
        }

        @CacheInvalidateAll(cacheName = ASYNC_CACHE_NAME)
        public CompletableFuture<Void> deleteEntity(String id) {
            deletionInvocations.incrementAndGet();
            entityExists.set(false);
            return CompletableFuture.completedFuture(null);
        }

        public int getRetrievalInvocations() {
            return retrievalInvocations.get();
        }

        public int getDeletionInvocations() {
            return deletionInvocations.get();
        }
    }
}
