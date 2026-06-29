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
 * This should cause a persistence unit to be created (from the injection point)
 * and fail because no JDBC driver is available.
 *
 * @see <a href="https://github.com/quarkusio/quarkus/issues/51268">#51268</a>.
 */
public class JdbcDriverMissingInjectionTest {

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(JdbcDriverMissingInjectionTest.class))
            .setExcludedDependencies(Set.of(
                    ArtifactKey.of("io.quarkus", "quarkus-jdbc-h2"),
                    ArtifactKey.of("io.quarkus", "quarkus-jdbc-h2-deployment")))
            .assertException(t -> assertThat(t)
                    .hasMessageContainingAll(
                            "Hibernate ORM persistence unit '<default>' cannot be created",
                            "JDBC datasource '<default>' cannot be created",
                            "Cannot infer the database kind", "no JDBC driver extension",
                            "being created because of",
                            "Injection of 'StatelessSession'"));

    @Inject
    StatelessSession statelessSession;

    @Test
    public void test() {
        // Should not be reached
        Assertions.fail("Startup should have failed");
    }

}
