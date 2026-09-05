package io.quarkus.hibernate.orm.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * Test that we get a helpful error message when using Hibernate ORM with entities
 * but without any JDBC driver dependency (the user forgot to add one).
 *
 * @see <a href="https://github.com/quarkusio/quarkus/issues/51268">#51268</a>.
 */
public class JdbcDriverMissingEntitiesTest {

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(MyEntity.class))
            .setExcludedDependencies(Set.of(
                    ArtifactKey.of("io.quarkus", "quarkus-jdbc-h2"),
                    ArtifactKey.of("io.quarkus", "quarkus-jdbc-h2-deployment")))
            .assertException(t -> assertThat(t)
                    .hasMessageContainingAll(
                            "Hibernate ORM persistence unit '<default>' cannot be created",
                            "JDBC datasource '<default>' cannot be created",
                            "Cannot infer the database kind", "no JDBC driver extension",
                            "being created because of",
                            "JPA model including classes/packages", MyEntity.class.getName()));

    @Test
    public void test() {
        Assertions.fail("Startup should have failed");
    }

}
