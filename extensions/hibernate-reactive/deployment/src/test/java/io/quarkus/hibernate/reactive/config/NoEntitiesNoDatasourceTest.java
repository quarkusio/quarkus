package io.quarkus.hibernate.reactive.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;

import org.hibernate.reactive.mutiny.Mutiny;
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
                    ArtifactKey.of("io.quarkus", "quarkus-reactive-pg-client"),
                    ArtifactKey.of("io.quarkus", "quarkus-reactive-pg-client-deployment")));

    @Inject
    InjectableInstance<Mutiny.SessionFactory> sessionFactory;

    // When having no entities, no configuration, and no datasource,
    // as long as the Hibernate Reactive beans are not injected anywhere,
    // we should still be able to start the application.
    @Test
    @ActivateRequestContext
    public void test() {
        // But Hibernate Reactive should be disabled, so its beans should not be there.
        assertThat(sessionFactory.isUnsatisfied()).isTrue();
    }

}
