package io.quarkus.hibernate.panache.blocking;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import jakarta.persistence.LockModeType;

import io.quarkus.hibernate.panache.PanacheRepositoryQueries;
import io.quarkus.panache.common.Sort;

public interface PanacheRepositoryBlockingQueries<Entity, Id>
        extends PanacheRepositoryQueries<Entity, List<Entity>, PanacheBlockingQuery<Entity>, Long, Boolean, Id> {

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
     * @param query a {@link io.quarkus.hibernate.panache query string}
     * @param params optional sequence of indexed parameters
     * @return a {@link Stream} containing all results, without paging
     * @see #stream(String, Sort, Object...)
     * @see #stream(String, Map)
     * @see #find(String, Object...)
     * @see #list(String, Object...)
     */
    Stream<Entity> stream(String query, Object... params);

    /**
     * Find entities matching a query and the given sort options, with optional indexed parameters.
     * This method is a shortcut for <code>find(query, sort, params).stream()</code>.
     * It requires a transaction to work.
     * Without a transaction, the underlying cursor can be closed before the end of the stream.
     *
     * @param query a {@link io.quarkus.hibernate.panache query string}
     * @param sort the sort strategy to use
     * @param params optional sequence of indexed parameters
     * @return a {@link Stream} containing all results, without paging
     * @see #stream(String, Object...)
     * @see #stream(String, Sort, Map)
     * @see #find(String, Sort, Object...)
     * @see #list(String, Sort, Object...)
     */
    Stream<Entity> stream(String query, Sort sort, Object... params);

    /**
     * Find entities matching a query, with named parameters.
     * This method is a shortcut for <code>find(query, params).stream()</code>.
     * It requires a transaction to work.
     * Without a transaction, the underlying cursor can be closed before the end of the stream.
     *
     * @param query a {@link io.quarkus.hibernate.panache query string}
     * @param params {@link Map} of named parameters
     * @return a {@link Stream} containing all results, without paging
     * @see #stream(String, Sort, Map)
     * @see #stream(String, Object...)
     * @see #find(String, Map)
     * @see #list(String, Map)
     */
    Stream<Entity> stream(String query, Map<String, Object> params);

    /**
     * Find entities matching a query and the given sort options, with named parameters.
     * This method is a shortcut for <code>find(query, sort, params).stream()</code>.
     * It requires a transaction to work.
     * Without a transaction, the underlying cursor can be closed before the end of the stream.
     *
     * @param query a {@link io.quarkus.hibernate.panache query string}
     * @param sort the sort strategy to use
     * @param params {@link Map} of indexed parameters
     * @return a {@link Stream} containing all results, without paging
     * @see #stream(String, Map)
     * @see #stream(String, Sort, Object...)
     * @see #find(String, Sort, Map)
     * @see #list(String, Sort, Map)
     */
    Stream<Entity> stream(String query, Sort sort, Map<String, Object> params);

    /**
     * Find all entities of this type.
     * This method is a shortcut for <code>findAll().stream()</code>.
     * It requires a transaction to work.
     * Without a transaction, the underlying cursor can be closed before the end of the stream.
     *
     * @return a {@link Stream} containing all results, without paging
     * @see #streamAll(Sort)
     * @see #findAll()
     * @see #listAll()
     */
    Stream<Entity> streamAll(Sort sort);

    /**
     * Find all entities of this type, in the given order.
     * This method is a shortcut for <code>findAll().stream()</code>.
     * It requires a transaction to work.
     * Without a transaction, the underlying cursor can be closed before the end of the stream.
     *
     * @return a {@link Stream} containing all results, without paging
     * @see #streamAll()
     * @see #findAll(Sort)
     * @see #listAll(Sort)
     */
    Stream<Entity> streamAll();
}
