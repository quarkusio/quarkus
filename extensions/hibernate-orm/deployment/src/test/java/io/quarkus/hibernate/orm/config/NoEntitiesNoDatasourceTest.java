package io.quarkus.hibernate.orm.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;

import org.hibernate.Session;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.InjectableInstance;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.test.QuarkusExtensionTest;

public class NoEntitiesNoDatasourceTest {

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withEmptyApplication()
            .setExcludedDependencies(Set.of(
                    ArtifactKey.of("io.quarkus", "quarkus-jdbc-h2"),
                    ArtifactKey.of("io.quarkus", "quarkus-jdbc-h2-deployment")));

    @Inject
    InjectableInstance<Session> session;

    // When having no entities, no configuration, and no datasource,
    // as long as the Hibernate ORM beans are not injected anywhere,
    // we should still be able to start the application.
    @Test
    @ActivateRequestContext
    public void test() {
        // But Hibernate ORM should be disabled, so its beans should not be there.
        assertThat(session.isUnsatisfied()).isTrue();
    }

}
