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

import io.quarkus.hibernate.orm.TransactionTestUtils;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * Tests that setting {@code maximum-weight} without a custom weigher uses the
 * built-in dehydrated-entity weigher and still caches entities correctly.
 */
public class DefaultWeigherCacheTest {

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(DataEntity.class)
                    .addClass(TransactionTestUtils.class)
                    .addAsResource("application.properties"))
            .overrideRuntimeConfigKey("quarkus.hibernate-orm.second-level-caching-enabled", "true")
            .overrideRuntimeConfigKey(
                    "quarkus.hibernate-orm.cache.\"io.quarkus.hibernate.orm.cache.DefaultWeigherCacheTest$DataEntity\".memory.maximum-weight",
                    "1000");

    @Inject
    EntityManager em;

    @Inject
    UserTransaction tx;

    @Inject
    org.hibernate.Cache hibernateCache;

    @Test
    public void testDefaultWeigherWorks() {
        DataEntity entity = new DataEntity("small data");
        TransactionTestUtils.inTransaction(tx, () -> {
            em.persist(entity);
            em.flush();
        });

        TransactionTestUtils.inTransaction(tx, () -> {
            DataEntity loaded = em.find(DataEntity.class, entity.getId());
            assertNotNull(loaded, "Entity should be loaded");
            assertTrue(hibernateCache.contains(DataEntity.class, entity.getId()),
                    "Entity should be in cache after load");
        });
    }

    @Entity
    @Cacheable
    public static class DataEntity {

        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "defaultWeigherDataSeq")
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
}
