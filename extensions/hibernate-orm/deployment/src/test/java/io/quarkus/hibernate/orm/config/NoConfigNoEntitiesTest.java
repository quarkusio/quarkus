package io.quarkus.hibernate.orm.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.hibernate.Session;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.test.QuarkusUnitTest;

public class NoConfigNoEntitiesTest {

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
        // ... but Hibernate ORM's beans should not be available.
        assertThat(Arc.container().instance(Session.class).get()).isNull();
    }

}
