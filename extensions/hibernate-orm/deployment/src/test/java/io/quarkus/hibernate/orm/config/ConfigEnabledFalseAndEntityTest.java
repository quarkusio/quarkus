package io.quarkus.hibernate.orm.config;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.test.QuarkusUnitTest;

public class ConfigEnabledFalseAndEntityTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClass(MyEntity.class))
            .withConfigurationResource("application.properties")
            // This should disable Hibernate ORM even if there is an entity
            .overrideConfigKey("quarkus.hibernate-orm.enabled", "false");

    @Test
    public void entityManagerFactory() {
        // The bean is not defined during static init, so it's null.
        assertThat(Arc.container().instance(EntityManagerFactory.class).get())
                .isNull();
    }

    @Test
    public void sessionFactory() {
        // The bean is not defined during static init, so it's null.
        assertThat(Arc.container().instance(SessionFactory.class).get())
                .isNull();
    }

    @Test
    @ActivateRequestContext
    public void entityManager() {
        // The bean is not defined during static init, so it's null.
        assertThat(Arc.container().instance(EntityManager.class).get())
                .isNull();
    }

    @Test
    @ActivateRequestContext
    public void session() {
        // The bean is not defined during static init, so it's null.
        assertThat(Arc.container().instance(Session.class).get())
                .isNull();
    }
}
