package io.quarkus.hibernate.orm.panache;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import javax.json.bind.annotation.JsonbTransient;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.Transient;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.quarkus.hibernate.orm.panache.runtime.JpaOperations;
import io.quarkus.panache.common.Parameters;
import io.quarkus.panache.common.Sort;
import io.quarkus.panache.common.impl.GenerateBridge;

/**
 * <p>
 * Represents an entity. If your Hibernate entities extend this class they gain auto-generated accessors
 * to all their public fields (unless annotated with {@link Transient}), as well as a lot of useful
 * methods. Unless you have a custom ID strategy, you should not extend this class directly but extend
 * {@link PanacheEntity} instead.
 * </p>
 *
 * @author Stéphane Épardaud
 * @see PanacheEntity
 */
public abstract class PanacheEntityBase {

    // Operations

    /**
     * Returns the default {@link EntityManager} for extra operations (eg. CriteriaQueries)
     *
     * @return the default {@link EntityManager}
     */
    @JsonbTransient
    // @JsonIgnore is here to avoid serialization of this property with jackson
    @JsonIgnore
    public EntityManager getEntityManager() {
        return JpaOperations.getEntityManager(this.getClass());
    }

    /**
     * Persist this entity in the database, if not already persisted. This will set your ID field if it is not already set.
     *
     * @see #isPersistent()
     * @see #persist(Iterable)
     * @see #persist(Stream)
     * @see #persist(Object, Object...)
     */
    public void persist() {
        JpaOperations.persist(this);
    }

    /**
     * Persist this entity in the database, if not already persisted. This will set your ID field if it is not already set.
     * Then flushes all pending changes to the database.
     *
     * @see #isPersistent()
     * @see #persist(Iterable)
     * @see #persist(Stream)
     * @see #persist(Object, Object...)
     */
    public void persistAndFlush() {
        JpaOperations.persist(this);
        JpaOperations.flush(this);
    }

    /**
     * Delete this entity from the database, if it is already persisted.
     *
     * @see #isPersistent()
     * @see #delete(String, Object...)
     * @see #delete(String, Map)
     * @see #delete(String, Parameters)
     * @see #deleteAll()
     */
    public void delete() {
        JpaOperations.delete(this);
    }

    /**
     * Returns true if this entity is persistent in the database. If yes, all modifications to
     * its persistent fields will be automatically committed to the database at transaction
     * commit time.
     *
     * @return true if this entity is persistent in the database.
     */
    @JsonbTransient
    // @JsonIgnore is here to avoid serialization of this property with jackson
    @JsonIgnore
    public boolean isPersistent() {
        return JpaOperations.isPersistent(this);
    }

    /**
     * Flushes all pending changes to the database.
     */
    public void flush() {
        JpaOperations.flush(this);
    }

    // Queries

    /**
     * Find an entity of this type by ID.
     *
     * @param id the ID of the entity to find.
     * @return the entity found, or <code>null</code> if not found.
     */
    @GenerateBridge(targetReturnTypeErased = true)
    public static <T extends PanacheEntityBase> T findById(Object id) {
        throw JpaOperations.implementationInjectionMissing();
    }

    /**
     * Find an entity of this type by ID and lock it.
     *
     * @param id the ID of the entity to find.
     * @param lockModeType the locking strategy to be used when retrieving the entity.
     * @return the entity found, or <code>null</code> if not found.
     */
    @GenerateBridge(targetReturnTypeErased = true)
    public static <T extends PanacheEntityBase> T findById(Object id, LockModeType lockModeType) {
        throw JpaOperations.implementationInjectionMissing();
    }

    /**
     * Find an entity of this type by ID.
     *
     * @param id the ID of the entity to find.
     * @return if found, an optional containing the entity, else <code>Optional.empty()</code>.
     */
    @GenerateBridge
    public static <T extends PanacheEntityBase> Optional<T> findByIdOptional(Object id) {
        throw JpaOperations.implementationInjectionMissing();
    }

    /**
     * Find an entity of this type by ID.
     *
     * @param id the ID of the entity to find.
     * @param lockModeType the locking strategy to be used when retrieving the entity.
     * @return if found, an optional containing the entity, else <code>Optional.empty()</code>.
     */
    @GenerateBridge
    public static <T extends PanacheEntityBase> Optional<T> findByIdOptional(Object id, LockModeType lockModeType) {
        throw JpaOperations.implementationInjectionMissing();
    }

    /**
     * Find entities using a query, with optional indexed parameters.
     *
     * @param query a {@link io.quarkus.hibernate.orm.panache query string}
     * @param params optional sequence of indexed parameters
     * @return a new {@link PanacheQuery} instance for the given query
     * @see #find(String, Sort, Object...)
     * @see #find(String, Map)
     * @see #find(String, Parameters)
     * @see #list(String, Object...)
     * @see #stream(String, Object...)
     */
    @GenerateBridge
    public static <T extends PanacheEntityBase> PanacheQuery<T> find(String query, Object... params) {
        throw JpaOperations.implementationInjectionMissing();
    }

    /**
     * Find entities using a query and the given sort options, with optional indexed parameters.
     *
     * @param query a {@link io.quarkus.hibernate.orm.panache query string}
     * @param sort the sort strategy to use
     * @param params optional sequence of indexed parameters
     * @return a new {@link PanacheQuery} instance for the given query
     * @see #find(String, Object...)
     * @see #find(String, Sort, Map)
     * @see #find(String, Sort, Parameters)
     * @see #list(String, Sort, Object...)
     * @see #stream(String, Sort, Object...)
     */
    @GenerateBridge
    public static <T extends PanacheEntityBase> PanacheQuery<T> find(String query, Sort sort, Object... params) {
        throw JpaOperations.implementationInjectionMissing();
    }

    /**
     * Find entities using a query, with named parameters.
     *
     * @param query a {@link io.quarkus.hibernate.orm.panache query string}
     * @param params {@link Map} of named parameters
     * @return a new {@link PanacheQuery} instance for the given query
     * @see #find(String, Sort, Map)
     * @see #find(String, Object...)
     * @see #find(String, Parameters)
     * @see #list(String, Map)
     * @see #stream(String, Map)
     */
    @GenerateBridge
    public static <T extends PanacheEntityBase> PanacheQuery<T> find(String query, Map<String, Object> params) {
        throw JpaOperations.implementationInjectionMissing();
    }

    /**
     * Find entities using a query and the given sort options, with named parameters.
     *
     * @param query a {@link io.quarkus.hibernate.orm.panache query string}
     * @param sort the sort strategy to use
     * @param params {@link Map} of indexed parameters
     * @return a new {@link PanacheQuery} instance for the given query
     * @see #find(String, Map)
     * @see #find(String, Sort, Object...)
     * @see #find(String, Sort, Parameters)
     * @see #list(String, Sort, Map)
     * @see #stream(String, Sort, Map)
     */
    @GenerateBridge
    public static <T extends PanacheEntityBase> PanacheQuery<T> find(String query, Sort sort, Map<String, Object> params) {
        throw JpaOperations.implementationInjectionMissing();
    }

    /**
     * Find entities using a query, with named parameters.
     *
     * @param query a {@link io.quarkus.hibernate.orm.panache query string}
     * @param params {@link Parameters} of named parameters
     * @return a new {@link PanacheQuery} instance for the given query
     * @see #find(String, Sort, Parameters)
     * @see #find(String, Map)
     * @see #find(String, Parameters)
     * @see #list(String, Parameters)
     * @see #stream(String, Parameters)
     */
    @GenerateBridge
    public static <T extends PanacheEntityBase> PanacheQuery<T> find(String query, Parameters params) {
        throw JpaOperations.implementationInjectionMissing();
    }

    /**
     * Find entities using a query and the given sort options, with named parameters.
     *
     * @param query a {@link io.quarkus.hibernate.orm.panache query string}
     * @param sort the sort strategy to use
     * @param params {@link Parameters} of indexed parameters
     * @return a new {@link PanacheQuery} instance for the given query
     * @see #find(String, Parameters)
     * @see #find(String, Sort, Map)
     * @see #find(String, Sort, Parameters)
     * @see #list(String, Sort, Parameters)
     * @see #stream(String, Sort, Parameters)
     */
    @GenerateBridge
    public static <T extends PanacheEntityBase> PanacheQuery<T> find(String query, Sort sort, Parameters params) {
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
    @GenerateBridge
    public static <T extends PanacheEntityBase> PanacheQuery<T> findAll() {
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
    @GenerateBridge
    public static <T extends PanacheEntityBase> PanacheQuery<T> findAll(Sort sort) {
        throw JpaOperations.implementationInjectionMissing();
    }

    /**
     * Find entities matching a query, with optional indexed parameters.
     * This method is a shortcut for <code>find(query, params).list()</code>.
     *
     * @param query a {@link io.quarkus.hibernate.orm.panache query string}
     * @param params optional sequence of indexed parameters
     * @return a {@link List} containing all results, without paging
     * @see #list(String, Sort, Object...)
     * @see #list(String, Map)
     * @see #list(String, Parameters)
     * @see #find(String, Object...)
     * @see #stream(String, Object...)
     */
    @GenerateBridge
    public static <T extends PanacheEntityBase> List<T> list(String query, Object... params) {
        throw JpaOperations.implementationInjectionMissing();
    }

    /**
     * Find entities matching a query and the given sort options, with optional indexed parameters.
     * This method is a shortcut for <code>find(query, sort, params).list()</code>.
     *
     * @param query a {@link io.quarkus.hibernate.orm.panache query string}
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
    public static <T extends PanacheEntityBase> List<T> list(String query, Sort sort, Object... params) {
        throw JpaOperations.implementationInjectionMissing();
    }

    /**
     * Find entities matching a query, with named parameters.
     * This method is a shortcut for <code>find(query, params).list()</code>.
     *
     * @param query a {@link io.quarkus.hibernate.orm.panache query string}
     * @param params {@link Map} of named parameters
     * @return a {@link List} containing all results, without paging
     * @see #list(String, Sort, Map)
     * @see #list(String, Object...)
     * @see #list(String, Parameters)
     * @see #find(String, Map)
     * @see #stream(String, Map)
     */
    @GenerateBridge
    public static <T extends PanacheEntityBase> List<T> list(String query, Map<String, Object> params) {
        throw JpaOperations.implementationInjectionMissing();
    }

    /**
     * Find entities matching a query and the given sort options, with named parameters.
     * This method is a shortcut for <code>find(query, sort, params).list()</code>.
     *
     * @param query a {@link io.quarkus.hibernate.orm.panache query string}
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
    public static <T extends PanacheEntityBase> List<T> list(String query, Sort sort, Map<String, Object> params) {
        throw JpaOperations.implementationInjectionMissing();
    }

    /**
     * Find entities matching a query, with named parameters.
     * This method is a shortcut for <code>find(query, params).list()</code>.
     *
     * @param query a {@link io.quarkus.hibernate.orm.panache query string}
     * @param params {@link Parameters} of named parameters
     * @return a {@link List} containing all results, without paging
     * @see #list(String, Sort, Parameters)
     * @see #list(String, Object...)
     * @see #list(String, Map)
     * @see #find(String, Parameters)
     * @see #stream(String, Parameters)
     */
    @GenerateBridge
    public static <T extends PanacheEntityBase> List<T> list(String query, Parameters params) {
        throw JpaOperations.implementationInjectionMissing();
    }

    /**
     * Find entities matching a query and the given sort options, with named parameters.
     * This method is a shortcut for <code>find(query, sort, params).list()</code>.
     *
     * @param query a {@link io.quarkus.hibernate.orm.panache query string}
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
    public static <T extends PanacheEntityBase> List<T> list(String query, Sort sort, Parameters params) {
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
    @GenerateBridge
    public static <T extends PanacheEntityBase> List<T> listAll() {
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
    @GenerateBridge
    public static <T extends PanacheEntityBase> List<T> listAll(Sort sort) {
        throw JpaOperations.implementationInjectionMissing();
    }

    /**
     * Find entities matching a query, with optional indexed parameters.
     * This method is a shortcut for <code>find(query, params).stream()</code>.
     * It requires a transaction to work.
     * Without a transaction, the underlying cursor can be closed before the end of the stream.
     *
     * @param query a {@link io.quarkus.hibernate.orm.panache query string}
     * @param params optional sequence of indexed parameters
     * @return a {@link Stream} containing all results, without paging
     * @see #stream(String, Sort, Object...)
     * @see #stream(String, Map)
     * @see #stream(String, Parameters)
     * @see #find(String, Object...)
     * @see #list(String, Object...)
     */
    @GenerateBridge
    public static <T extends PanacheEntityBase> Stream<T> stream(String query, Object... params) {
        throw JpaOperations.implementationInjectionMissing();
    }

    /**
     * Find entities matching a query and the given sort options, with optional indexed parameters.
     * This method is a shortcut for <code>find(query, sort, params).stream()</code>.
     * It requires a transaction to work.
     * Without a transaction, the underlying cursor can be closed before the end of the stream.
     *
     * @param query a {@link io.quarkus.hibernate.orm.panache query string}
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
    public static <T extends PanacheEntityBase> Stream<T> stream(String query, Sort sort, Object... params) {
        throw JpaOperations.implementationInjectionMissing();
    }

    /**
     * Find entities matching a query, with named parameters.
     * This method is a shortcut for <code>find(query, params).stream()</code>.
     * It requires a transaction to work.
     * Without a transaction, the underlying cursor can be closed before the end of the stream.
     *
     * @param query a {@link io.quarkus.hibernate.orm.panache query string}
     * @param params {@link Map} of named parameters
     * @return a {@link Stream} containing all results, without paging
     * @see #stream(String, Sort, Map)
     * @see #stream(String, Object...)
     * @see #stream(String, Parameters)
     * @see #find(String, Map)
     * @see #list(String, Map)
     */
    @GenerateBridge
    public static <T extends PanacheEntityBase> Stream<T> stream(String query, Map<String, Object> params) {
        throw JpaOperations.implementationInjectionMissing();
    }

    /**
     * Find entities matching a query and the given sort options, with named parameters.
     * This method is a shortcut for <code>find(query, sort, params).stream()</code>.
     * It requires a transaction to work.
     * Without a transaction, the underlying cursor can be closed before the end of the stream.
     *
     * @param query a {@link io.quarkus.hibernate.orm.panache query string}
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
    public static <T extends PanacheEntityBase> Stream<T> stream(String query, Sort sort, Map<String, Object> params) {
        throw JpaOperations.implementationInjectionMissing();
    }

    /**
     * Find entities matching a query, with named parameters.
     * This method is a shortcut for <code>find(query, params).stream()</code>.
     * It requires a transaction to work.
     * Without a transaction, the underlying cursor can be closed before the end of the stream.
     *
     * @param query a {@link io.quarkus.hibernate.orm.panache query string}
     * @param params {@link Parameters} of named parameters
     * @return a {@link Stream} containing all results, without paging
     * @see #stream(String, Sort, Parameters)
     * @see #stream(String, Object...)
     * @see #stream(String, Map)
     * @see #find(String, Parameters)
     * @see #list(String, Parameters)
     */
    @GenerateBridge
    public static <T extends PanacheEntityBase> Stream<T> stream(String query, Parameters params) {
        throw JpaOperations.implementationInjectionMissing();
    }

    /**
     * Find entities matching a query and the given sort options, with named parameters.
     * This method is a shortcut for <code>find(query, sort, params).stream()</code>.
     * It requires a transaction to work.
     * Without a transaction, the underlying cursor can be closed before the end of the stream.
     *
     * @param query a {@link io.quarkus.hibernate.orm.panache query string}
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
    public static <T extends PanacheEntityBase> Stream<T> stream(String query, Sort sort, Parameters params) {
        throw JpaOperations.implementationInjectionMissing();
    }

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
    @GenerateBridge
    public static <T extends PanacheEntityBase> Stream<T> streamAll() {
        throw JpaOperations.implementationInjectionMissing();
    }

    /**
     * Find all entities of this type, in the given order.
     * This method is a shortcut for <code>findAll(sort).stream()</code>.
     * It requires a transaction to work.
     * Without a transaction, the underlying cursor can be closed before the end of the stream.
     *
     * @param sort the sort order to use
     * @return a {@link Stream} containing all results, without paging
     * @see #streamAll()
     * @see #findAll(Sort)
     * @see #listAll(Sort)
     */
    @GenerateBridge
    public static <T extends PanacheEntityBase> Stream<T> streamAll(Sort sort) {
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
    @GenerateBridge
    public static long count() {
        throw JpaOperations.implementationInjectionMissing();
    }

    /**
     * Counts the number of this type of entity matching the given query, with optional indexed parameters.
     *
     * @param query a {@link io.quarkus.hibernate.orm.panache query string}
     * @param params optional sequence of indexed parameters
     * @return the number of entities counted.
     * @see #count()
     * @see #count(String, Map)
     * @see #count(String, Parameters)
     */
    @GenerateBridge
    public static long count(String query, Object... params) {
        throw JpaOperations.implementationInjectionMissing();
    }

    /**
     * Counts the number of this type of entity matching the given query, with named parameters.
     *
     * @param query a {@link io.quarkus.hibernate.orm.panache query string}
     * @param params {@link Map} of named parameters
     * @return the number of entities counted.
     * @see #count()
     * @see #count(String, Object...)
     * @see #count(String, Parameters)
     */
    @GenerateBridge
    public static long count(String query, Map<String, Object> params) {
        throw JpaOperations.implementationInjectionMissing();
    }

    /**
     * Counts the number of this type of entity matching the given query, with named parameters.
     *
     * @param query a {@link io.quarkus.hibernate.orm.panache query string}
     * @param params {@link Parameters} of named parameters
     * @return the number of entities counted.
     * @see #count()
     * @see #count(String, Object...)
     * @see #count(String, Map)
     */
    @GenerateBridge
    public static long count(String query, Parameters params) {
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
    @GenerateBridge
    public static long deleteAll() {
        throw JpaOperations.implementationInjectionMissing();
    }

    /**
     * Delete an entity of this type by ID.
     *
     * @param id the ID of the entity to delete.
     * @return false if the entity was not deleted (not found).
     */
    @GenerateBridge
    public static boolean deleteById(Object id) {
        throw JpaOperations.implementationInjectionMissing();
    }

    /**
     * Delete all entities of this type matching the given query, with optional indexed parameters.
     *
     * @param query a {@link io.quarkus.hibernate.orm.panache query string}
     * @param params optional sequence of indexed parameters
     * @return the number of entities deleted.
     * @see #deleteAll()
     * @see #delete(String, Map)
     * @see #delete(String, Parameters)
     */
    @GenerateBridge
    public static long delete(String query, Object... params) {
        throw JpaOperations.implementationInjectionMissing();
    }

    /**
     * Delete all entities of this type matching the given query, with named parameters.
     *
     * @param query a {@link io.quarkus.hibernate.orm.panache query string}
     * @param params {@link Map} of named parameters
     * @return the number of entities deleted.
     * @see #deleteAll()
     * @see #delete(String, Object...)
     * @see #delete(String, Parameters)
     */
    @GenerateBridge
    public static long delete(String query, Map<String, Object> params) {
        throw JpaOperations.implementationInjectionMissing();
    }

    /**
     * Delete all entities of this type matching the given query, with named parameters.
     *
     * @param query a {@link io.quarkus.hibernate.orm.panache query string}
     * @param params {@link Parameters} of named parameters
     * @return the number of entities deleted.
     * @see #deleteAll()
     * @see #delete(String, Object...)
     * @see #delete(String, Map)
     */
    @GenerateBridge
    public static long delete(String query, Parameters params) {
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
    public static void persist(Iterable<?> entities) {
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
    public static void persist(Stream<?> entities) {
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
    public static void persist(Object firstEntity, Object... entities) {
        JpaOperations.persist(firstEntity, entities);
    }

    /**
     * Update all entities of this type matching the given query, with optional indexed parameters.
     *
     * @param query a {@link io.quarkus.hibernate.orm.panache query string}
     * @param params optional sequence of indexed parameters
     * @return the number of entities updated.
     * @see #update(String, Map)
     * @see #update(String, Parameters)
     */
    @GenerateBridge
    public static int update(String query, Object... params) {
        throw JpaOperations.implementationInjectionMissing();
    }

    /**
     * Update all entities of this type matching the given query, with named parameters.
     *
     * @param query a {@link io.quarkus.hibernate.orm.panache query string}
     * @param params {@link Map} of named parameters
     * @return the number of entities updated.
     * @see #update(String, Object...)
     * @see #update(String, Parameters)
     * 
     */
    @GenerateBridge
    public static int update(String query, Map<String, Object> params) {
        throw JpaOperations.implementationInjectionMissing();
    }

    /**
     * Update all entities of this type matching the given query, with named parameters.
     *
     * @param query a {@link io.quarkus.hibernate.orm.panache query string}
     * @param params {@link Parameters} of named parameters
     * @return the number of entities updated.
     * @see #update(String, Object...)
     * @see #update(String, Map)
     */
    @GenerateBridge
    public static int update(String query, Parameters params) {
        throw JpaOperations.implementationInjectionMissing();
    }
}
