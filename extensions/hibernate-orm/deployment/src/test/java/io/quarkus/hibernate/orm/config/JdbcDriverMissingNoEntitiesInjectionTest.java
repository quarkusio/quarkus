package io.quarkus.hibernate.orm.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import jakarta.inject.Inject;

import org.assertj.core.api.Assertions;
import org.hibernate.StatelessSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * No entities and no JDBC driver, but the application injects a StatelessSession.
 * <p>
 * Ideally this should cause a persistence unit to be created (from the injection point)
 * and fail because no JDBC driver is available.
 * <p>
 * Right now injection points are ignored, so we only get a message from Arc about an unsatisfied injection point.
 *
 * @see <a href="https://github.com/quarkusio/quarkus/issues/51268">#51268</a>.
 * @see <a href="https://github.com/quarkusio/quarkus/issues/55217">#55217</a>.
 */
public class JdbcDriverMissingNoEntitiesInjectionTest {

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(JdbcDriverMissingNoEntitiesInjectionTest.class))
            .setExcludedDependencies(Set.of(
                    ArtifactKey.of("io.quarkus", "quarkus-jdbc-h2"),
                    ArtifactKey.of("io.quarkus", "quarkus-jdbc-h2-deployment")))
            .assertException(t -> assertThat(t)
                    .hasMessageContainingAll(
                            "Unsatisfied dependency for type org.hibernate.StatelessSession"));

    @Inject
    StatelessSession statelessSession;

    @Test
    public void test() {
        // Should not be reached
        Assertions.fail("Startup should have failed");
    }

}
