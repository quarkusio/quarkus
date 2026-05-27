package io.quarkus.hibernate.orm.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.hibernate.Session;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * No entities, no injection, no configuration: Hibernate ORM should not create
 * any persistence unit, and the application should start successfully even without
 * a JDBC driver.
 *
 * @see <a href="https://github.com/quarkusio/quarkus/issues/51268">#51268</a>.
 */
public class JdbcDriverMissingNoEntitiesTest {

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withEmptyApplication()
            .setExcludedDependencies(Set.of(
                    ArtifactKey.of("io.quarkus", "quarkus-jdbc-h2"),
                    ArtifactKey.of("io.quarkus", "quarkus-jdbc-h2-deployment")));

    @Test
    public void test() {
        assertThat(Arc.container().select(Session.class).isUnsatisfied()).isTrue();
    }

}
