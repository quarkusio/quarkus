package io.quarkus.data.hibernate.blocking;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import jakarta.persistence.LockModeType;

import io.quarkus.data.hibernate.RepositoryQueries;

public interface BlockingRepositoryQueries<Entity, Id>
        extends RepositoryQueries<Entity, List<Entity>, BlockingDataQuery<Entity>, Long, Boolean, Id> {

    // Queries

    /**
     * Find an entity of this type by ID.
     *
     * @param id the ID of the entity to find.
     * @return if found, an optional containing the entity, else <code>Optional.empty()</code>.
     */
    Optional<Entity> findByIdOptional(Id id);

    /**
     * Find an entity of this type by ID.
     *
     * @param id the ID of the entity to find.
     * @return if found, an optional containing the entity, else <code>Optional.empty()</code>.
     */
    Optional<Entity> findByIdOptional(Id id, LockModeType lockModeType);

    /**
     * Find entities matching a query, with optional indexed parameters.
     * This method is a shortcut for <code>find(query, params).stream()</code>.
     * It requires a transaction to work.
     * Without a transaction, the underlying cursor can be closed before the end of the stream.
     *
     * @param query a {@link io.quarkus.data.hibernate query string}
     * @param params optional sequence of indexed parameters
     * @return a {@link Stream} containing all results, without paging
     * @see #stream(String, Map)
     * @see #find(String, Object...)
     * @see #list(String, Object...)
     */
    Stream<Entity> stream(String query, Object... params);

    /**
     * Find entities matching a query, with named parameters.
     * This method is a shortcut for <code>find(query, params).stream()</code>.
     * It requires a transaction to work.
     * Without a transaction, the underlying cursor can be closed before the end of the stream.
     *
     * @param query a {@link io.quarkus.data.hibernate query string}
     * @param params {@link Map} of named parameters
     * @return a {@link Stream} containing all results, without paging
     * @see #stream(String, Object...)
     * @see #find(String, Map)
     * @see #list(String, Map)
     */
    Stream<Entity> stream(String query, Map<String, Object> params);

    /**
     * Find all entities of this type.
     * This method is a shortcut for <code>findAll().stream()</code>.
     * It requires a transaction to work.
     * Without a transaction, the underlying cursor can be closed before the end of the stream.
     *
     * @return a {@link Stream} containing all results, without paging
     * @see #findAll()
     * @see #listAll()
     */
    Stream<Entity> streamAll();
}
