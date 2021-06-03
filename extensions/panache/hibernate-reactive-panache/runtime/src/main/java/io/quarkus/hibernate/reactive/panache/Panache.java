package io.quarkus.hibernate.reactive.panache;

import java.util.Map;
import java.util.function.Supplier;

import org.hibernate.reactive.mutiny.Mutiny;

import io.quarkus.hibernate.reactive.panache.common.runtime.AbstractJpaOperations;
import io.quarkus.panache.common.Parameters;
import io.smallrye.mutiny.Uni;

/**
 * Utility class for Panache.
 *
 * @author Stéphane Épardaud
 */
public class Panache {

    /**
     * Returns the current {@link Mutiny.Session}
     * 
     * @return the current {@link Mutiny.Session}
     */
    public static Uni<Mutiny.Session> getSession() {
        return AbstractJpaOperations.getSession();
    }

    /**
     * Performs the given work within the scope of a database transaction, automatically flushing the session.
     * The transaction will be rolled back if the work completes with an uncaught exception, or if
     * {@link Mutiny.Transaction#markForRollback()} is called.
     * 
     * @param <T> The function's return type
     * @param work The function to execute in the new transaction
     * @return the result of executing the function
     */
    public static <T> Uni<T> withTransaction(Supplier<Uni<T>> work) {
        return getSession().flatMap(session -> session.withTransaction(t -> work.get()));
    }

    /**
     * Executes a database update operation and return the number of rows operated on.
     *
     * @param query a normal HQL query
     * @param params optional list of indexed parameters
     * @return the number of rows operated on.
     */
    public static Uni<Integer> executeUpdate(String query, Object... params) {
        return AbstractJpaOperations.executeUpdate(query, params);
    }

    /**
     * Executes a database update operation and return the number of rows operated on.
     *
     * @param query a normal HQL query
     * @param params {@link Map} of named parameters
     * @return the number of rows operated on.
     */
    public static Uni<Integer> executeUpdate(String query, Map<String, Object> params) {
        return AbstractJpaOperations.executeUpdate(query, params);
    }

    /**
     * Executes a database update operation and return the number of rows operated on.
     *
     * @param query a normal HQL query
     * @param params {@link Parameters} of named parameters
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
}
