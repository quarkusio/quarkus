package io.quarkus.hibernate.orm.cache;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.github.benmanes.caffeine.cache.Weigher;

import io.quarkus.test.QuarkusExtensionTest;

/**
 * Tests that a {@code weigher-class} without a public no-arg constructor fails at startup with a clear error message.
 */
public class WeigherClassWithoutNoArgConstructorTest {

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(CachedEntity.class)
                    .addClass(WeigherWithoutNoArgConstructor.class)
                    .addAsResource("application.properties"))
            .overrideConfigKey("quarkus.hibernate-orm.second-level-caching-enabled", "true")
            .overrideConfigKey(
                    "quarkus.hibernate-orm.cache.\"io.quarkus.hibernate.orm.cache.WeigherClassWithoutNoArgConstructorTest$CachedEntity\".memory.maximum-weight",
                    "1000")
            .overrideConfigKey(
                    "quarkus.hibernate-orm.cache.\"io.quarkus.hibernate.orm.cache.WeigherClassWithoutNoArgConstructorTest$CachedEntity\".memory.weigher-class",
                    "io.quarkus.hibernate.orm.cache.WeigherClassWithoutNoArgConstructorTest$WeigherWithoutNoArgConstructor")
            .assertException(t -> assertThat(t)
                    .hasStackTraceContaining("Unable to instantiate weigher class: "
                            + "io.quarkus.hibernate.orm.cache.WeigherClassWithoutNoArgConstructorTest$WeigherWithoutNoArgConstructor")
                    .hasRootCauseInstanceOf(NoSuchMethodException.class));

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

    public static class WeigherWithoutNoArgConstructor implements Weigher<Object, Object> {
        private final int weight;

        public WeigherWithoutNoArgConstructor(int weight) {
            this.weight = weight;
        }

        @Override
        public int weigh(Object key, Object value) {
            return weight;
        }
    }
}
