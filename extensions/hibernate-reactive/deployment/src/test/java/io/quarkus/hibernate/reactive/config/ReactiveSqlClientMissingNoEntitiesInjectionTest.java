package io.quarkus.hibernate.reactive.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;

import jakarta.inject.Inject;

import org.hibernate.reactive.mutiny.Mutiny;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * No entities and no reactive SQL client, but the application injects a Mutiny.SessionFactory.
 * <p>
 * This should cause a persistence unit to be created (from the injection point)
 * and fail because no reactive SQL client is available.
 * <p>
 * Right now injection points are ignored, so we only get a message from Arc about an unsatisfied injection point.
 *
 * @see <a href="https://github.com/quarkusio/quarkus/issues/51268">#51268</a>.
 * @see <a href="https://github.com/quarkusio/quarkus/issues/55217">#55217</a>.
 */
public class ReactiveSqlClientMissingNoEntitiesInjectionTest {

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(ReactiveSqlClientMissingNoEntitiesInjectionTest.class))
            .setExcludedDependencies(Set.of(
                    ArtifactKey.of("io.quarkus", "quarkus-reactive-pg-client"),
                    ArtifactKey.of("io.quarkus", "quarkus-reactive-pg-client-deployment")))
            .overrideConfigKey("quarkus.devservices.enabled", "false")
            .assertException(t -> assertThat(t)
                    .hasMessageContainingAll(
                            "Unsatisfied dependency for type org.hibernate.reactive.mutiny.Mutiny$SessionFactory"));

    @Inject
    Mutiny.SessionFactory sessionFactory;

    @Test
    public void test() {
        fail("Startup should have failed");
    }

}
