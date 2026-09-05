package io.quarkus.hibernate.orm.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.Version;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * Multiple JDBC drivers on the classpath (none test-scoped), no explicit db-kind:
 * the datasource cannot be created because the default db-kind is ambiguous.
 *
 * @see <a href="https://github.com/quarkusio/quarkus/issues/51268">#51268</a>.
 */
public class JdbcDriverMultipleTest {

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(MyEntity.class))
            // Remove the test-scoped H2 driver to prevent it from being picked as default
            .setExcludedDependencies(Set.of(
                    ArtifactKey.of("io.quarkus", "quarkus-jdbc-h2"),
                    ArtifactKey.of("io.quarkus", "quarkus-jdbc-h2-deployment")))
            // Force two non-test-scoped JDBC drivers so the resolver sees ambiguity
            .setForcedDependencies(List.of(
                    Dependency.of("io.quarkus", "quarkus-jdbc-postgresql-deployment", Version.getVersion()),
                    Dependency.of("io.quarkus", "quarkus-jdbc-mysql-deployment", Version.getVersion())))
            .assertException(t -> assertThat(t)
                    .hasMessageContainingAll(
                            "Hibernate ORM persistence unit '<default>' cannot be created",
                            "JDBC datasource '<default>' cannot be created",
                            "Cannot infer the database kind", "multiple JDBC driver extensions"));

    @Test
    public void test() {
        Assertions.fail("Startup should have failed");
    }

}
