package io.quarkus.hibernate.reactive.config;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.hibernate.reactive.mutiny.Mutiny;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InactiveBeanException;
import io.quarkus.test.QuarkusUnitTest;

public class NoConfigTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withEmptyApplication()
            // The config won't really be empty if dev services are enabled
            .overrideConfigKey("quarkus.devservices.enabled", "false");

    // When having no entities, no configuration, no datasource,
    // as long as the Hibernate ORM beans are not injected anywhere,
    // we should still be able to start the application.
    @Test
    public void testBootSucceedsButHibernateOrmDeactivated() {
        assertThatThrownBy(() -> Arc.container().instance(Mutiny.SessionFactory.class).get())
                .isInstanceOf(InactiveBeanException.class)
                .hasMessageContainingAll(
                        "Persistence unit '<default>' was deactivated automatically because its datasource '<default>' was deactivated.",
                        "Datasource '<default>' was deactivated automatically because its URL is not set.",
                        "To avoid this exception while keeping the bean inactive", // Message from Arc with generic hints
                        "To activate the datasource, set configuration property 'quarkus.datasource.reactive.url'.",
                        "Refer to https://quarkus.io/guides/datasource for guidance.");
    }

}
