package io.quarkus.hibernate.reactive.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.inject.Inject;

import org.hibernate.reactive.mutiny.Mutiny;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.InactiveBeanException;
import io.quarkus.arc.InjectableBean;
import io.quarkus.arc.InjectableInstance;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.vertx.RunOnVertxContext;

public class NoConfigNoEntitiesTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withEmptyApplication()
            // The config won't really be empty if dev services are enabled
            .overrideConfigKey("quarkus.devservices.enabled", "false");

    @Inject
    InjectableInstance<Mutiny.SessionFactory> sessionFactory;

    // When having no entities, no configuration, no datasource,
    // as long as the Hibernate ORM beans are not injected anywhere,
    // we should still be able to start the application.
    @Test
    @RunOnVertxContext
    public void testBootSucceedsButHibernateReactiveDeactivated() {
        // ... and Hibernate Reactive's beans should be available, but inactive.
        var instance = sessionFactory;
        assertThat(instance.getHandle().getBean())
                .isNotNull()
                .returns(false, InjectableBean::isActive);
        var object = instance.get();
        assertThat(object).isNotNull();
        // and any attempt to use these beans at runtime should fail.
        assertThatThrownBy(object::getCriteriaBuilder)
                .isInstanceOf(InactiveBeanException.class)
                .hasMessageContainingAll(
                        "Persistence unit '<default>' was deactivated automatically because it doesn't include any entity type and its datasource '<default>' was deactivated",
                        "Datasource '<default>' was deactivated automatically because its URL is not set",
                        "To avoid this exception while keeping the bean inactive", // Message from Arc with generic hints
                        "To activate the datasource, set configuration property 'quarkus.datasource.reactive.url'",
                        "Refer to https://quarkus.io/guides/datasource for guidance.");
    }

}
