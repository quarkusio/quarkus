package io.quarkus.data.hibernate.blocking;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import jakarta.data.metamodel.Attribute;
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

    @Override
    default <T> BlockingDataQuery<Entity> find(Attribute<T> attribute, Object value) {
        return value == null ? find(attribute.name() + " IS NULL") : find(attribute.name() + " = ?1", value);
    }

    @Override
    default <T> List<Entity> list(Attribute<T> attribute, Object value) {
        return value == null ? list(attribute.name() + " IS NULL") : list(attribute.name() + " = ?1", value);
    }

    /**
     * Find entities matching a query based on an entity attribute and a value.
     * This method accepts Jakarta Data metamodel attributes.
     * This method is a shortcut for <code>find(attribute, value).stream()</code>.
     * It requires a transaction to work.
     * Without a transaction, the underlying cursor can be closed before the end of the stream.
     *
     * @param <T> the entity type
     * @param attribute the entity attribute from the Jakarta Data metamodel
     * @param value the value to match
     * @return a {@link Stream} containing all results, without paging
     * @see #find(Attribute, Object)
     * @see #stream(String, Object...)
     */
    default <T> Stream<Entity> stream(Attribute<T> attribute, Object value) {
        return value == null ? stream(attribute.name() + " IS NULL") : stream(attribute.name() + " = ?1", value);
    }

    @Override
    default <T> Long count(Attribute<T> attribute, Object value) {
        return value == null ? count(attribute.name() + " IS NULL") : count(attribute.name() + " = ?1", value);
    }

    @Override
    default <T> Long delete(Attribute<T> attribute, Object value) {
        return value == null ? delete(attribute.name() + " IS NULL") : delete(attribute.name() + " = ?1", value);
    }
}
