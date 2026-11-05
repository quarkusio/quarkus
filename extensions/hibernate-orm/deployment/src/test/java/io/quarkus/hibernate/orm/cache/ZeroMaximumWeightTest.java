package io.quarkus.hibernate.orm.cache;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import jakarta.inject.Inject;
import jakarta.persistence.Cacheable;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.transaction.UserTransaction;

import org.awaitility.Awaitility;
import org.hibernate.SessionFactory;
import org.hibernate.cache.jcache.internal.JCacheRegionFactory;
import org.hibernate.cache.spi.CacheImplementor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.github.benmanes.caffeine.cache.Weigher;
import com.github.benmanes.caffeine.jcache.configuration.CaffeineConfiguration;

import io.quarkus.hibernate.orm.TransactionTestUtils;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * Tests that {@code maximum-weight=0} is accepted and evicts every entry with a positive weight.
 */
public class ZeroMaximumWeightTest {

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(CachedEntity.class)
                    .addClass(ConstantWeigher.class)
                    .addClass(TransactionTestUtils.class)
                    .addAsResource("application.properties"))
            .overrideConfigKey("quarkus.hibernate-orm.second-level-caching-enabled", "true")
            .overrideConfigKey(
                    "quarkus.hibernate-orm.cache.\"io.quarkus.hibernate.orm.cache.ZeroMaximumWeightTest$CachedEntity\".memory.maximum-weight",
                    "0")
            .overrideConfigKey(
                    "quarkus.hibernate-orm.cache.\"io.quarkus.hibernate.orm.cache.ZeroMaximumWeightTest$CachedEntity\".memory.weigher-class",
                    "io.quarkus.hibernate.orm.cache.ZeroMaximumWeightTest$ConstantWeigher");

    @Inject
    EntityManager em;

    @Inject
    UserTransaction tx;

    @Inject
    org.hibernate.Cache hibernateCache;

    @Test
    public void testEveryEntryIsEvicted() {
        // the region really is weight-bounded at 0 with the weigher in place, so an absent entry
        // below is caused by eviction rather than the region being misconfigured
        var regionFactory = (JCacheRegionFactory) ((CacheImplementor) em.getEntityManagerFactory()
                .unwrap(SessionFactory.class).getCache()).getRegionFactory();
        CaffeineConfiguration<?, ?> config = regionFactory.getCacheManager().getCache(CachedEntity.class.getName())
                .getConfiguration(CaffeineConfiguration.class);
        assertThat(config.getMaximumWeight()).hasValue(0);
        assertThat(config.getWeigherFactory()).isPresent();

        CachedEntity entity = new CachedEntity();
        TransactionTestUtils.inTransaction(tx, () -> {
            em.persist(entity);
            em.flush();
        });
        TransactionTestUtils.inTransaction(tx, () -> em.find(CachedEntity.class, entity.id));

        // Caffeine evicts asynchronously, so the entry may still be visible right after the load
        Awaitility.await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> assertThat(hibernateCache.contains(CachedEntity.class, entity.id)).isFalse());
    }

    @Entity
    @Cacheable
    public static class CachedEntity {
        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE)
        private long id;

        private String name;
    }

    public static class ConstantWeigher implements Weigher<Object, Object> {
        @Override
        public int weigh(Object key, Object value) {
            return 1;
        }
    }
}
