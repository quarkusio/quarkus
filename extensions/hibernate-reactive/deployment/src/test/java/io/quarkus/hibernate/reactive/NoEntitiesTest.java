package io.quarkus.hibernate.reactive;

import static org.assertj.core.api.Assertions.assertThat;

import org.hibernate.reactive.mutiny.Mutiny;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.test.QuarkusUnitTest;

public class NoEntitiesTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withEmptyApplication()
            .withConfigurationResource("application.properties");

    // When having no entities,
    // as long as the Hibernate Reactive beans are not injected anywhere,
    // we should still be able to start the application.
    @Test
    public void testBootSucceedsButHibernateOrmDeactivated() {
        // ... but Hibernate Reactive's beans should not be available.
        assertThat(Arc.container().instance(Mutiny.SessionFactory.class).get()).isNull();
    }

}
