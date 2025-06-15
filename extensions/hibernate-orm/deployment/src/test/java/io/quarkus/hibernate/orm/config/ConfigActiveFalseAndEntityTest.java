package io.quarkus.hibernate.orm.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;

import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.enterprise.inject.CreationException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.test.QuarkusUnitTest;

public class ConfigActiveFalseAndEntityTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest().withApplicationRoot(jar -> jar.addClass(MyEntity.class))
            .withConfigurationResource("application.properties")
            .overrideConfigKey("quarkus.hibernate-orm.active", "false");

    @Test
    public void entityManagerFactory() {
        EntityManagerFactory entityManagerFactory = Arc.container().instance(EntityManagerFactory.class).get();

        // The bean is always available to be injected during static init
        // since we don't know whether ORM will be active at runtime.
        // So the bean cannot be null.
        assertThat(entityManagerFactory).isNotNull();
        // However, any attempt to use it at runtime will fail.
        CreationException e = assertThrows(CreationException.class, () -> entityManagerFactory.getMetamodel());
        assertThat(e.getCause()).isInstanceOf(IllegalStateException.class).hasMessageContainingAll(
                "Cannot retrieve the EntityManagerFactory/SessionFactory for persistence unit <default>",
                "Hibernate ORM was deactivated through configuration properties");
    }

    @Test
    public void sessionFactory() {
        SessionFactory sessionFactory = Arc.container().instance(SessionFactory.class).get();

        // The bean is always available to be injected during static init
        // since we don't know whether ORM will be active at runtime.
        // So the bean cannot be null.
        assertThat(sessionFactory).isNotNull();
        // However, any attempt to use it at runtime will fail.
        CreationException e = assertThrows(CreationException.class, () -> sessionFactory.getMetamodel());
        assertThat(e.getCause()).isInstanceOf(IllegalStateException.class).hasMessageContainingAll(
                "Cannot retrieve the EntityManagerFactory/SessionFactory for persistence unit <default>",
                "Hibernate ORM was deactivated through configuration properties");
    }

    @Test
    @ActivateRequestContext
    public void entityManager() {
        EntityManager entityManager = Arc.container().instance(EntityManager.class).get();

        // The bean is always available to be injected during static init
        // since we don't know whether ORM will be active at runtime.
        // So the bean cannot be null.
        assertThat(entityManager).isNotNull();
        // However, any attempt to use it at runtime will fail.
        assertThatThrownBy(() -> entityManager.find(MyEntity.class, 0L))
                // Note that unlike for EntityManagerFactory/SessionFactory we get an IllegalStateException because
                // the real Session/EntityManager instance is created lazily through SessionLazyDelegator in
                // HibernateOrmRecorder.sessionSupplier()
                .isInstanceOf(IllegalStateException.class).hasMessageContainingAll(
                        "Cannot retrieve the EntityManagerFactory/SessionFactory for persistence unit <default>",
                        "Hibernate ORM was deactivated through configuration properties");
    }

    @Test
    @ActivateRequestContext
    public void session() {
        Session session = Arc.container().instance(Session.class).get();

        // The bean is always available to be injected during static init
        // since we don't know whether ORM will be active at runtime.
        // So the bean cannot be null.
        assertThat(session).isNotNull();
        // However, any attempt to use it at runtime will fail.
        assertThatThrownBy(() -> session.find(MyEntity.class, 0L))
                // Note that unlike for EntityManagerFactory/SessionFactory we get an IllegalStateException because
                // the real Session/EntityManager instance is created lazily through SessionLazyDelegator in
                // HibernateOrmRecorder.sessionSupplier()
                .isInstanceOf(IllegalStateException.class).hasMessageContainingAll(
                        "Cannot retrieve the EntityManagerFactory/SessionFactory for persistence unit <default>",
                        "Hibernate ORM was deactivated through configuration properties");
    }
}
