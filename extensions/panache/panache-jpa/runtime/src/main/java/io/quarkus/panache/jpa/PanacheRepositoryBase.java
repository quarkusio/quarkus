package io.quarkus.panache.jpa;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import io.quarkus.panache.common.Parameters;
import io.quarkus.panache.common.Sort;
import io.quarkus.panache.jpa.impl.JpaOperations;

/**
 * <p>
 * Represents a Repository for a specific type of entity {@code Entity}, with an ID type
 * of {@code Id}. Implementing this repository will gain you the exact same useful methods
 * that are on {@link PanacheEntityBase}. Unless you have a custom ID strategy, you should not
 * implement this interface directly but implement {@link PanacheRepository} instead.
 * </p>
 *
 * @author Stéphane Épardaud
 * @param <Entity> The type of entity to operate on
 * @param <Id> The ID type of the entity
 */
public interface PanacheRepositoryBase<Entity, Id> {

    // Operations

    /**
     * Persist the given entity in the database, if not already persisted.
     * 
     * @param entity the entity to persist.
     * @see #isPersistent(Object)
     * @see #persist(Iterable)
     * @see #persist(Stream)
     * @see #persist(Object, Object...)
     */
    public default void persist(Entity entity) {
        JpaOperations.persist(entity);
    }

    /**
     * Delete the given entity from the database, if it is already persisted.
     * 
     * @param entity the entity to delete.
     * @see #isPersistent()
     * @see #delete(String, Object...)
     * @see #delete(String, Map)
     * @see #delete(String, Parameters)
     * @see #deleteAll()
     */
    public default void delete(Entity entity) {
        JpaOperations.delete(entity);
    }

    /**
     * Returns true if the given entity is persistent in the database. If yes, all modifications to
     * its persistent fields will be automatically committed to the database at transaction
     * commit time.
     * 
     * @param the entity to check
     * @return true if the entity is persistent in the database.
     */
    public default boolean isPersistent(Entity entity) {
        return JpaOperations.isPersistent(entity);
    }

    // Queries

    /**
     * Find an entity of this type by ID.
     * 
     * @param id the ID of the entity to find.
     * @return the entity found, or <code>null</code> if not found.
     */
    public default Entity findById(Id id) {
        throw JpaOperations.implementationInjectionMissing();
    }

    /**
     * Find entities using a query, with optional indexed parameters.
     * 
     * @param query a {@link io.quarkus.panache.jpa query string}
     * @param params optional sequence of indexed parameters
     * @return a new {@link PanacheQuery} instance for the given query
     * @see #find(String, Sort, Object...)
     * @see #find(String, Map)
     * @see #find(String, Parameters)
     * @see #list(String, Object...)
     * @see #stream(String, Object...)
     */
    public default PanacheQuery<Entity> find(String query, Object... params) {
        throw JpaOperations.implementationInjectionMissing();
    }

    /**
     * Find entities using a query and the given sort options, with optional indexed parameters.
     * 
     * @param query a {@link io.quarkus.panache.jpa query string}
     * @param sort the sort strategy to use
     * @param params optional sequence of indexed parameters
     * @return a new {@link PanacheQuery} instance for the given query
     * @see #find(String, Object...)
     * @see #find(String, Sort, Map)
     * @see #find(String, Sort, Parameters)
     * @see #list(String, Sort, Object...)
     * @see #stream(String, Sort, Object...)
     */
    public default PanacheQuery<Entity> find(String query, Sort sort, Object... params) {
        throw JpaOperations.implementationInjectionMissing();
    }

    /**
     * Find entities using a query, with named parameters.
     * 
     * @param query a {@link io.quarkus.panache.jpa query string}
     * @param params {@link Map} of named parameters
     * @return a new {@link PanacheQuery} instance for the given query
     * @see #find(String, Sort, Map)
     * @see #find(String, Object...)
     * @see #find(String, Parameters)
     * @see #list(String, Map)
     * @see #stream(String, Map)
     */
    public default PanacheQuery<Entity> find(String query, Map<String, Object> params) {
        throw JpaOperations.implementationInjectionMissing();
    }

    /**
     * Find entities using a query and the given sort options, with named parameters.
     * 
     * @param query a {@link io.quarkus.panache.jpa query string}
     * @param sort the sort strategy to use
     * @param params {@link Map} of indexed parameters
     * @return a new {@link PanacheQuery} instance for the given query
     * @see #find(String, Map)
     * @see #find(String, Sort, Object...)
     * @see #find(String, Sort, Parameters)
     * @see #list(String, Sort, Map)
     * @see #stream(String, Sort, Map)
     */
    public default PanacheQuery<Entity> find(String query, Sort sort, Map<String, Object> params) {
        throw JpaOperations.implementationInjectionMissing();
    }

    /**
     * Find entities using a query, with named parameters.
     * 
     * @param query a {@link io.quarkus.panache.jpa query string}
     * @param params {@link Parameters} of named parameters
     * @return a new {@link PanacheQuery} instance for the given query
     * @see #find(String, Sort, Parameters)
     * @see #find(String, Map)
     * @see #find(String, Parameters)
     * @see #list(String, Parameters)
     * @see #stream(String, Parameters)
     */
    public default PanacheQuery<Entity> find(String query, Parameters params) {
        throw JpaOperations.implementationInjectionMissing();
    }

    /**
     * Find entities using a query and the given sort options, with named parameters.
     * 
     * @param query a {@link io.quarkus.panache.jpa query string}
     * @param sort the sort strategy to use
     * @param params {@link Parameters} of indexed parameters
     * @return a new {@link PanacheQuery} instance for the given query
     * @see #find(String, Parameters)
     * @see #find(String, Sort, Map)
     * @see #find(String, Sort, Parameters)
     * @see #list(String, Sort, Parameters)
     * @see #stream(String, Sort, Parameters)
     */
    public default PanacheQuery<Entity> find(String query, Sort sort, Parameters params) {
        throw JpaOperations.implementationInjectionMissing();
    }

    /**
     * Find all entities of this type.
     * 
     * @return a new {@link PanacheQuery} instance to find all entities of this type.
     * @see #findAll(Sort)
     * @see #listAll()
     * @see #streamAll()
     */
    public default PanacheQuery<Entity> findAll() {
        throw JpaOperations.implementationInjectionMissing();
    }

    /**
     * Find all entities of this type, in the given order.
     * 
     * @param sort the sort order to use
     * @return a new {@link PanacheQuery} instance to find all entities of this type.
     * @see #findAll()
     * @see #listAll(Sort)
     * @see #streamAll(Sort)
     */
    public default PanacheQuery<Entity> findAll(Sort sort) {
        throw JpaOperations.implementationInjectionMissing();
    }

    /**
     * Find entities matching a query, with optional indexed parameters.
     * This method is a shortcut for <code>find(query, params).list()</code>.
     * 
     * @param query a {@link io.quarkus.panache.jpa query string}
     * @param params optional sequence of indexed parameters
     * @return a {@link List} containing all results, without paging
     * @see #list(String, Sort, Object...)
     * @see #list(String, Map)
     * @see #list(String, Parameters)
     * @see #find(String, Object...)
     * @see #stream(String, Object...)
     */
    public default List<Entity> list(String query, Object... params) {
        throw JpaOperations.implementationInjectionMissing();
    }

    /**
     * Find entities matching a query and the given sort options, with optional indexed parameters.
     * This method is a shortcut for <code>find(query, sort, params).list()</code>.
     * 
     * @param query a {@link io.quarkus.panache.jpa query string}
     * @param sort the sort strategy to use
     * @param params optional sequence of indexed parameters
     * @return a {@link List} containing all results, without paging
     * @see #list(String, Object...)
     * @see #list(String, Sort, Map)
     * @see #list(String, Sort, Parameters)
     * @see #find(String, Sort, Object...)
     * @see #stream(String, Sort, Object...)
     */
    public default List<Entity> list(String query, Sort sort, Object... params) {
        throw JpaOperations.implementationInjectionMissing();
    }

    /**
     * Find entities matching a query, with named parameters.
     * This method is a shortcut for <code>find(query, params).list()</code>.
     * 
     * @param query a {@link io.quarkus.panache.jpa query string}
     * @param params {@link Map} of named parameters
     * @return a {@link List} containing all results, without paging
     * @see #list(String, Sort, Map)
     * @see #list(String, Object...)
     * @see #list(String, Parameters)
     * @see #find(String, Map)
     * @see #stream(String, Map)
     */
    public default List<Entity> list(String query, Map<String, Object> params) {
        throw JpaOperations.implementationInjectionMissing();
    }

    /**
     * Find entities matching a query and the given sort options, with named parameters.
     * This method is a shortcut for <code>find(query, sort, params).list()</code>.
     * 
     * @param query a {@link io.quarkus.panache.jpa query string}
     * @param sort the sort strategy to use
     * @param params {@link Map} of indexed parameters
     * @return a {@link List} containing all results, without paging
     * @see #list(String, Map)
     * @see #list(String, Sort, Object...)
     * @see #list(String, Sort, Parameters)
     * @see #find(String, Sort, Map)
     * @see #stream(String, Sort, Map)
     */
    public default List<Entity> list(String query, Sort sort, Map<String, Object> params) {
        throw JpaOperations.implementationInjectionMissing();
    }

    /**
     * Find entities matching a query, with named parameters.
     * This method is a shortcut for <code>find(query, params).list()</code>.
     * 
     * @param query a {@link io.quarkus.panache.jpa query string}
     * @param params {@link Parameters} of named parameters
     * @return a {@link List} containing all results, without paging
     * @see #list(String, Sort, Parameters)
     * @see #list(String, Object...)
     * @see #list(String, Map)
     * @see #find(String, Parameters)
     * @see #stream(String, Parameters)
     */
    public default List<Entity> list(String query, Parameters params) {
        throw JpaOperations.implementationInjectionMissing();
    }

    /**
     * Find entities matching a query and the given sort options, with named parameters.
     * This method is a shortcut for <code>find(query, sort, params).list()</code>.
     * 
     * @param query a {@link io.quarkus.panache.jpa query string}
     * @param sort the sort strategy to use
     * @param params {@link Parameters} of indexed parameters
     * @return a {@link List} containing all results, without paging
     * @see #list(String, Parameters)
     * @see #list(String, Sort, Object...)
     * @see #list(String, Sort, Map)
     * @see #find(String, Sort, Parameters)
     * @see #stream(String, Sort, Parameters)
     */
    public default List<Entity> list(String query, Sort sort, Parameters params) {
        throw JpaOperations.implementationInjectionMissing();
    }

    /**
     * Find all entities of this type.
     * This method is a shortcut for <code>findAll().list()</code>.
     * 
     * @return a {@link List} containing all results, without paging
     * @see #listAll(Sort)
     * @see #findAll()
     * @see #streamAll()
     */
    public default List<Entity> listAll() {
        throw JpaOperations.implementationInjectionMissing();
    }

    /**
     * Find all entities of this type, in the given order.
     * This method is a shortcut for <code>findAll(sort).list()</code>.
     * 
     * @param sort the sort order to use
     * @return a {@link List} containing all results, without paging
     * @see #listAll()
     * @see #findAll(Sort)
     * @see #streamAll(Sort)
     */
    public default List<Entity> listAll(Sort sort) {
        throw JpaOperations.implementationInjectionMissing();
    }

    /**
     * Find entities matching a query, with optional indexed parameters.
     * This method is a shortcut for <code>find(query, params).stream()</code>.
     * 
     * @param query a {@link io.quarkus.panache.jpa query string}
     * @param params optional sequence of indexed parameters
     * @return a {@link Stream} containing all results, without paging
     * @see #stream(String, Sort, Object...)
     * @see #stream(String, Map)
     * @see #stream(String, Parameters)
     * @see #find(String, Object...)
     * @see #list(String, Object...)
     */
    public default Stream<Entity> stream(String query, Object... params) {
        throw JpaOperations.implementationInjectionMissing();
    }

    /**
     * Find entities matching a query and the given sort options, with optional indexed parameters.
     * This method is a shortcut for <code>find(query, sort, params).stream()</code>.
     * 
     * @param query a {@link io.quarkus.panache.jpa query string}
     * @param sort the sort strategy to use
     * @param params optional sequence of indexed parameters
     * @return a {@link Stream} containing all results, without paging
     * @see #stream(String, Object...)
     * @see #stream(String, Sort, Map)
     * @see #stream(String, Sort, Parameters)
     * @see #find(String, Sort, Object...)
     * @see #list(String, Sort, Object...)
     */
    public default Stream<Entity> stream(String query, Sort sort, Object... params) {
        throw JpaOperations.implementationInjectionMissing();
    }

    /**
     * Find entities matching a query, with named parameters.
     * This method is a shortcut for <code>find(query, params).stream()</code>.
     * 
     * @param query a {@link io.quarkus.panache.jpa query string}
     * @param params {@link Map} of named parameters
     * @return a {@link Stream} containing all results, without paging
     * @see #stream(String, Sort, Map)
     * @see #stream(String, Object...)
     * @see #stream(String, Parameters)
     * @see #find(String, Map)
     * @see #list(String, Map)
     */
    public default Stream<Entity> stream(String query, Map<String, Object> params) {
        throw JpaOperations.implementationInjectionMissing();
    }

    /**
     * Find entities matching a query and the given sort options, with named parameters.
     * This method is a shortcut for <code>find(query, sort, params).stream()</code>.
     * 
     * @param query a {@link io.quarkus.panache.jpa query string}
     * @param sort the sort strategy to use
     * @param params {@link Map} of indexed parameters
     * @return a {@link Stream} containing all results, without paging
     * @see #stream(String, Map)
     * @see #stream(String, Sort, Object...)
     * @see #stream(String, Sort, Parameters)
     * @see #find(String, Sort, Map)
     * @see #list(String, Sort, Map)
     */
    public default Stream<Entity> stream(String query, Sort sort, Map<String, Object> params) {
        throw JpaOperations.implementationInjectionMissing();
    }

    /**
     * Find entities matching a query, with named parameters.
     * This method is a shortcut for <code>find(query, params).stream()</code>.
     * 
     * @param query a {@link io.quarkus.panache.jpa query string}
     * @param params {@link Parameters} of named parameters
     * @return a {@link Stream} containing all results, without paging
     * @see #stream(String, Sort, Parameters)
     * @see #stream(String, Object...)
     * @see #stream(String, Map)
     * @see #find(String, Parameters)
     * @see #list(String, Parameters)
     */
    public default Stream<Entity> stream(String query, Parameters params) {
        throw JpaOperations.implementationInjectionMissing();
    }

    /**
     * Find entities matching a query and the given sort options, with named parameters.
     * This method is a shortcut for <code>find(query, sort, params).stream()</code>.
     * 
     * @param query a {@link io.quarkus.panache.jpa query string}
     * @param sort the sort strategy to use
     * @param params {@link Parameters} of indexed parameters
     * @return a {@link Stream} containing all results, without paging
     * @see #stream(String, Parameters)
     * @see #stream(String, Sort, Object...)
     * @see #stream(String, Sort, Map)
     * @see #find(String, Sort, Parameters)
     * @see #list(String, Sort, Parameters)
     */
    public default Stream<Entity> stream(String query, Sort sort, Parameters params) {
        throw JpaOperations.implementationInjectionMissing();
    }

    /**
     * Find all entities of this type.
     * This method is a shortcut for <code>findAll().stream()</code>.
     * 
     * @return a {@link Stream} containing all results, without paging
     * @see #streamAll(Sort)
     * @see #findAll()
     * @see #listAll()
     */
    public default Stream<Entity> streamAll(Sort sort) {
        throw JpaOperations.implementationInjectionMissing();
    }

    /**
     * Find all entities of this type, in the given order.
     * This method is a shortcut for <code>findAll(sort).stream()</code>.
     * 
     * @param sort the sort order to use
     * @return a {@link Stream} containing all results, without paging
     * @see #streamAll()
     * @see #findAll(Sort)
     * @see #listAll(Sort)
     */
    public default Stream<Entity> streamAll() {
        throw JpaOperations.implementationInjectionMissing();
    }

    /**
     * Counts the number of this type of entity in the database.
     * 
     * @return the number of this type of entity in the database.
     * @see #count(String, Object...)
     * @see #count(String, Map)
     * @see #count(String, Parameters)
     */
    public default long count() {
        throw JpaOperations.implementationInjectionMissing();
    }

    /**
     * Counts the number of this type of entity matching the given query, with optional indexed parameters.
     * 
     * @param query a {@link io.quarkus.panache.jpa query string}
     * @param params optional sequence of indexed parameters
     * @return the number of entities counted.
     * @see #count()
     * @see #count(String, Map)
     * @see #count(String, Parameters)
     */
    public default long count(String query, Object... params) {
        throw JpaOperations.implementationInjectionMissing();
    }

    /**
     * Counts the number of this type of entity matching the given query, with named parameters.
     * 
     * @param query a {@link io.quarkus.panache.jpa query string}
     * @param params {@link Map} of named parameters
     * @return the number of entities counted.
     * @see #count()
     * @see #count(String, Object...)
     * @see #count(String, Parameters)
     */
    public default long count(String query, Map<String, Object> params) {
        throw JpaOperations.implementationInjectionMissing();
    }

    /**
     * Counts the number of this type of entity matching the given query, with named parameters.
     * 
     * @param query a {@link io.quarkus.panache.jpa query string}
     * @param params {@link Parameters} of named parameters
     * @return the number of entities counted.
     * @see #count()
     * @see #count(String, Object...)
     * @see #count(String, Map)
     */
    public default long count(String query, Parameters params) {
        throw JpaOperations.implementationInjectionMissing();
    }

    /**
     * Delete all entities of this type from the database.
     * 
     * @return the number of entities deleted.
     * @see #delete(String, Object...)
     * @see #delete(String, Map)
     * @see #delete(String, Parameters)
     */
    public default long deleteAll() {
        throw JpaOperations.implementationInjectionMissing();
    }

    /**
     * Delete all entities of this type matching the given query, with optional indexed parameters.
     * 
     * @param query a {@link io.quarkus.panache.jpa query string}
     * @param params optional sequence of indexed parameters
     * @return the number of entities deleted.
     * @see #deleteAll()
     * @see #delete(String, Map)
     * @see #delete(String, Parameters)
     */
    public default long delete(String query, Object... params) {
        throw JpaOperations.implementationInjectionMissing();
    }

    /**
     * Delete all entities of this type matching the given query, with named parameters.
     * 
     * @param query a {@link io.quarkus.panache.jpa query string}
     * @param params {@link Map} of named parameters
     * @return the number of entities deleted.
     * @see #deleteAll()
     * @see #delete(String, Object...)
     * @see #delete(String, Parameters)
     */
    public default long delete(String query, Map<String, Object> params) {
        throw JpaOperations.implementationInjectionMissing();
    }

    /**
     * Delete all entities of this type matching the given query, with named parameters.
     * 
     * @param query a {@link io.quarkus.panache.jpa query string}
     * @param params {@link Parameters} of named parameters
     * @return the number of entities deleted.
     * @see #deleteAll()
     * @see #delete(String, Object...)
     * @see #delete(String, Map)
     */
    public default long delete(String query, Parameters params) {
        throw JpaOperations.implementationInjectionMissing();
    }

    /**
     * Persist all given entities.
     * 
     * @param entities the entities to persist
     * @see #persist()
     * @see #persist(Stream)
     * @see #persist(Object,Object...)
     */
    public default void persist(Iterable<Entity> entities) {
        JpaOperations.persist(entities);
    }

    /**
     * Persist all given entities.
     * 
     * @param entities the entities to persist
     * @see #persist()
     * @see #persist(Iterable)
     * @see #persist(Object,Object...)
     */
    public default void persist(Stream<Entity> entities) {
        JpaOperations.persist(entities);
    }

    /**
     * Persist all given entities.
     * 
     * @param entities the entities to persist
     * @see #persist()
     * @see #persist(Stream)
     * @see #persist(Iterable)
     */
    public default void persist(Entity firstEntity, @SuppressWarnings("unchecked") Entity... entities) {
        JpaOperations.persist(firstEntity, entities);
    }
}
