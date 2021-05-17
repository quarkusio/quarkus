package io.quarkus.mongodb.panache.reactive;

import static io.quarkus.mongodb.panache.reactive.runtime.JavaReactiveMongoOperations.INSTANCE;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.bson.Document;

import io.quarkus.mongodb.panache.PanacheUpdate;
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
@SuppressWarnings("unchecked")
public interface ReactivePanacheMongoRepositoryBase<Entity, Id> {
    /**
     * Persist the given entity in the database.
     * This will set it's ID field if not already set.
     * 
     * @param entity the entity to insert.
     * @see #persist(Iterable)
     * @see #persist(Stream)
     * @see #persist(Object, Object...)
     */
    default Uni<Entity> persist(Entity entity) {
        return INSTANCE.persist(entity).map(v -> entity);
    }

    /**
     * Update the given entity in the database.
     *
     * @param entity the entity to update.
     * @see #update(Iterable)
     * @see #update(Stream)
     * @see #update(Object, Object...)
     */
    default Uni<Entity> update(Entity entity) {
        return INSTANCE.update(entity).map(v -> entity);
    }

    /**
     * Persist the given entity in the database or update it if it already exist.
     *
     * @param entity the entity to update.
     * @see #persistOrUpdate(Iterable)
     * @see #persistOrUpdate(Stream)
     * @see #persistOrUpdate(Object, Object...)
     */
    default Uni<Entity> persistOrUpdate(Entity entity) {
        return INSTANCE.persistOrUpdate(entity).map(v -> entity);
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
    default Uni<Void> delete(Entity entity) {
        return INSTANCE.delete(entity);
    }

    // Queries

    /**
     * Find an entity of this type by ID.
     * 
     * @param id the ID of the entity to find.
     * @return the entity found, or <code>null</code> if not found.
     */
    @GenerateBridge
    default Uni<Entity> findById(Id id) {
        throw INSTANCE.implementationInjectionMissing();
    }

    /**
     * Find an entity of this type by ID.
     *
     * @param id the ID of the entity to find.
     * @return if found, an optional containing the entity, else <code>Optional.empty()</code>.
     */
    @GenerateBridge
    default Uni<Optional<Entity>> findByIdOptional(Object id) {
        throw INSTANCE.implementationInjectionMissing();
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
    default ReactivePanacheQuery<Entity> find(String query, Object... params) {
        throw INSTANCE.implementationInjectionMissing();
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
    default ReactivePanacheQuery<Entity> find(String query, Sort sort, Object... params) {
        throw INSTANCE.implementationInjectionMissing();
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
    default ReactivePanacheQuery<Entity> find(String query, Map<String, Object> params) {
        throw INSTANCE.implementationInjectionMissing();
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
    default ReactivePanacheQuery<Entity> find(String query, Sort sort, Map<String, Object> params) {
        throw INSTANCE.implementationInjectionMissing();
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
    default ReactivePanacheQuery<Entity> find(String query, Parameters params) {
        throw INSTANCE.implementationInjectionMissing();
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
    default ReactivePanacheQuery<Entity> find(String query, Sort sort, Parameters params) {
        throw INSTANCE.implementationInjectionMissing();
    }

    /**
     * Find entities using a BSON query.
     *
     * @param query a {@link Document} query
     * @return a new {@link ReactivePanacheQuery} instance for the given query
     * @see #find(Document, Document)
     * @see #list(Document)
     * @see #list(Document, Document)
     * @see #stream(Document)
     * @see #stream(Document, Document)
     */
    @GenerateBridge
    default ReactivePanacheQuery<Entity> find(Document query) {
        throw INSTANCE.implementationInjectionMissing();
    }

    /**
     * Find entities using a a BSON query and a BSON sort.
     *
     * @param query a {@link Document} query
     * @param sort the {@link Document} sort
     * @return a new {@link ReactivePanacheQuery} instance for the given query
     * @see #find(Document)
     * @see #list(Document)
     * @see #list(Document, Document)
     * @see #stream(Document)
     * @see #stream(Document, Document)
     */
    @GenerateBridge
    default ReactivePanacheQuery<Entity> find(Document query, Document sort) {
        throw INSTANCE.implementationInjectionMissing();
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
    default ReactivePanacheQuery<Entity> findAll() {
        throw INSTANCE.implementationInjectionMissing();
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
    default ReactivePanacheQuery<Entity> findAll(Sort sort) {
        throw INSTANCE.implementationInjectionMissing();
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
    default Uni<List<Entity>> list(String query, Object... params) {
        throw INSTANCE.implementationInjectionMissing();
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
    default Uni<List<Entity>> list(String query, Sort sort, Object... params) {
        throw INSTANCE.implementationInjectionMissing();
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
    default Uni<List<Entity>> list(String query, Map<String, Object> params) {
        throw INSTANCE.implementationInjectionMissing();
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
    default Uni<List<Entity>> list(String query, Sort sort, Map<String, Object> params) {
        throw INSTANCE.implementationInjectionMissing();
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
    default Uni<List<Entity>> list(String query, Parameters params) {
        throw INSTANCE.implementationInjectionMissing();
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
    default Uni<List<Entity>> list(String query, Sort sort, Parameters params) {
        throw INSTANCE.implementationInjectionMissing();
    }

    /**
     * Find entities using a BSON query.
     * This method is a shortcut for <code>find(query).list()</code>.
     *
     * @param query a {@link Document} query
     * @return a {@link List} containing all results, without paging
     * @see #find(Document)
     * @see #find(Document, Document)
     * @see #list(Document, Document)
     * @see #stream(Document)
     * @see #stream(Document, Document)
     */
    @GenerateBridge
    default Uni<List<Entity>> list(Document query) {
        throw INSTANCE.implementationInjectionMissing();
    }

    /**
     * Find entities using a a BSON query and a BSON sort.
     * This method is a shortcut for <code>find(query, sort).list()</code>.
     *
     * @param query a {@link Document} query
     * @param sort the {@link Document} sort
     * @return a {@link List} containing all results, without paging
     * @see #find(Document)
     * @see #find(Document, Document)
     * @see #list(Document)
     * @see #stream(Document)
     * @see #stream(Document, Document)
     */
    @GenerateBridge
    default Uni<List<Entity>> list(Document query, Document sort) {
        throw INSTANCE.implementationInjectionMissing();
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
    default Uni<List<Entity>> listAll() {
        throw INSTANCE.implementationInjectionMissing();
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
    default Uni<List<Entity>> listAll(Sort sort) {
        throw INSTANCE.implementationInjectionMissing();
    }

    /**
     * Find entities matching a query, with optional indexed parameters.
     * This method is a shortcut for <code>find(query, params).stream()</code>.
     *
     * @param query a {@link io.quarkus.mongodb.panache query string}
     * @param params optional sequence of indexed parameters
     * @return a {@link Multi} containing all results, without paging
     * @see #stream(String, Sort, Object...)
     * @see #stream(String, Map)
     * @see #stream(String, Parameters)
     * @see #find(String, Object...)
     * @see #list(String, Object...)
     */
    @GenerateBridge
    default Multi<Entity> stream(String query, Object... params) {
        throw INSTANCE.implementationInjectionMissing();
    }

    /**
     * Find entities matching a query and the given sort options, with optional indexed parameters.
     * This method is a shortcut for <code>find(query, sort, params).stream()</code>.
     *
     * @param query a {@link io.quarkus.mongodb.panache query string}
     * @param sort the sort strategy to use
     * @param params optional sequence of indexed parameters
     * @return a {@link Multi} containing all results, without paging
     * @see #stream(String, Object...)
     * @see #stream(String, Sort, Map)
     * @see #stream(String, Sort, Parameters)
     * @see #find(String, Sort, Object...)
     * @see #list(String, Sort, Object...)
     */
    @GenerateBridge
    default Multi<Entity> stream(String query, Sort sort, Object... params) {
        throw INSTANCE.implementationInjectionMissing();
    }

    /**
     * Find entities matching a query, with named parameters.
     * This method is a shortcut for <code>find(query, params).stream()</code>.
     *
     * @param query a {@link io.quarkus.mongodb.panache query string}
     * @param params {@link Map} of named parameters
     * @return a {@link Multi} containing all results, without paging
     * @see #stream(String, Sort, Map)
     * @see #stream(String, Object...)
     * @see #stream(String, Parameters)
     * @see #find(String, Map)
     * @see #list(String, Map)
     */
    @GenerateBridge
    default Multi<Entity> stream(String query, Map<String, Object> params) {
        throw INSTANCE.implementationInjectionMissing();
    }

    /**
     * Find entities matching a query and the given sort options, with named parameters.
     * This method is a shortcut for <code>find(query, sort, params).stream()</code>.
     *
     * @param query a {@link io.quarkus.mongodb.panache query string}
     * @param sort the sort strategy to use
     * @param params {@link Map} of indexed parameters
     * @return a {@link Multi} containing all results, without paging
     * @see #stream(String, Map)
     * @see #stream(String, Sort, Object...)
     * @see #stream(String, Sort, Parameters)
     * @see #find(String, Sort, Map)
     * @see #list(String, Sort, Map)
     */
    @GenerateBridge
    default Multi<Entity> stream(String query, Sort sort, Map<String, Object> params) {
        throw INSTANCE.implementationInjectionMissing();
    }

    /**
     * Find entities matching a query, with named parameters.
     * This method is a shortcut for <code>find(query, params).stream()</code>.
     *
     * @param query a {@link io.quarkus.mongodb.panache query string}
     * @param params {@link Parameters} of named parameters
     * @return a {@link Multi} containing all results, without paging
     * @see #stream(String, Sort, Parameters)
     * @see #stream(String, Object...)
     * @see #stream(String, Map)
     * @see #find(String, Parameters)
     * @see #list(String, Parameters)
     */
    @GenerateBridge
    default Multi<Entity> stream(String query, Parameters params) {
        throw INSTANCE.implementationInjectionMissing();
    }

    /**
     * Find entities matching a query and the given sort options, with named parameters.
     * This method is a shortcut for <code>find(query, sort, params).stream()</code>.
     *
     * @param query a {@link io.quarkus.mongodb.panache query string}
     * @param sort the sort strategy to use
     * @param params {@link Parameters} of indexed parameters
     * @return a {@link Multi} containing all results, without paging
     * @see #stream(String, Parameters)
     * @see #stream(String, Sort, Object...)
     * @see #stream(String, Sort, Map)
     * @see #find(String, Sort, Parameters)
     * @see #list(String, Sort, Parameters)
     */
    @GenerateBridge
    default Multi<Entity> stream(String query, Sort sort, Parameters params) {
        throw INSTANCE.implementationInjectionMissing();
    }

    /**
     * Find entities using a BSON query.
     * This method is a shortcut for <code>find(query).stream()</code>.
     *
     * @param query a {@link Document} query
     * @return a {@link Multi} containing all results, without paging
     * @see #find(Document)
     * @see #find(Document, Document)
     * @see #list(Document)
     * @see #list(Document, Document)
     * @see #stream(Document)
     * @see #stream(Document, Document)
     */
    @GenerateBridge
    default Multi<Entity> stream(Document query) {
        throw INSTANCE.implementationInjectionMissing();
    }

    /**
     * Find entities using a a BSON query and a BSON sort.
     * This method is a shortcut for <code>find(query, sort).stream()</code>.
     *
     * @param query a {@link Document} query
     * @param sort the {@link Document} sort
     * @return a {@link Multi} containing all results, without paging
     * @see #find(Document)
     * @see #find(Document, Document)
     * @see #list(Document)
     * @see #list(Document, Document)
     * @see #stream(Document)
     * @see #stream(Document, Document)
     */
    @GenerateBridge
    default Multi<Entity> stream(Document query, Document sort) {
        throw INSTANCE.implementationInjectionMissing();
    }

    /**
     * Find all entities of this type.
     * This method is a shortcut for <code>findAll().stream()</code>.
     *
     * @return a {@link Multi} containing all results, without paging
     * @see #streamAll(Sort)
     * @see #findAll()
     * @see #listAll()
     */
    @GenerateBridge
    default Multi<Entity> streamAll(Sort sort) {
        throw INSTANCE.implementationInjectionMissing();
    }

    /**
     * Find all entities of this type, in the given order.
     * This method is a shortcut for <code>findAll(sort).stream()</code>.
     *
     * @return a {@link Multi} containing all results, without paging
     * @see #streamAll()
     * @see #findAll(Sort)
     * @see #listAll(Sort)
     */
    @GenerateBridge
    default Multi<Entity> streamAll() {
        throw INSTANCE.implementationInjectionMissing();
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
    default Uni<Long> count() {
        throw INSTANCE.implementationInjectionMissing();
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
    default Uni<Long> count(String query, Object... params) {
        throw INSTANCE.implementationInjectionMissing();
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
    default Uni<Long> count(String query, Map<String, Object> params) {
        throw INSTANCE.implementationInjectionMissing();
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
    default Uni<Long> count(String query, Parameters params) {
        throw INSTANCE.implementationInjectionMissing();
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
    default Uni<Long> count(Document query) {
        throw INSTANCE.implementationInjectionMissing();
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
    default Uni<Long> deleteAll() {
        throw INSTANCE.implementationInjectionMissing();
    }

    /**
     * Delete an entity of this type by ID.
     *
     * @param id the ID of the entity to delete.
     * @return false if the entity was not deleted (not found).
     */
    @GenerateBridge
    default Uni<Boolean> deleteById(Id id) {
        throw INSTANCE.implementationInjectionMissing();
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
    default Uni<Long> delete(String query, Object... params) {
        throw INSTANCE.implementationInjectionMissing();
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
    default Uni<Long> delete(String query, Map<String, Object> params) {
        throw INSTANCE.implementationInjectionMissing();
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
    default Uni<Long> delete(String query, Parameters params) {
        throw INSTANCE.implementationInjectionMissing();
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
    default Uni<Long> delete(Document query) {
        throw INSTANCE.implementationInjectionMissing();
    }

    /**
     * Persist all given entities.
     *
     * @param entities the entities to insert
     * @see #persist(Object)
     * @see #persist(Stream)
     * @see #persist(Object,Object...)
     */
    default Uni<Void> persist(Iterable<Entity> entities) {
        return INSTANCE.persist(entities);
    }

    /**
     * Persist all given entities.
     *
     * @param entities the entities to insert
     * @see #persist(Object)
     * @see #persist(Iterable)
     * @see #persist(Object,Object...)
     */
    default Uni<Void> persist(Stream<Entity> entities) {
        return INSTANCE.persist(entities);
    }

    /**
     * Persist all given entities.
     *
     * @param entities the entities to insert
     * @see #persist(Object)
     * @see #persist(Stream)
     * @see #persist(Iterable)
     */
    default Uni<Void> persist(Entity firstEntity, Entity... entities) {
        return INSTANCE.persist(firstEntity, entities);
    }

    /**
     * Update all given entities.
     *
     * @param entities the entities to update
     * @see #update(Object)
     * @see #update(Stream)
     * @see #update(Object,Object...)
     */
    default Uni<Void> update(Iterable<Entity> entities) {
        return INSTANCE.update(entities);
    }

    /**
     * Update all given entities.
     *
     * @param entities the entities to update
     * @see #update(Object)
     * @see #update(Iterable)
     * @see #update(Object,Object...)
     */
    default Uni<Void> update(Stream<Entity> entities) {
        return INSTANCE.update(entities);
    }

    /**
     * Update all given entities.
     *
     * @param entities the entities to update
     * @see #update(Object)
     * @see #update(Stream)
     * @see #update(Iterable)
     */
    default Uni<Void> update(Entity firstEntity, Entity... entities) {
        return INSTANCE.update(firstEntity, entities);
    }

    /**
     * Persist all given entities or update them if they already exist.
     *
     * @param entities the entities to update
     * @see #persistOrUpdate(Object)
     * @see #persistOrUpdate(Stream)
     * @see #persistOrUpdate(Object,Object...)
     */
    default Uni<Void> persistOrUpdate(Iterable<Entity> entities) {
        return INSTANCE.persistOrUpdate(entities);
    }

    /**
     * Persist all given entities or update them if they already exist.
     *
     * @param entities the entities to update
     * @see #persistOrUpdate(Object)
     * @see #persistOrUpdate(Iterable)
     * @see #persistOrUpdate(Object,Object...)
     */
    default Uni<Void> persistOrUpdate(Stream<Entity> entities) {
        return INSTANCE.persistOrUpdate(entities);
    }

    /**
     * Persist all given entities or update them if they already exist.
     *
     * @param entities the entities to update
     * @see #update(Object)
     * @see #update(Stream)
     * @see #update(Iterable)
     */
    default Uni<Void> persistOrUpdate(Entity firstEntity, Entity... entities) {
        return INSTANCE.persistOrUpdate(firstEntity, entities);
    }

    /**
     * Update all entities of this type by the given update document, with optional indexed parameters.
     * The returned {@link PanacheUpdate} object will allow to restrict on which document the update should be applied.
     *
     * @param update the update document, if it didn't contain any update operator, we add <code>$set</code>.
     *        It can also be expressed as a {@link io.quarkus.mongodb.panache query string}.
     * @param params optional sequence of indexed parameters
     * @return a new {@link ReactivePanacheUpdate} instance for the given update document
     * @see #update(String, Map)
     * @see #update(String, Parameters)
     */
    @GenerateBridge
    default ReactivePanacheUpdate update(String update, Object... params) {
        throw INSTANCE.implementationInjectionMissing();
    }

    /**
     * Update all entities of this type by the given update document, with named parameters.
     * The returned {@link PanacheUpdate} object will allow to restrict on which document the update should be applied.
     *
     * @param update the update document, if it didn't contain any update operator, we add <code>$set</code>.
     *        It can also be expressed as a {@link io.quarkus.mongodb.panache query string}.
     * @param params {@link Map} of named parameters
     * @return a new {@link ReactivePanacheUpdate} instance for the given update document
     * @see #update(String, Object...)
     * @see #update(String, Parameters)
     *
     */
    @GenerateBridge
    default ReactivePanacheUpdate update(String update, Map<String, Object> params) {
        throw INSTANCE.implementationInjectionMissing();
    }

    /**
     * Update all entities of this type by the given update document, with named parameters.
     * The returned {@link PanacheUpdate} object will allow to restrict on which document the update should be applied.
     *
     * @param update the update document, if it didn't contain any update operator, we add <code>$set</code>.
     *        It can also be expressed as a {@link io.quarkus.mongodb.panache query string}.
     * @param params {@link Parameters} of named parameters
     * @return a new {@link ReactivePanacheUpdate} instance for the given update document
     * @see #update(String, Object...)
     * @see #update(String, Map)
     */
    @GenerateBridge
    default ReactivePanacheUpdate update(String update, Parameters params) {
        throw INSTANCE.implementationInjectionMissing();
    }

    /**
     * Allow to access the underlying Mongo Collection
     */
    @GenerateBridge
    default ReactiveMongoCollection<Entity> mongoCollection() {
        throw INSTANCE.implementationInjectionMissing();
    }

    /**
     * Allow to access the underlying Mongo Database.
     */
    @GenerateBridge
    default ReactiveMongoDatabase mongoDatabase() {
        throw INSTANCE.implementationInjectionMissing();
    }
}
