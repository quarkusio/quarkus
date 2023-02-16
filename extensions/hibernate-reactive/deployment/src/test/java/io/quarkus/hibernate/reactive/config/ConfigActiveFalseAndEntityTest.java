package io.quarkus.hibernate.reactive.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;

import jakarta.enterprise.inject.CreationException;
import jakarta.persistence.EntityManagerFactory;

import org.hibernate.SessionFactory;
import org.hibernate.reactive.mutiny.Mutiny;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.test.QuarkusUnitTest;

public class ConfigActiveFalseAndEntityTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClass(MyEntity.class))
            .withConfigurationResource("application.properties")
            .overrideConfigKey("quarkus.hibernate-orm.active", "false");

    @Test
    public void entityManagerFactory() {
        EntityManagerFactory entityManagerFactory = Arc.container().instance(EntityManagerFactory.class).get();

        // The bean is always available to be injected during static init
        // since we don't know whether Hibernate Reactive will be active at runtime.
        // So the bean cannot be null.
        assertThat(entityManagerFactory).isNotNull();
        // However, any attempt to use it at runtime will fail.
        CreationException e = assertThrows(CreationException.class, () -> entityManagerFactory.getMetamodel());
        assertThat(e.getCause())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContainingAll(
                        "Cannot retrieve the EntityManagerFactory/SessionFactory for persistence unit default-reactive",
                        "Hibernate ORM was deactivated through configuration properties");
    }

    @Test
    public void sessionFactory() {
        SessionFactory sessionFactory = Arc.container().instance(SessionFactory.class).get();

        // The bean is always available to be injected during static init
        // since we don't know whether Hibernate Reactive will be active at runtime.
        // So the bean cannot be null.
        assertThat(sessionFactory).isNotNull();
        // However, any attempt to use it at runtime will fail.
        CreationException e = assertThrows(CreationException.class, () -> sessionFactory.getMetamodel());
        assertThat(e.getCause())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContainingAll(
                        "Cannot retrieve the EntityManagerFactory/SessionFactory for persistence unit default-reactive",
                        "Hibernate ORM was deactivated through configuration properties");
    }

    @Test
    public void mutinySessionFactory() {
        Mutiny.SessionFactory sessionFactory = Arc.container().instance(Mutiny.SessionFactory.class).get();

        // The bean is always available to be injected during static init
        // since we don't know whether Hibernate Reactive will be active at runtime.
        // So the bean cannot be null.
        assertThat(sessionFactory).isNotNull();
        // However, any attempt to use it at runtime will fail.
        assertThatThrownBy(sessionFactory::getMetamodel)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContainingAll(
                        "Cannot retrieve the Mutiny.SessionFactory for persistence unit default-reactive",
                        "Hibernate Reactive was deactivated through configuration properties");
    }

}
