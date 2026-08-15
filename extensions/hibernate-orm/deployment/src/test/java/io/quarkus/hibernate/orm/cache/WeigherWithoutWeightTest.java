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
 * Tests that setting {@code weigher-class} without {@code maximum-weight}
 * fails at build time with a clear error message.
 */
public class WeigherWithoutWeightTest {

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(CachedEntity.class)
                    .addAsResource("application.properties"))
            .overrideConfigKey("quarkus.hibernate-orm.second-level-caching-enabled", "true")
            .overrideConfigKey(
                    "quarkus.hibernate-orm.cache.\"io.quarkus.hibernate.orm.cache.WeigherWithoutWeightTest$CachedEntity\".memory.weigher-class",
                    "com.example.SomeWeigher")
            .assertException(t -> assertThat(t).hasMessageContaining("'weigher-class' requires 'maximum-weight' to be set"));

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
