package io.quarkus.hibernate.reactive.config;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;

import org.hibernate.reactive.mutiny.Mutiny;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.InjectableInstance;
import io.quarkus.test.QuarkusUnitTest;

public class NoEntitiesNoDatasourceTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withEmptyApplication()
            // Ideally we would not add quarkus-reactive-pg-client to the classpath and there _really_ wouldn't be a datasource,
            // but that's inconvenient given our testing setup,
            // so we'll just disable the implicit datasource.
            .overrideConfigKey("quarkus.datasource.reactive", "false");

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
