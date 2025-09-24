package io.quarkus.hibernate.reactive.panache.test.config;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.vertx.RunOnVertxContext;

public class NoEntitiesNoDatasourceTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withEmptyApplication()
            // Ideally we would not add quarkus-reactive-pg-client to the classpath and there _really_ wouldn't be a datasource,
            // but that's inconvenient given our testing setup,
            // so we'll just disable the implicit datasource.
            .overrideConfigKey("quarkus.datasource.reactive", "false");

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
