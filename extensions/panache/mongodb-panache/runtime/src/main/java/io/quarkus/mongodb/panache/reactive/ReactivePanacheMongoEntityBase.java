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
 * Represents an entity. If your Mongo entities extend this class they gain auto-generated accessors
 * to all their public fields, as well as a lot of useful
 * methods. Unless you have a custom ID strategy, you should not extend this class directly but extend
 * {@link ReactivePanacheMongoEntity} instead.
 *
 * @see ReactivePanacheMongoEntity
 */
public abstract class ReactivePanacheMongoEntityBase {

    // Operations

    /**
     * Persist this entity in the database.
     * This will set it's ID field if not already set.
     *
     * @see #persist(Iterable)
     * @see #persist(Stream)
     * @see #persist(Object, Object...)
     */
    public Uni<Void> persist() {
        return ReactiveMongoOperations.persist(this);
    }

    /**
     * Update this entity in the database.
     *
     * @see #update(Iterable)
     * @see #update(Stream)
     * @see #update(Object, Object...)
     */
    public Uni<Void> update() {
        return ReactiveMongoOperations.update(this);
    }

    /**
     * Persist this entity in the database or update it if it already exist.
     *
     * @see #persistOrUpdate(Iterable)
     * @see #persistOrUpdate(Stream)
     * @see #persistOrUpdate(Object, Object...)
     */
    public Uni<Void> persistOrUpdate() {
        return ReactiveMongoOperations.persistOrUpdate(this);
    }

    /**
     * Delete this entity from the database, if it is already persisted.
     *
     * @see #delete(String, Object...)
     * @see #delete(String, Map)
     * @see #delete(String, Parameters)
     * @see #deleteAll()
     */
    public Uni<Void> delete() {
        return ReactiveMongoOperations.delete(this);
    }

    // Queries

    /**
     * Find an entity of this type by ID.
     *
     * @param id the ID of the entity to find.
     * @return the entity found, or <code>null</code> if not found.
     */
    @GenerateBridge
    public static <T extends ReactivePanacheMongoEntityBase> Uni<T> findById(Object id) {
        throw ReactiveMongoOperations.implementationInjectionMissing();
    }

    /**
     * Find an entity of this type by ID.
     *
     * @param id the ID of the entity to find.
     * @return if found, an optional containing the entity, else <code>Optional.empty()</code>.
     */
    @GenerateBridge
    public static <T extends ReactivePanacheMongoEntityBase> Uni<Optional<T>> findByIdOptional(Object id) {
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
    public static <T extends ReactivePanacheMongoEntityBase> ReactivePanacheQuery<T> find(String query, Object... params) {
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
    public static <T extends ReactivePanacheMongoEntityBase> ReactivePanacheQuery<T> find(String query, Sort sort,
            Object... params) {
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
    public static <T extends ReactivePanacheMongoEntityBase> ReactivePanacheQuery<T> find(String query,
            Map<String, Object> params) {
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
    public static <T extends ReactivePanacheMongoEntityBase> ReactivePanacheQuery<T> find(String query, Sort sort,
            Map<String, Object> params) {
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
    public static <T extends ReactivePanacheMongoEntityBase> ReactivePanacheQuery<T> find(String query, Parameters params) {
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
    public static <T extends ReactivePanacheMongoEntityBase> ReactivePanacheQuery<T> find(String query, Sort sort,
            Parameters params) {
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
    public static <T extends ReactivePanacheMongoEntityBase> ReactivePanacheQuery<T> find(Document query) {
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
    public static <T extends ReactivePanacheMongoEntityBase> ReactivePanacheQuery<T> find(Document query, Document sort) {
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
    public static <T extends ReactivePanacheMongoEntityBase> ReactivePanacheQuery<T> findAll() {
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
    public static <T extends ReactivePanacheMongoEntityBase> ReactivePanacheQuery<T> findAll(Sort sort) {
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
    public static <T extends ReactivePanacheMongoEntityBase> Uni<List<T>> list(String query, Object... params) {
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
    public static <T extends ReactivePanacheMongoEntityBase> Uni<List<T>> list(String query, Sort sort,
            Object... params) {
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
    public static <T extends ReactivePanacheMongoEntityBase> Uni<List<T>> list(String query,
            Map<String, Object> params) {
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
    public static <T extends ReactivePanacheMongoEntityBase> Uni<List<T>> list(String query, Sort sort,
            Map<String, Object> params) {
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
    public static <T extends ReactivePanacheMongoEntityBase> Uni<List<T>> list(String query, Parameters params) {
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
    public static <T extends ReactivePanacheMongoEntityBase> Uni<List<T>> list(String query, Sort sort,
            Parameters params) {
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
    public static <T extends ReactivePanacheMongoEntityBase> Uni<List<T>> list(Document query) {
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
    public static <T extends ReactivePanacheMongoEntityBase> Uni<List<T>> list(Document query, Document sort) {
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
    public static <T extends ReactivePanacheMongoEntityBase> Uni<List<T>> listAll() {
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
    public static <T extends ReactivePanacheMongoEntityBase> Uni<List<T>> listAll(Sort sort) {
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
    public static <T extends ReactivePanacheMongoEntityBase> Multi<T> stream(String query, Object... params) {
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
    public static <T extends ReactivePanacheMongoEntityBase> Multi<T> stream(String query, Sort sort, Object... params) {
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
    public static <T extends ReactivePanacheMongoEntityBase> Multi<T> stream(String query, Map<String, Object> params) {
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
    public static <T extends ReactivePanacheMongoEntityBase> Multi<T> stream(String query, Sort sort,
            Map<String, Object> params) {
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
    public static <T extends ReactivePanacheMongoEntityBase> Multi<T> stream(String query, Parameters params) {
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
    public static <T extends ReactivePanacheMongoEntityBase> Multi<T> stream(String query, Sort sort, Parameters params) {
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
    public static <T extends ReactivePanacheMongoEntityBase> Multi<T> stream(Document query) {
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
    public static <T extends ReactivePanacheMongoEntityBase> Multi<T> stream(Document query, Document sort) {
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
    public static <T extends ReactivePanacheMongoEntityBase> Multi<T> streamAll() {
        throw ReactiveMongoOperations.implementationInjectionMissing();
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
    @GenerateBridge
    public static <T extends ReactivePanacheMongoEntityBase> Multi<T> streamAll(Sort sort) {
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
    public static Uni<Long> count() {
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
    public static Uni<Long> count(String query, Object... params) {
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
    public static Uni<Long> count(String query, Map<String, Object> params) {
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
    public static Uni<Long> count(String query, Parameters params) {
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
    public static Uni<Long> count(Document query) {
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
    public static Uni<Long> deleteAll() {
        throw ReactiveMongoOperations.implementationInjectionMissing();
    }

    /**
     * Delete an entity of this type by ID.
     *
     * @param id the ID of the entity to delete.
     * @return false if the entity was not deleted (not found).
     */
    @GenerateBridge
    public static Uni<Boolean> deleteById(Object id) {
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
    public static Uni<Long> delete(String query, Object... params) {
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
    public static Uni<Long> delete(String query, Map<String, Object> params) {
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
    public static Uni<Long> delete(String query, Parameters params) {
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
    public static Uni<Long> delete(Document query) {
        throw ReactiveMongoOperations.implementationInjectionMissing();
    }

    /**
     * Insert all given entities.
     *
     * @param entities the entities to insert
     * @see #persist()
     * @see #persist(Stream)
     * @see #persist(Object,Object...)
     */
    public static Uni<Void> persist(Iterable<?> entities) {
        return ReactiveMongoOperations.persist(entities);
    }

    /**
     * Insert all given entities.
     *
     * @param entities the entities to insert
     * @see #persist()
     * @see #persist(Iterable)
     * @see #persist(Object,Object...)
     */
    public static Uni<Void> persist(Stream<?> entities) {
        return ReactiveMongoOperations.persist(entities);
    }

    /**
     * Insert all given entities.
     *
     * @param entities the entities to update
     * @see #persist()
     * @see #persist(Stream)
     * @see #persist(Iterable)
     */
    public static Uni<Void> persist(Object firstEntity, Object... entities) {
        return ReactiveMongoOperations.persist(firstEntity, entities);
    }

    /**
     * Update all given entities.
     *
     * @param entities the entities to update
     * @see #update()
     * @see #update(Stream)
     * @see #update(Object,Object...)
     */
    public static Uni<Void> update(Iterable<?> entities) {
        return ReactiveMongoOperations.update(entities);
    }

    /**
     * Update all given entities.
     *
     * @param entities the entities to insert
     * @see #update()
     * @see #update(Iterable)
     * @see #update(Object,Object...)
     */
    public static Uni<Void> update(Stream<?> entities) {
        return ReactiveMongoOperations.update(entities);
    }

    /**
     * Update all given entities.
     *
     * @param entities the entities to update
     * @see #update()
     * @see #update(Stream)
     * @see #update(Iterable)
     */
    public static Uni<Void> update(Object firstEntity, Object... entities) {
        return ReactiveMongoOperations.update(firstEntity, entities);
    }

    /**
     * Persist all given entities or update them if they already exist.
     *
     * @param entities the entities to update
     * @see #persistOrUpdate()
     * @see #persistOrUpdate(Stream)
     * @see #persistOrUpdate(Object,Object...)
     */
    public static Uni<Void> persistOrUpdate(Iterable<?> entities) {
        return ReactiveMongoOperations.persistOrUpdate(entities);
    }

    /**
     * Persist all given entities.
     *
     * @param entities the entities to insert
     * @see #persistOrUpdate()
     * @see #persistOrUpdate(Iterable)
     * @see #persistOrUpdate(Object,Object...)
     */
    public static Uni<Void> persistOrUpdate(Stream<?> entities) {
        return ReactiveMongoOperations.persistOrUpdate(entities);
    }

    /**
     * Persist all given entities.
     *
     * @param entities the entities to update
     * @see #persistOrUpdate()
     * @see #persistOrUpdate(Stream)
     * @see #persistOrUpdate(Iterable)
     */
    public static Uni<Void> persistOrUpdate(Object firstEntity, Object... entities) {
        return ReactiveMongoOperations.persistOrUpdate(firstEntity, entities);
    }

    /**
     * Allow to access the underlying Mongo Collection.
     */
    @GenerateBridge
    public static <T extends ReactivePanacheMongoEntityBase> ReactiveMongoCollection<T> mongoCollection() {
        throw ReactiveMongoOperations.implementationInjectionMissing();
    }

    /**
     * Allow to access the underlying Mongo Database.
     */
    @GenerateBridge
    public static ReactiveMongoDatabase mongoDatabase() {
        throw ReactiveMongoOperations.implementationInjectionMissing();
    }
}
