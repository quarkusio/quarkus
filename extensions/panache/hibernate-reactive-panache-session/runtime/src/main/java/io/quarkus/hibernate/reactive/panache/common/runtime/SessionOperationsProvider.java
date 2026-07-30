package io.quarkus.hibernate.reactive.panache.common.runtime;

import java.util.function.Function;
import java.util.function.Supplier;

import org.hibernate.reactive.mutiny.Mutiny;
import org.hibernate.reactive.mutiny.Mutiny.Transaction;

import io.smallrye.mutiny.Uni;
import io.vertx.core.Context;

/**
 * Implementation of reactive session operations.
 * <p>
 * Registered by {@code quarkus-hibernate-reactive-panache-common} when Hibernate Reactive is present.
 */
public interface SessionOperationsProvider {

    <T> Uni<T> withSessionOnDemand(Supplier<Uni<T>> work);

    <T> Uni<T> withTransaction(Supplier<Uni<T>> work);

    <T> Uni<T> withTransaction(String persistenceUnitName, Supplier<Uni<T>> work);

    <T> Uni<T> withTransaction(Function<Transaction, Uni<T>> work);

    <T> Uni<T> withStatelessTransaction(Supplier<Uni<T>> work);

    <T> Uni<T> withStatelessTransaction(String persistenceUnitName, Supplier<Uni<T>> work);

    <T> Uni<T> withStatelessTransaction(Function<Transaction, Uni<T>> work);

    <T> Uni<T> withSession(String persistenceUnitName, Function<Mutiny.Session, Uni<T>> work);

    <T> Uni<T> withSession(Function<Mutiny.Session, Uni<T>> work);

    <T> Uni<T> withStatelessSession(String persistenceUnitName, Function<Mutiny.StatelessSession, Uni<T>> work);

    <T> Uni<T> withStatelessSession(Function<Mutiny.StatelessSession, Uni<T>> work);

    Uni<Mutiny.Session> getSession();

    Uni<Mutiny.Session> getSession(String persistenceUnitName);

    Uni<Mutiny.StatelessSession> getStatelessSession();

    Uni<Mutiny.StatelessSession> getStatelessSession(String persistenceUnitName);

    Mutiny.Session getCurrentSession(String persistenceUnitName);

    Mutiny.StatelessSession getCurrentStatelessSession(String persistenceUnitName);

    Context vertxContext();
}
