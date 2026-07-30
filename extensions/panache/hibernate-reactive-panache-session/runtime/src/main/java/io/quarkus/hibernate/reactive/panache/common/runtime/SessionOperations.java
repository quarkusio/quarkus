package io.quarkus.hibernate.reactive.panache.common.runtime;

import java.util.function.Function;
import java.util.function.Supplier;

import org.hibernate.reactive.mutiny.Mutiny;
import org.hibernate.reactive.mutiny.Mutiny.Transaction;

import io.smallrye.mutiny.Uni;
import io.vertx.core.Context;

/**
 * Static util methods for {@link Mutiny.Session}.
 * <p>
 * Used by Hibernate Reactive Panache and by Quarkus Data Hibernate processor-generated metamodel code.
 * The implementation is registered by {@code quarkus-hibernate-reactive-panache-common} when
 * Hibernate Reactive is present.
 */
public final class SessionOperations {

    private static final String MISSING_HIBERNATE_REACTIVE = "Reactive Quarkus Data / Panache types require the Hibernate Reactive extension. "
            + "Add the 'quarkus-hibernate-reactive' extension and a reactive driver extension "
            + "(for example 'quarkus-reactive-pg-client') to your project dependencies.";

    private static volatile SessionOperationsProvider provider;

    private SessionOperations() {
    }

    /**
     * Registers the reactive session operations implementation.
     * Called from the Hibernate Reactive Panache common extension at static init.
     */
    public static void setProvider(SessionOperationsProvider provider) {
        SessionOperations.provider = provider;
    }

    static SessionOperationsProvider provider() {
        SessionOperationsProvider p = provider;
        if (p == null) {
            throw new IllegalStateException(MISSING_HIBERNATE_REACTIVE);
        }
        return p;
    }

    public static <T> Uni<T> withSessionOnDemand(Supplier<Uni<T>> work) {
        return provider().withSessionOnDemand(work);
    }

    public static <T> Uni<T> withTransaction(Supplier<Uni<T>> work) {
        return provider().withTransaction(work);
    }

    public static <T> Uni<T> withTransaction(String persistenceUnitName, Supplier<Uni<T>> work) {
        return provider().withTransaction(persistenceUnitName, work);
    }

    public static <T> Uni<T> withTransaction(Function<Transaction, Uni<T>> work) {
        return provider().withTransaction(work);
    }

    public static <T> Uni<T> withStatelessTransaction(Supplier<Uni<T>> work) {
        return provider().withStatelessTransaction(work);
    }

    public static <T> Uni<T> withStatelessTransaction(String persistenceUnitName, Supplier<Uni<T>> work) {
        return provider().withStatelessTransaction(persistenceUnitName, work);
    }

    public static <T> Uni<T> withStatelessTransaction(Function<Transaction, Uni<T>> work) {
        return provider().withStatelessTransaction(work);
    }

    public static <T> Uni<T> withSession(String persistenceUnitName, Function<Mutiny.Session, Uni<T>> work) {
        return provider().withSession(persistenceUnitName, work);
    }

    public static <T> Uni<T> withSession(Function<Mutiny.Session, Uni<T>> work) {
        return provider().withSession(work);
    }

    public static <T> Uni<T> withStatelessSession(String persistenceUnitName,
            Function<Mutiny.StatelessSession, Uni<T>> work) {
        return provider().withStatelessSession(persistenceUnitName, work);
    }

    public static <T> Uni<T> withStatelessSession(Function<Mutiny.StatelessSession, Uni<T>> work) {
        return provider().withStatelessSession(work);
    }

    public static Uni<Mutiny.Session> getSession() {
        return provider().getSession();
    }

    public static Uni<Mutiny.Session> getSession(String persistenceUnitName) {
        return provider().getSession(persistenceUnitName);
    }

    public static Uni<Mutiny.StatelessSession> getStatelessSession() {
        return provider().getStatelessSession();
    }

    public static Uni<Mutiny.StatelessSession> getStatelessSession(String persistenceUnitName) {
        return provider().getStatelessSession(persistenceUnitName);
    }

    public static Mutiny.Session getCurrentSession(String persistenceUnitName) {
        return provider().getCurrentSession(persistenceUnitName);
    }

    public static Mutiny.StatelessSession getCurrentStatelessSession(String persistenceUnitName) {
        return provider().getCurrentStatelessSession(persistenceUnitName);
    }

    public static Context vertxContext() {
        return provider().vertxContext();
    }
}
