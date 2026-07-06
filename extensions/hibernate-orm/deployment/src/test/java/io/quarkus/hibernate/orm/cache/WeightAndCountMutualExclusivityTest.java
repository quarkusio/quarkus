package io.quarkus.hibernate.orm.cache;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

/**
 * Tests that setting both {@code object-count} and {@code maximum-weight}
 * on the same cache region fails at build time with a clear error message.
 */
public class WeightAndCountMutualExclusivityTest {

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(CachedEntity.class)
                    .addAsResource("application.properties"))
            .overrideConfigKey("quarkus.hibernate-orm.second-level-caching-enabled", "true")
            .overrideConfigKey(
                    "quarkus.hibernate-orm.cache.\"io.quarkus.hibernate.orm.cache.WeightAndCountMutualExclusivityTest$CachedEntity\".memory.object-count",
                    "100")
            .overrideConfigKey(
                    "quarkus.hibernate-orm.cache.\"io.quarkus.hibernate.orm.cache.WeightAndCountMutualExclusivityTest$CachedEntity\".memory.maximum-weight",
                    "50000")
            .assertException(
                    t -> assertThat(t).hasMessageContaining("'object-count' and 'maximum-weight' are mutually exclusive"));

    @Test
    public void testValidationFails() {
        // Should not reach here -- the build should fail
    }

    @Entity
    @Cacheable
    public static class CachedEntity {

        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE)
        private long id;

        private String name;

        public CachedEntity() {
        }
    }
}
