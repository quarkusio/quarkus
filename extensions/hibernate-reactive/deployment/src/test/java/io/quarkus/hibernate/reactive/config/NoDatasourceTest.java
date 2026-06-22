package io.quarkus.hibernate.reactive.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import jakarta.inject.Inject;

import org.hibernate.reactive.mutiny.Mutiny;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.InjectableInstance;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.test.QuarkusExtensionTest;

public class NoDatasourceTest {

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(MyEntity.class))
            .setExcludedDependencies(Set.of(
                    ArtifactKey.of("io.quarkus", "quarkus-reactive-pg-client"),
                    ArtifactKey.of("io.quarkus", "quarkus-reactive-pg-client-deployment")));

    @Inject
    InjectableInstance<Mutiny.SessionFactory> sessionFactory;

    @Test
    public void test() {
        // Unlike Hibernate ORM, Hibernate Reactive will silently disable itself if the default datasource is missing, even if there are entities.
        // We may want to revisit that someday, but it's not easy to do without deeper interaction between the Hibernate ORM and Reactive extensions.
        assertThat(sessionFactory.isUnsatisfied()).isTrue();
    }

}
