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
 * Tests that a {@code weigher-class} that does not implement {@code Weigher} fails at startup with a clear error
 * message.
 */
public class WeigherClassNotImplementingWeigherTest {

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(CachedEntity.class)
                    .addClass(NotAWeigher.class)
                    .addAsResource("application.properties"))
            .overrideConfigKey("quarkus.hibernate-orm.second-level-caching-enabled", "true")
            .overrideConfigKey(
                    "quarkus.hibernate-orm.cache.\"io.quarkus.hibernate.orm.cache.WeigherClassNotImplementingWeigherTest$CachedEntity\".memory.maximum-weight",
                    "1000")
            .overrideConfigKey(
                    "quarkus.hibernate-orm.cache.\"io.quarkus.hibernate.orm.cache.WeigherClassNotImplementingWeigherTest$CachedEntity\".memory.weigher-class",
                    "io.quarkus.hibernate.orm.cache.WeigherClassNotImplementingWeigherTest$NotAWeigher")
            .assertException(t -> assertThat(t).hasStackTraceContaining(
                    "Weigher class 'io.quarkus.hibernate.orm.cache.WeigherClassNotImplementingWeigherTest$NotAWeigher'"
                            + " must implement com.github.benmanes.caffeine.cache.Weigher"));

    @Test
    public void testStartupFails() {
        // Should not reach here -- the startup should fail
    }

    @Entity
    @Cacheable
    public static class CachedEntity {
        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE)
        private long id;

        private String name;
    }

    public static class NotAWeigher {
    }
}
