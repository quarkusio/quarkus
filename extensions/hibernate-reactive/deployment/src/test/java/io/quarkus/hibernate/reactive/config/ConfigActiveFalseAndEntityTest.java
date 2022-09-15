package io.quarkus.hibernate.reactive.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import jakarta.persistence.EntityManagerFactory;

import org.hibernate.SessionFactory;
import org.hibernate.reactive.mutiny.Mutiny;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.test.QuarkusUnitTest;
import io.vertx.core.Vertx;
import io.vertx.core.impl.ContextInternal;

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
        assertThatThrownBy(entityManagerFactory::getMetamodel)
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
        assertThatThrownBy(sessionFactory::getMetamodel)
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

    @Test
    public void mutinySession() {
        Mutiny.Session session = Arc.container().instance(Mutiny.Session.class).get();

        // The bean is always available to be injected during static init
        // since we don't know whether Hibernate Reactive will be active at runtime.
        // So the bean cannot be null.
        assertThat(session).isNotNull();
        // However, any attempt to use it at runtime will fail.
        assertThatThrownBy(() -> runInVertxAndJoin(() -> {
            try {
                Arc.container().requestContext().activate();
                return session.find(MyEntity.class, 0L);
            } finally {
                Arc.container().requestContext().deactivate();
            }
        }))
                .cause()
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContainingAll("Cannot retrieve the Mutiny.SessionFactory for persistence unit default-reactive",
                        "Hibernate Reactive was deactivated through configuration properties");
    }

    private <T> T runInVertxAndJoin(Supplier<T> action) {
        CompletableFuture<T> future = new CompletableFuture<>();
        runInVertx(() -> {
            try {
                future.complete(action.get());
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future.join();
    }

    // Copied from org.hibernate.reactive.context.impl.VertxContext.execute.
    // Unfortunately we can't use that class here, so we need to duplicate a few lines of code.
    // This seems to be the simplest way to run code in Vertx from a test...
    private void runInVertx(Runnable action) {
        final io.vertx.core.Context newContext = Arc.container().instance(Vertx.class).get().getOrCreateContext();
        ContextInternal newContextInternal = (ContextInternal) newContext;
        final ContextInternal duplicate = newContextInternal.duplicate();
        duplicate.runOnContext(ignored -> action.run());
    }
}
