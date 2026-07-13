package io.quarkus.hibernate.reactive.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;

import jakarta.inject.Inject;

import org.hibernate.reactive.mutiny.Mutiny;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.InjectableInstance;
import io.quarkus.builder.Version;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * Multiple reactive SQL clients on the classpath, no explicit db-kind:
 * the reactive datasource cannot be created because the default db-kind is ambiguous.
 * Hibernate Reactive silently disables itself in this case.
 *
 * @see <a href="https://github.com/quarkusio/quarkus/issues/51268">#51268</a>.
 */
public class ReactiveSqlClientMultipleTest {

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(MyEntity.class))
            // Remove the default reactive PG client
            .setExcludedDependencies(Set.of(
                    ArtifactKey.of("io.quarkus", "quarkus-reactive-pg-client"),
                    ArtifactKey.of("io.quarkus", "quarkus-reactive-pg-client-deployment")))
            // Force two reactive SQL clients so the resolver sees ambiguity.
            // These must not bring in a JDBC driver transitively, otherwise
            // the ORM processor (not the reactive one) would handle the entities.
            .setForcedDependencies(List.of(
                    Dependency.of("io.quarkus", "quarkus-reactive-mysql-client-deployment", Version.getVersion()),
                    Dependency.of("io.quarkus", "quarkus-reactive-db2-client-deployment", Version.getVersion())))
            .overrideConfigKey("quarkus.devservices.enabled", "false");

    @Inject
    InjectableInstance<Mutiny.SessionFactory> sessionFactory;

    @Test
    public void test() {
        assertThat(sessionFactory.isUnsatisfied()).isTrue();
    }

}
