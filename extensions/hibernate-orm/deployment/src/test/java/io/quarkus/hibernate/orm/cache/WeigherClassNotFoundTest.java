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
 * Tests that a {@code weigher-class} that cannot be loaded fails at startup with a clear error message.
 */
public class WeigherClassNotFoundTest {

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(CachedEntity.class)
                    .addAsResource("application.properties"))
            .overrideConfigKey("quarkus.hibernate-orm.second-level-caching-enabled", "true")
            .overrideConfigKey(
                    "quarkus.hibernate-orm.cache.\"io.quarkus.hibernate.orm.cache.WeigherClassNotFoundTest$CachedEntity\".memory.maximum-weight",
                    "1000")
            .overrideConfigKey(
                    "quarkus.hibernate-orm.cache.\"io.quarkus.hibernate.orm.cache.WeigherClassNotFoundTest$CachedEntity\".memory.weigher-class",
                    "com.example.DoesNotExist")
            .assertException(t -> assertThat(t)
                    .hasStackTraceContaining("Unable to instantiate weigher class: com.example.DoesNotExist")
                    .hasRootCauseInstanceOf(ClassNotFoundException.class));

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
}
