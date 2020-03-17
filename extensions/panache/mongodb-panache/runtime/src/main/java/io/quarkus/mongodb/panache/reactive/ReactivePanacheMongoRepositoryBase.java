package io.quarkus.mongodb.panache.reactive;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.bson.Document;

import io.quarkus.mongodb.panache.reactive.runtime.ReactiveMongoOperations;
import io.quarkus.mongodb.panache.runtime.MongoOperations;
import io.quarkus.mongodb.reactive.ReactiveMongoCollection;
import io.quarkus.mongodb.reactive.ReactiveMongoDatabase;
import io.quarkus.panache.common.Parameters;
import io.quarkus.panache.common.Sort;
import io.quarkus.panache.common.impl.GenerateBridge;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

/**
 * Represents a Repository for a specific type of entity {@code Entity}, with an ID type
 * of {@code Id}. Implementing this repository will gain you the exact same useful methods
 * that are on {@link ReactivePanacheMongoEntityBase}. Unless you have a custom ID strategy, you should not
 * implement this interface directly but implement {@link ReactivePanacheMongoRepository} instead.
 *
 * @param <Entity> The type of entity to operate on
 * @param <Id> The ID type of the entity
 * @see ReactivePanacheMongoRepository
 */
public interface ReactivePanacheMongoRepositoryBase<Entity, Id> {

    // Operations

    /**
     * Persist the given entity in the database.
     * This will set it's ID field if not already set.
     * 
     * @param entity the entity to insert.
     * @see #persist(Iterable)
     * @see #persist(Stream)
     * @see #persist(Object, Object...)
     */
    public default Uni<Void> persist(Entity entity) {
        return ReactiveMongoOperations.persist(entity);
    }

    /**
     * Update the given entity in the database.
     *
     * @param entity the entity to update.
     * @see #update(Iterable)
     * @see #update(Stream)
     * @see #update(Object, Object...)
     */
    public default Uni<Void> update(Entity entity) {
        return ReactiveMongoOperations.update(entity);
    }

    /**
     * Persist the given entity in the database or update it if it already exist.
     *
     * @param entity the entity to update.
     * @see #persistOrUpdate(Iterable)
     * @see #persistOrUpdate(Stream)
     * @see #persistOrUpdate(Object, Object...)
     */
    public default Uni<Void> persistOrUpdate(Entity entity) {
        return ReactiveMongoOperations.persistOrUpdate(entity);
    }

    /**
     * Delete the given entity from the database, if it is already persisted.
     * 
     * @param entity the entity to delete.
     * @see #delete(String, Object...)
     * @see #delete(String, Map)
     * @see #delete(String, Parameters)
     * @see #deleteAll()
     */
    public default Uni<Void> delete(Entity entity) {
        return ReactiveMongoOperations.delete(entity);
    }

    // Queries

    /**
     * Find an entity of this type by ID.
     * 
     * @param id the ID of the entity to find.
     * @return the entity found, or <code>null</code> if not found.
     */
    @GenerateBridge
    public default Uni<Entity> findById(Id id) {
        throw ReactiveMongoOperations.implementationInjectionMissing();
    }

    /**
     * Find an entity of this type by ID.
     *
     * @param id the ID of the entity to find.
     * @return if found, an optional containing the entity, else <code>Optional.empty()</code>.
     */
    @GenerateBridge
    public default Uni<Optional<Entity>> findByIdOptional(Object id) {
        throw ReactiveMongoOperations.implementationInjectionMissing();
    }

    /**
     * Find entities using a query, with optional indexed parameters.
     * 
     * @param query a {@link io.quarkus.mongodb.panache query string}
     * @param params optional sequence of indexed parameters
     * @return a new {@link ReactivePanacheQuery} instance for the given query
     * @see #find(String, Sort, Object...)
     * @see #find(String, Map)
     * @see #find(String, Parameters)
     * @see #list(String, Object...)
     * @see #stream(String, Object...)
     */
    @GenerateBridge
    public default ReactivePanacheQuery<Entity> find(String query, Object... params) {
        throw ReactiveMongoOperations.implementationInjectionMissing();
    }

    /**
     * Find entities using a query and the given sort options, with optional indexed parameters.
     * 
     * @param query a {@link io.quarkus.mongodb.panache query string}
     * @param sort the sort strategy to use
     * @param params optional sequence of indexed parameters
     * @return a new {@link ReactivePanacheQuery} instance for the given query
     * @see #find(String, Object...)
     * @see #find(String, Sort, Map)
     * @see #find(String, Sort, Parameters)
     * @see #list(String, Sort, Object...)
     * @see #stream(String, Sort, Object...)
     */
    @GenerateBridge
    public default ReactivePanacheQuery<Entity> find(String query, Sort sort, Object... params) {
        throw ReactiveMongoOperations.implementationInjectionMissing();
    }

    /**
     * Find entities using a query, with named parameters.
     * 
     * @param query a {@link io.quarkus.mongodb.panache query string}
     * @param params {@link Map} of named parameters
     * @return a new {@link ReactivePanacheQuery} instance for the given query
     * @see #find(String, Sort, Map)
     * @see #find(String, Object...)
     * @see #find(String, Parameters)
     * @see #list(String, Map)
     * @see #stream(String, Map)
     */
    @GenerateBridge
    public default ReactivePanacheQuery<Entity> find(String query, Map<String, Object> params) {
        throw ReactiveMongoOperations.implementationInjectionMissing();
    }

    /**
     * Find entities using a query and the given sort options, with named parameters.
     * 
     * @param query a {@link io.quarkus.mongodb.panache query string}
     * @param sort the sort strategy to use
     * @param params {@link Map} of indexed parameters
     * @return a new {@link ReactivePanacheQuery} instance for the given query
     * @see #find(String, Map)
     * @see #find(String, Sort, Object...)
     * @see #find(String, Sort, Parameters)
     * @see #list(String, Sort, Map)
     * @see #stream(String, Sort, Map)
     */
    @GenerateBridge
    public default ReactivePanacheQuery<Entity> find(String query, Sort sort, Map<String, Object> params) {
        throw ReactiveMongoOperations.implementationInjectionMissing();
    }

    /**
     * Find entities using a query, with named parameters.
     * 
     * @param query a {@link io.quarkus.mongodb.panache query string}
     * @param params {@link Parameters} of named parameters
     * @return a new {@link ReactivePanacheQuery} instance for the given query
     * @see #find(String, Sort, Parameters)
     * @see #find(String, Map)
     * @see #find(String, Parameters)
     * @see #list(String, Parameters)
     * @see #stream(String, Parameters)
     */
    @GenerateBridge
    public default ReactivePanacheQuery<Entity> find(String query, Parameters params) {
        throw ReactiveMongoOperations.implementationInjectionMissing();
    }

    /**
     * Find entities using a query and the given sort options, with named parameters.
     * 
     * @param query a {@link io.quarkus.mongodb.panache query string}
     * @param sort the sort strategy to use
     * @param params {@link Parameters} of indexed parameters
     * @return a new {@link ReactivePanacheQuery} instance for the given query
     * @see #find(String, Parameters)
     * @see #find(String, Sort, Map)
     * @see #find(String, Sort, Parameters)
     * @see #list(String, Sort, Parameters)
     * @see #stream(String, Sort, Parameters)
     */
    @GenerateBridge
    public default ReactivePanacheQuery<Entity> find(String query, Sort sort, Parameters params) {
        throw ReactiveMongoOperations.implementationInjectionMissing();
    }

    /**
     * Find entities using a BSON query.
     *
     * @param query a {@link Document} query
     * @return a new {@link ReactivePanacheQuery} instance for the given query
     * @see #find(String, Parameters)
     * @see #find(String, Sort, Map)
     * @see #find(String, Sort, Parameters)
     * @see #list(String, Sort, Parameters)
     * @see #stream(String, Sort, Parameters)
     */
    @GenerateBridge
    public default ReactivePanacheQuery<Entity> find(Document query) {
        throw ReactiveMongoOperations.implementationInjectionMissing();
    }

    /**
     * Find entities using a a BSON query and a BSON sort.
     *
     * @param query a {@link Document} query
     * @param sort the {@link Document} sort
     * @return a new {@link ReactivePanacheQuery} instance for the given query
     * @see #find(String, Parameters)
     * @see #find(String, Sort, Map)
     * @see #find(String, Sort, Parameters)
     * @see #list(String, Sort, Parameters)
     * @see #stream(String, Sort, Parameters)
     */
    @GenerateBridge
    public default ReactivePanacheQuery<Entity> find(Document query, Document sort) {
        throw ReactiveMongoOperations.implementationInjectionMissing();
    }

    /**
     * Find all entities of this type.
     *
     * @return a new {@link ReactivePanacheQuery} instance to find all entities of this type.
     * @see #findAll(Sort)
     * @see #listAll()
     * @see #streamAll()
     */
    @GenerateBridge
    public default ReactivePanacheQuery<Entity> findAll() {
        throw ReactiveMongoOperations.implementationInjectionMissing();
    }

    /**
     * Find all entities of this type, in the given order.
     *
     * @param sort the sort order to use
     * @return a new {@link ReactivePanacheQuery} instance to find all entities of this type.
     * @see #findAll()
     * @see #listAll(Sort)
     * @see #streamAll(Sort)
     */
    @GenerateBridge
    public default ReactivePanacheQuery<Entity> findAll(Sort sort) {
        throw ReactiveMongoOperations.implementationInjectionMissing();
    }

    /**
     * Find entities matching a query, with optional indexed parameters.
     * This method is a shortcut for <code>find(query, params).list()</code>.
     *
     * @param query a {@link io.quarkus.mongodb.panache query string}
     * @param params optional sequence of indexed parameters
     * @return a {@link List} containing all results, without paging
     * @see #list(String, Sort, Object...)
     * @see #list(String, Map)
     * @see #list(String, Parameters)
     * @see #find(String, Object...)
     * @see #stream(String, Object...)
     */
    @GenerateBridge
    public default Uni<List<Entity>> list(String query, Object... params) {
        throw ReactiveMongoOperations.implementationInjectionMissing();
    }

    /**
     * Find entities matching a query and the given sort options, with optional indexed parameters.
     * This method is a shortcut for <code>find(query, sort, params).list()</code>.
     *
     * @param query a {@link io.quarkus.mongodb.panache query string}
     * @param sort the sort strategy to use
     * @param params optional sequence of indexed parameters
     * @return a {@link List} containing all results, without paging
     * @see #list(String, Object...)
     * @see #list(String, Sort, Map)
     * @see #list(String, Sort, Parameters)
     * @see #find(String, Sort, Object...)
     * @see #stream(String, Sort, Object...)
     */
    @GenerateBridge
    public default Uni<List<Entity>> list(String query, Sort sort, Object... params) {
        throw ReactiveMongoOperations.implementationInjectionMissing();
    }

    /**
     * Find entities matching a query, with named parameters.
     * This method is a shortcut for <code>find(query, params).list()</code>.
     *
     * @param query a {@link io.quarkus.mongodb.panache query string}
     * @param params {@link Map} of named parameters
     * @return a {@link List} containing all results, without paging
     * @see #list(String, Sort, Map)
     * @see #list(String, Object...)
     * @see #list(String, Parameters)
     * @see #find(String, Map)
     * @see #stream(String, Map)
     */
    @GenerateBridge
    public default Uni<List<Entity>> list(String query, Map<String, Object> params) {
        throw ReactiveMongoOperations.implementationInjectionMissing();
    }

    /**
     * Find entities matching a query and the given sort options, with named parameters.
     * This method is a shortcut for <code>find(query, sort, params).list()</code>.
     *
     * @param query a {@link io.quarkus.mongodb.panache query string}
     * @param sort the sort strategy to use
     * @param params {@link Map} of indexed parameters
     * @return a {@link List} containing all results, without paging
     * @see #list(String, Map)
     * @see #list(String, Sort, Object...)
     * @see #list(String, Sort, Parameters)
     * @see #find(String, Sort, Map)
     * @see #stream(String, Sort, Map)
     */
    @GenerateBridge
    public default Uni<List<Entity>> list(String query, Sort sort, Map<String, Object> params) {
        throw ReactiveMongoOperations.implementationInjectionMissing();
    }

    /**
     * Find entities matching a query, with named parameters.
     * This method is a shortcut for <code>find(query, params).list()</code>.
     *
     * @param query a {@link io.quarkus.mongodb.panache query string}
     * @param params {@link Parameters} of named parameters
     * @return a {@link List} containing all results, without paging
     * @see #list(String, Sort, Parameters)
     * @see #list(String, Object...)
     * @see #list(String, Map)
     * @see #find(String, Parameters)
     * @see #stream(String, Parameters)
     */
    @GenerateBridge
    public default Uni<List<Entity>> list(String query, Parameters params) {
        throw ReactiveMongoOperations.implementationInjectionMissing();
    }

    /**
     * Find entities matching a query and the given sort options, with named parameters.
     * This method is a shortcut for <code>find(query, sort, params).list()</code>.
     *
     * @param query a {@link io.quarkus.mongodb.panache query string}
     * @param sort the sort strategy to use
     * @param params {@link Parameters} of indexed parameters
     * @return a {@link List} containing all results, without paging
     * @see #list(String, Parameters)
     * @see #list(String, Sort, Object...)
     * @see #list(String, Sort, Map)
     * @see #find(String, Sort, Parameters)
     * @see #stream(String, Sort, Parameters)
     */
    @GenerateBridge
    public default Uni<List<Entity>> list(String query, Sort sort, Parameters params) {
        throw ReactiveMongoOperations.implementationInjectionMissing();
    }

    /**
     * Find entities using a BSON query.
     * This method is a shortcut for <code>find(query).list()</code>.
     *
     * @param query a {@link Document} query
     * @return a new {@link ReactivePanacheQuery} instance for the given query
     * @see #find(String, Parameters)
     * @see #find(String, Sort, Map)
     * @see #find(String, Sort, Parameters)
     * @see #list(String, Sort, Parameters)
     * @see #stream(String, Sort, Parameters)
     */
    @GenerateBridge
    public default ReactivePanacheQuery<Entity> list(Document query) {
        throw ReactiveMongoOperations.implementationInjectionMissing();
    }

    /**
     * Find entities using a a BSON query and a BSON sort.
     * This method is a shortcut for <code>find(query, sort).list()</code>.
     *
     * @param query a {@link Document} query
     * @param sort the {@link Document} sort
     * @return a new {@link ReactivePanacheQuery} instance for the given query
     * @see #find(String, Parameters)
     * @see #find(String, Sort, Map)
     * @see #find(String, Sort, Parameters)
     * @see #list(String, Sort, Parameters)
     * @see #stream(String, Sort, Parameters)
     */
    @GenerateBridge
    public default ReactivePanacheQuery<Entity> list(Document query, Document sort) {
        throw ReactiveMongoOperations.implementationInjectionMissing();
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
    @GenerateBridge
    public default Uni<List<Entity>> listAll() {
        throw ReactiveMongoOperations.implementationInjectionMissing();
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
    @GenerateBridge
    public default Uni<List<Entity>> listAll(Sort sort) {
        throw ReactiveMongoOperations.implementationInjectionMissing();
    }

    /**
     * Find entities matching a query, with optional indexed parameters.
     * This method is a shortcut for <code>find(query, params).stream()</code>.
     *
     * @param query a {@link io.quarkus.mongodb.panache query string}
     * @param params optional sequence of indexed parameters
     * @return a {@link Stream} containing all results, without paging
     * @see #stream(String, Sort, Object...)
     * @see #stream(String, Map)
     * @see #stream(String, Parameters)
     * @see #find(String, Object...)
     * @see #list(String, Object...)
     */
    @GenerateBridge
    public default Multi<Entity> stream(String query, Object... params) {
        throw ReactiveMongoOperations.implementationInjectionMissing();
    }

    /**
     * Find entities matching a query and the given sort options, with optional indexed parameters.
     * This method is a shortcut for <code>find(query, sort, params).stream()</code>.
     *
     * @param query a {@link io.quarkus.mongodb.panache query string}
     * @param sort the sort strategy to use
     * @param params optional sequence of indexed parameters
     * @return a {@link Stream} containing all results, without paging
     * @see #stream(String, Object...)
     * @see #stream(String, Sort, Map)
     * @see #stream(String, Sort, Parameters)
     * @see #find(String, Sort, Object...)
     * @see #list(String, Sort, Object...)
     */
    @GenerateBridge
    public default Multi<Entity> stream(String query, Sort sort, Object... params) {
        throw ReactiveMongoOperations.implementationInjectionMissing();
    }

    /**
     * Find entities matching a query, with named parameters.
     * This method is a shortcut for <code>find(query, params).stream()</code>.
     *
     * @param query a {@link io.quarkus.mongodb.panache query string}
     * @param params {@link Map} of named parameters
     * @return a {@link Stream} containing all results, without paging
     * @see #stream(String, Sort, Map)
     * @see #stream(String, Object...)
     * @see #stream(String, Parameters)
     * @see #find(String, Map)
     * @see #list(String, Map)
     */
    @GenerateBridge
    public default Multi<Entity> stream(String query, Map<String, Object> params) {
        throw ReactiveMongoOperations.implementationInjectionMissing();
    }

    /**
     * Find entities matching a query and the given sort options, with named parameters.
     * This method is a shortcut for <code>find(query, sort, params).stream()</code>.
     *
     * @param query a {@link io.quarkus.mongodb.panache query string}
     * @param sort the sort strategy to use
     * @param params {@link Map} of indexed parameters
     * @return a {@link Stream} containing all results, without paging
     * @see #stream(String, Map)
     * @see #stream(String, Sort, Object...)
     * @see #stream(String, Sort, Parameters)
     * @see #find(String, Sort, Map)
     * @see #list(String, Sort, Map)
     */
    @GenerateBridge
    public default Multi<Entity> stream(String query, Sort sort, Map<String, Object> params) {
        throw ReactiveMongoOperations.implementationInjectionMissing();
    }

    /**
     * Find entities matching a query, with named parameters.
     * This method is a shortcut for <code>find(query, params).stream()</code>.
     *
     * @param query a {@link io.quarkus.mongodb.panache query string}
     * @param params {@link Parameters} of named parameters
     * @return a {@link Stream} containing all results, without paging
     * @see #stream(String, Sort, Parameters)
     * @see #stream(String, Object...)
     * @see #stream(String, Map)
     * @see #find(String, Parameters)
     * @see #list(String, Parameters)
     */
    @GenerateBridge
    public default Multi<Entity> stream(String query, Parameters params) {
        throw ReactiveMongoOperations.implementationInjectionMissing();
    }

    /**
     * Find entities matching a query and the given sort options, with named parameters.
     * This method is a shortcut for <code>find(query, sort, params).stream()</code>.
     *
     * @param query a {@link io.quarkus.mongodb.panache query string}
     * @param sort the sort strategy to use
     * @param params {@link Parameters} of indexed parameters
     * @return a {@link Stream} containing all results, without paging
     * @see #stream(String, Parameters)
     * @see #stream(String, Sort, Object...)
     * @see #stream(String, Sort, Map)
     * @see #find(String, Sort, Parameters)
     * @see #list(String, Sort, Parameters)
     */
    @GenerateBridge
    public default Multi<Entity> stream(String query, Sort sort, Parameters params) {
        throw ReactiveMongoOperations.implementationInjectionMissing();
    }

    /**
     * Find entities using a BSON query.
     * This method is a shortcut for <code>find(query).stream()</code>.
     *
     * @param query a {@link Document} query
     * @return a new {@link ReactivePanacheQuery} instance for the given query
     * @see #find(String, Parameters)
     * @see #find(String, Sort, Map)
     * @see #find(String, Sort, Parameters)
     * @see #list(String, Sort, Parameters)
     * @see #stream(String, Sort, Parameters)
     */
    @GenerateBridge
    public default Multi<Entity> stream(Document query) {
        throw ReactiveMongoOperations.implementationInjectionMissing();
    }

    /**
     * Find entities using a a BSON query and a BSON sort.
     * This method is a shortcut for <code>find(query, sort).stream()</code>.
     *
     * @param query a {@link Document} query
     * @param sort the {@link Document} sort
     * @return a new {@link ReactivePanacheQuery} instance for the given query
     * @see #find(String, Parameters)
     * @see #find(String, Sort, Map)
     * @see #find(String, Sort, Parameters)
     * @see #list(String, Sort, Parameters)
     * @see #stream(String, Sort, Parameters)
     */
    @GenerateBridge
    public default Multi<Entity> stream(Document query, Document sort) {
        throw ReactiveMongoOperations.implementationInjectionMissing();
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
    @GenerateBridge
    public default Multi<Entity> streamAll(Sort sort) {
        throw ReactiveMongoOperations.implementationInjectionMissing();
    }

    /**
     * Find all entities of this type, in the given order.
     * This method is a shortcut for <code>findAll(sort).stream()</code>.
     *
     * @return a {@link Stream} containing all results, without paging
     * @see #streamAll()
     * @see #findAll(Sort)
     * @see #listAll(Sort)
     */
    @GenerateBridge
    public default Multi<Entity> streamAll() {
        throw ReactiveMongoOperations.implementationInjectionMissing();
    }

    /**
     * Counts the number of this type of entity in the database.
     *
     * @return the number of this type of entity in the database.
     * @see #count(String, Object...)
     * @see #count(String, Map)
     * @see #count(String, Parameters)
     */
    @GenerateBridge
    public default Uni<Long> count() {
        throw ReactiveMongoOperations.implementationInjectionMissing();
    }

    /**
     * Counts the number of this type of entity matching the given query, with optional indexed parameters.
     *
     * @param query a {@link io.quarkus.mongodb.panache query string}
     * @param params optional sequence of indexed parameters
     * @return the number of entities counted.
     * @see #count()
     * @see #count(String, Map)
     * @see #count(String, Parameters)
     */
    @GenerateBridge
    public default Uni<Long> count(String query, Object... params) {
        throw ReactiveMongoOperations.implementationInjectionMissing();
    }

    /**
     * Counts the number of this type of entity matching the given query, with named parameters.
     *
     * @param query a {@link io.quarkus.mongodb.panache query string}
     * @param params {@link Map} of named parameters
     * @return the number of entities counted.
     * @see #count()
     * @see #count(String, Object...)
     * @see #count(String, Parameters)
     */
    @GenerateBridge
    public default Uni<Long> count(String query, Map<String, Object> params) {
        throw ReactiveMongoOperations.implementationInjectionMissing();
    }

    /**
     * Counts the number of this type of entity matching the given query, with named parameters.
     *
     * @param query a {@link io.quarkus.mongodb.panache query string}
     * @param params {@link Parameters} of named parameters
     * @return the number of entities counted.
     * @see #count()
     * @see #count(String, Object...)
     * @see #count(String, Map)
     */
    @GenerateBridge
    public default Uni<Long> count(String query, Parameters params) {
        throw ReactiveMongoOperations.implementationInjectionMissing();
    }

    /**
     * Counts the number of this type of entity matching the given query
     *
     * @param query a {@link Document} query
     * @return he number of entities counted.
     * @see #count()
     * @see #count(String, Object...)
     * @see #count(String, Map)
     */
    @GenerateBridge
    public default Uni<Long> count(Document query) {
        throw ReactiveMongoOperations.implementationInjectionMissing();
    }

    /**
     * Delete all entities of this type from the database.
     *
     * @return the number of entities deleted.
     * @see #delete(String, Object...)
     * @see #delete(String, Map)
     * @see #delete(String, Parameters)
     */
    @GenerateBridge
    public default Uni<Long> deleteAll() {
        throw ReactiveMongoOperations.implementationInjectionMissing();
    }

    /**
     * Delete an entity of this type by ID.
     *
     * @param id the ID of the entity to delete.
     * @return false if the entity was not deleted (not found).
     */
    @GenerateBridge
    public default Uni<Boolean> deleteById(Id id) {
        throw MongoOperations.implementationInjectionMissing();
    }

    /**
     * Delete all entities of this type matching the given query, with optional indexed parameters.
     *
     * @param query a {@link io.quarkus.mongodb.panache query string}
     * @param params optional sequence of indexed parameters
     * @return the number of entities deleted.
     * @see #deleteAll()
     * @see #delete(String, Map)
     * @see #delete(String, Parameters)
     */
    @GenerateBridge
    public default Uni<Long> delete(String query, Object... params) {
        throw ReactiveMongoOperations.implementationInjectionMissing();
    }

    /**
     * Delete all entities of this type matching the given query, with named parameters.
     *
     * @param query a {@link io.quarkus.mongodb.panache query string}
     * @param params {@link Map} of named parameters
     * @return the number of entities deleted.
     * @see #deleteAll()
     * @see #delete(String, Object...)
     * @see #delete(String, Parameters)
     */
    @GenerateBridge
    public default Uni<Long> delete(String query, Map<String, Object> params) {
        throw ReactiveMongoOperations.implementationInjectionMissing();
    }

    /**
     * Delete all entities of this type matching the given query, with named parameters.
     *
     * @param query a {@link io.quarkus.mongodb.panache query string}
     * @param params {@link Parameters} of named parameters
     * @return the number of entities deleted.
     * @see #deleteAll()
     * @see #delete(String, Object...)
     * @see #delete(String, Map)
     */
    @GenerateBridge
    public default Uni<Long> delete(String query, Parameters params) {
        throw ReactiveMongoOperations.implementationInjectionMissing();
    }

    /**
     * Delete all entities of this type matching the given query
     *
     * @param query a {@link Document} query
     * @return he number of entities counted.
     * @see #count()
     * @see #count(String, Object...)
     * @see #count(String, Map)
     */
    @GenerateBridge
    public default Uni<Long> delete(Document query) {
        throw ReactiveMongoOperations.implementationInjectionMissing();
    }

    /**
     * Persist all given entities.
     *
     * @param entities the entities to insert
     * @see #persist(Object)
     * @see #persist(Stream)
     * @see #persist(Object,Object...)
     */
    public default Uni<Void> persist(Iterable<Entity> entities) {
        return ReactiveMongoOperations.persist(entities);
    }

    /**
     * Persist all given entities.
     *
     * @param entities the entities to insert
     * @see #persist(Object)
     * @see #persist(Iterable)
     * @see #persist(Object,Object...)
     */
    public default Uni<Void> persist(Stream<Entity> entities) {
        return ReactiveMongoOperations.persist(entities);
    }

    /**
     * Persist all given entities.
     *
     * @param entities the entities to insert
     * @see #persist(Object)
     * @see #persist(Stream)
     * @see #persist(Iterable)
     */
    public default Uni<Void> persist(Entity firstEntity, @SuppressWarnings("unchecked") Entity... entities) {
        return ReactiveMongoOperations.persist(firstEntity, entities);
    }

    /**
     * Update all given entities.
     *
     * @param entities the entities to update
     * @see #update(Object)
     * @see #update(Stream)
     * @see #update(Object,Object...)
     */
    public default Uni<Void> update(Iterable<Entity> entities) {
        return ReactiveMongoOperations.update(entities);
    }

    /**
     * Update all given entities.
     *
     * @param entities the entities to update
     * @see #update(Object)
     * @see #update(Iterable)
     * @see #update(Object,Object...)
     */
    public default Uni<Void> update(Stream<Entity> entities) {
        return ReactiveMongoOperations.update(entities);
    }

    /**
     * Update all given entities.
     *
     * @param entities the entities to update
     * @see #update(Object)
     * @see #update(Stream)
     * @see #update(Iterable)
     */
    public default Uni<Void> update(Entity firstEntity, @SuppressWarnings("unchecked") Entity... entities) {
        return ReactiveMongoOperations.update(firstEntity, entities);
    }

    /**
     * Persist all given entities or update them if they already exist.
     *
     * @param entities the entities to update
     * @see #persistOrUpdate(Object)
     * @see #persistOrUpdate(Stream)
     * @see #persistOrUpdate(Object,Object...)
     */
    public default Uni<Void> persistOrUpdate(Iterable<Entity> entities) {
        return ReactiveMongoOperations.persistOrUpdate(entities);
    }

    /**
     * Persist all given entities or update them if they already exist.
     *
     * @param entities the entities to update
     * @see #persistOrUpdate(Object)
     * @see #persistOrUpdate(Iterable)
     * @see #persistOrUpdate(Object,Object...)
     */
    public default Uni<Void> persistOrUpdate(Stream<Entity> entities) {
        return ReactiveMongoOperations.persistOrUpdate(entities);
    }

    /**
     * Persist all given entities or update them if they already exist.
     *
     * @param entities the entities to update
     * @see #update(Object)
     * @see #update(Stream)
     * @see #update(Iterable)
     */
    public default Uni<Void> persistOrUpdate(Entity firstEntity,
            @SuppressWarnings("unchecked") Entity... entities) {
        return ReactiveMongoOperations.persistOrUpdate(firstEntity, entities);
    }

    /**
     * Allow to access the underlying Mongo Collection
     */
    @GenerateBridge
    public default ReactiveMongoCollection<Entity> mongoCollection() {
        throw ReactiveMongoOperations.implementationInjectionMissing();
    }

    /**
     * Allow to access the underlying Mongo Database.
     */
    @GenerateBridge
    public default ReactiveMongoDatabase mongoDatabase() {
        throw ReactiveMongoOperations.implementationInjectionMissing();
    }
}
