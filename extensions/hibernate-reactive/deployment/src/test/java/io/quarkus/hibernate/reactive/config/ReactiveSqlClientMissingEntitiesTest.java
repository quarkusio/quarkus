package io.quarkus.hibernate.reactive.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.hibernate.reactive.mutiny.Mutiny;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * Test behavior when using Hibernate Reactive with entities
 * but without any reactive SQL client dependency (the user forgot to add one).
 *
 * @see <a href="https://github.com/quarkusio/quarkus/issues/51268">#51268</a>.
 */
public class ReactiveSqlClientMissingEntitiesTest {

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(MyEntity.class))
            .setExcludedDependencies(Set.of(
                    ArtifactKey.of("io.quarkus", "quarkus-reactive-pg-client"),
                    ArtifactKey.of("io.quarkus", "quarkus-reactive-pg-client-deployment")))
            .overrideConfigKey("quarkus.devservices.enabled", "false")
            .assertException(t -> assertThat(t)
                    .hasMessageContainingAll(
                            "persistence unit '<default>' cannot be created",
                            "Cannot infer the database kind", "no reactive SQL client extension"));

    @Test
    public void test() {
        // Hibernate Reactive silently disables itself when datasource is missing
        assertThat(Arc.container().select(Mutiny.SessionFactory.class).isUnsatisfied()).isTrue();
    }

}
