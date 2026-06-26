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
 * This should cause a persistence unit to be created (from the injection point)
 * and fail because no reactive SQL client is available.
 *
 * @see <a href="https://github.com/quarkusio/quarkus/issues/51268">#51268</a>.
 */
public class ReactiveSqlClientMissingInjectionTest {

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(ReactiveSqlClientMissingInjectionTest.class))
            .setExcludedDependencies(Set.of(
                    ArtifactKey.of("io.quarkus", "quarkus-reactive-pg-client"),
                    ArtifactKey.of("io.quarkus", "quarkus-reactive-pg-client-deployment")))
            .overrideConfigKey("quarkus.devservices.enabled", "false")
            .assertException(t -> assertThat(t)
                    .hasMessageContainingAll(
                            "persistence unit '<default>' cannot be created",
                            "Reactive datasource '<default>' cannot be created",
                            "Cannot infer the database kind", "no reactive SQL Client extension",
                            "Injection of 'SessionFactory'"));

    @Inject
    Mutiny.SessionFactory sessionFactory;

    @Test
    public void test() {
        fail("Startup should have failed");
    }

}
