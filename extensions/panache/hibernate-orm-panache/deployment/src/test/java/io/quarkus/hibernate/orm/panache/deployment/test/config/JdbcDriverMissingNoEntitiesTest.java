package io.quarkus.hibernate.orm.panache.deployment.test.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import jakarta.transaction.Transactional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.panache.Panache;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.test.QuarkusExtensionTest;

public class JdbcDriverMissingNoEntitiesTest {

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withEmptyApplication()
            .setExcludedDependencies(Set.of(
                    ArtifactKey.of("io.quarkus", "quarkus-jdbc-h2"),
                    ArtifactKey.of("io.quarkus", "quarkus-jdbc-h2-deployment")));

    // Test for https://github.com/quarkusio/quarkus/issues/50247
    // When having no entities, no configuration, and no datasource,
    // we should still be able to start the application.
    @Test
    @Transactional
    public void test() {
        // But Hibernate ORM should be disabled, so its beans should not be there.
        assertThat(Panache.getSession()).isNull();
    }

}
