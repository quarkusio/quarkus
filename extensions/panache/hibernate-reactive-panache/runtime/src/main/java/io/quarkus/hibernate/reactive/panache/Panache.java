package io.quarkus.hibernate.reactive.panache;

import java.util.Map;
import java.util.function.Supplier;

import org.hibernate.reactive.mutiny.Mutiny;

import io.quarkus.hibernate.reactive.panache.common.runtime.AbstractJpaOperations;
import io.quarkus.hibernate.reactive.panache.common.runtime.SessionOperations;
import io.quarkus.panache.common.Parameters;
import io.smallrye.mutiny.Uni;

/**
 * Utility class for Panache.
 *
 * @author Stéphane Épardaud
 */
public class Panache {

    /**
     * Obtains a {@link Uni} within the scope of a reactive session. If a reactive session exists then it is reused. If
     * it does not exist not exist then open a new session that is automatically closed when the provided {@link Uni}
     * completes.
     *
     * @param <T>
     * @param uniSupplier
     *
     * @return a new {@link Uni}
     */
    public static <T> Uni<T> withSession(Supplier<Uni<T>> uniSupplier) {
        return SessionOperations.withSession(s -> uniSupplier.get());
    }

    /**
     * Returns the current {@link Mutiny.Session}
     *
     * @return the current {@link Mutiny.Session}
     */
    public static Uni<Mutiny.Session> getSession() {
        return SessionOperations.getSession();
    }

    /**
     * Performs the given work within the scope of a database transaction, automatically flushing the session. The
     * transaction will be rolled back if the work completes with an uncaught exception, or if
     * {@link Mutiny.Transaction#markForRollback()} is called.
     *
     * @param <T>
     *        The function's return type
     * @param work
     *        The function to execute in the new transaction
     *
     * @return the result of executing the function
     *
     * @see Panache#currentTransaction()
     */
    public static <T> Uni<T> withTransaction(Supplier<Uni<T>> work) {
        return SessionOperations.withTransaction(() -> work.get());
    }

    /**
     * Executes a database update operation and return the number of rows operated on.
     *
     * @param query
     *        a normal HQL query
     * @param params
     *        optional list of indexed parameters
     *
     * @return the number of rows operated on.
     */
    public static Uni<Integer> executeUpdate(String query, Object... params) {
        return AbstractJpaOperations.executeUpdate(query, params);
    }

    /**
     * Executes a database update operation and return the number of rows operated on.
     *
     * @param query
     *        a normal HQL query
     * @param params
     *        {@link Map} of named parameters
     *
     * @return the number of rows operated on.
     */
    public static Uni<Integer> executeUpdate(String query, Map<String, Object> params) {
        return AbstractJpaOperations.executeUpdate(query, params);
    }

    /**
     * Executes a database update operation and return the number of rows operated on.
     *
     * @param query
     *        a normal HQL query
     * @param params
     *        {@link Parameters} of named parameters
     *
     * @return the number of rows operated on.
     */
    public static Uni<Integer> executeUpdate(String query, Parameters params) {
        return AbstractJpaOperations.executeUpdate(query, params.map());
    }

    /**
     * Flush all pending changes to the database.
     *
     * @return void
     */
    public static Uni<Void> flush() {
        return getSession().flatMap(session -> session.flush());
    }

    /**
     * Returns the current transaction, if any, or <code>null</code>.
     *
     * @return the current transaction, if any, or <code>null</code>.
     *
     * @see Panache#withTransaction(Supplier)
     */
    public static Uni<Mutiny.Transaction> currentTransaction() {
        return getSession().map(session -> session.currentTransaction());
    }
}
