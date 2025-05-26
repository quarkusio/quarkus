package io.quarkus.hibernate.orm.singlepersistenceunit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.inject.Inject;
import jakarta.persistence.Cache;
import jakarta.persistence.EntityManager;
import jakarta.transaction.UserTransaction;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.TransactionTestUtils;
import io.quarkus.test.QuarkusUnitTest;

public class SinglePersistenceUnitCdiCacheTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(DefaultEntity.class)
                    .addClass(TransactionTestUtils.class)
                    .addAsResource("application.properties"))
            .overrideRuntimeConfigKey("quarkus.hibernate-orm.second-level-caching-enabled", "true");

    @Inject
    Cache jakartaCache;

    @Inject
    org.hibernate.Cache hibernateCache;
    @Inject
    EntityManager em;

    @Inject
    UserTransaction tx;

    @Test
    public void testJakartaCacheOperations() {
        DefaultEntity entity = new DefaultEntity("test");
        TransactionTestUtils.inTransaction(tx, () -> {
            em.persist(entity);
            em.flush();
        });

        TransactionTestUtils.inTransaction(tx, () -> {
            DefaultEntity loaded = em.find(DefaultEntity.class, entity.getId());
            assertNotNull(loaded, "Entity should be loaded");

            // Verify entity is in cache
            assertTrue(jakartaCache.contains(DefaultEntity.class, entity.getId()),
                    "Entity should be in cache after load");

            // Test cache operations
            DefaultEntity fromCache = em.find(DefaultEntity.class, entity.getId());
            assertNotNull(fromCache, "Entity should be retrieved from cache");
            assertEquals("test", fromCache.getName(), "Entity data should match");

            jakartaCache.evict(DefaultEntity.class, entity.getId());
            assertFalse(jakartaCache.contains(DefaultEntity.class, entity.getId()),
                    "Entity should not be in cache after eviction");

            DefaultEntity fromDatabase = em.find(DefaultEntity.class, entity.getId());
            assertNotNull(fromDatabase, "Entity should be retrievable from database after cache eviction");
        });
    }

    @Test
    public void testHibernateCacheOperations() {
        DefaultEntity entity = new DefaultEntity("test");
        TransactionTestUtils.inTransaction(tx, () -> {
            em.persist(entity);
            em.flush();
        });

        TransactionTestUtils.inTransaction(tx, () -> {
            DefaultEntity loaded = em.find(DefaultEntity.class, entity.getId());
            assertNotNull(loaded, "Entity should be loaded");

            // Verify entity is in cache
            assertTrue(hibernateCache.contains(DefaultEntity.class, entity.getId()),
                    "Entity should be in cache after load");

            // Test cache operations
            DefaultEntity fromCache = em.find(DefaultEntity.class, entity.getId());
            assertNotNull(fromCache, "Entity should be retrieved from cache");
            assertEquals("test", fromCache.getName(), "Entity data should match");

            hibernateCache.evict(DefaultEntity.class, entity.getId());
            assertFalse(hibernateCache.contains(DefaultEntity.class, entity.getId()),
                    "Entity should not be in cache after eviction");

            DefaultEntity fromDatabase = em.find(DefaultEntity.class, entity.getId());
            assertNotNull(fromDatabase, "Entity should be retrievable from database after cache eviction");
        });
    }

}
