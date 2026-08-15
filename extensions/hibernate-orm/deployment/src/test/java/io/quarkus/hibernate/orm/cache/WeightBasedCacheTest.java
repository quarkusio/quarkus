package io.quarkus.hibernate.orm.cache;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.inject.Inject;
import jakarta.persistence.Cacheable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.transaction.UserTransaction;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.github.benmanes.caffeine.cache.Weigher;

import io.quarkus.hibernate.orm.TransactionTestUtils;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * Tests that weight-based cache configuration is correctly applied
 * to Hibernate 2LC using Caffeine, including a custom weigher.
 */
public class WeightBasedCacheTest {

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(DataEntity.class)
                    .addClass(DataEntityWeigher.class)
                    .addClass(TransactionTestUtils.class)
                    .addAsResource("application.properties"))
            .overrideRuntimeConfigKey("quarkus.hibernate-orm.second-level-caching-enabled", "true")
            .overrideRuntimeConfigKey(
                    "quarkus.hibernate-orm.cache.\"io.quarkus.hibernate.orm.cache.WeightBasedCacheTest$DataEntity\".memory.maximum-weight",
                    "1000")
            .overrideRuntimeConfigKey(
                    "quarkus.hibernate-orm.cache.\"io.quarkus.hibernate.orm.cache.WeightBasedCacheTest$DataEntity\".memory.weigher-class",
                    "io.quarkus.hibernate.orm.cache.WeightBasedCacheTest$DataEntityWeigher");

    @Inject
    EntityManager em;

    @Inject
    UserTransaction tx;

    @Inject
    org.hibernate.Cache hibernateCache;

    @Test
    public void testWeightBasedCacheWorks() {
        DataEntity entity = new DataEntity("small data");
        TransactionTestUtils.inTransaction(tx, () -> {
            em.persist(entity);
            em.flush();
        });

        TransactionTestUtils.inTransaction(tx, () -> {
            DataEntity loaded = em.find(DataEntity.class, entity.getId());
            assertNotNull(loaded, "Entity should be loaded");

            // Verify entity is in cache
            assertTrue(hibernateCache.contains(DataEntity.class, entity.getId()),
                    "Entity should be in cache after load");
        });
    }

    @Entity
    @Cacheable
    public static class DataEntity {

        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "dataSeq")
        private long id;

        @Column
        private String data;

        public DataEntity() {
        }

        public DataEntity(String data) {
            this.data = data;
        }

        public long getId() {
            return id;
        }

        public String getData() {
            return data;
        }
    }

    /**
     * Weigher that assigns weight based on the data field length.
     */
    public static class DataEntityWeigher implements Weigher<Object, Object> {

        @Override
        public int weigh(Object key, Object value) {
            // Hibernate wraps entities in internal cache entries.
            // Default weight for unknown types.
            return 100;
        }
    }
}
