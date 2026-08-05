package io.quarkus.hibernate.reactive.panache.test.config;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.vertx.RunOnVertxContext;

public class ReactiveSqlClientMissingNoEntitiesTest {

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withEmptyApplication()
            .setExcludedDependencies(Set.of(
                    ArtifactKey.of("io.quarkus", "quarkus-reactive-pg-client"),
                    ArtifactKey.of("io.quarkus", "quarkus-reactive-pg-client-deployment")));

    // When having no entities, no configuration, and no datasource,
    // we should still be able to start the application.
    @Test
    @RunOnVertxContext
    public void test() {
        // ... but any attempt to use Panache at runtime should fail.
        assertThatThrownBy(() -> Panache.getSession())
                .hasMessage("Mutiny.SessionFactory bean not found");
    }

}
