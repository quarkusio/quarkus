package io.quarkus.hibernate.reactive.panache.kotlin;

import io.quarkus.hibernate.reactive.panache.common.runtime.AbstractJpaOperations;
import io.quarkus.hibernate.reactive.panache.kotlin.runtime.KotlinJpaOperations
import io.quarkus.panache.common.Parameters
import io.quarkus.panache.common.Sort
import io.quarkus.panache.common.impl.GenerateBridge
import io.smallrye.mutiny.Multi
import io.smallrye.mutiny.Uni
import org.hibernate.reactive.mutiny.Mutiny
import java.util.function.Supplier
import java.util.stream.Stream
import javax.persistence.LockModeType

/**
 * Defines methods to be used via the companion objects of entities.
 *
 * @param Entity the entity type
 */
interface PanacheCompanion<Entity : PanacheEntityBase> : PanacheCompanionBase<Entity, Long>

/**
 * Defines methods to be used via the companion objects of entities.
 *
 * @param Entity the entity type
 */
interface PanacheCompanionBase<Entity : PanacheEntityBase, Id : Any> {

    /**
     * Returns the current {@link Mutiny.Session}
     *
     * @return the current {@link Mutiny.Session}
     */
    fun getSession(): Uni<Mutiny.Session> {
        return AbstractJpaOperations.getSession();
    }

// Queries
    /**
     * Find an entity of this type by ID.
     *
     * @param id the ID of the entity to find.
     * @return the entity found, or `null` if not found.
     */
//    @GenerateBridge
    fun findById(id: Any?): Uni<Entity> {
        throw KotlinJpaOperations.INSTANCE.implementationInjectionMissing()
    }

    /**
     * Find an entity of this type by ID and lock it.
     *
     * @param id the ID of the entity to find.
     * @param lockModeType the locking strategy to be used when retrieving the entity.
     * @return the entity found, or `null` if not found.
     */
    @GenerateBridge
    fun findById(
        id: Any?,
        lockModeType: LockModeType?
    ): Uni<Entity> {
        throw KotlinJpaOperations.INSTANCE.implementationInjectionMissing()
    }

    /**
     * Find entities using a query, with optional indexed parameters.
     *
     * @param query a [query string][io.quarkus.hibernate.reactive.panache]
     * @param params optional sequence of indexed parameters
     * @return a new [PanacheQuery] instance for the given query
     * @see .find
     * @see .find
     * @see .find
     * @see .list
     * @see .stream
     */
    @GenerateBridge
    fun find(
        query: String?,
        vararg params: Any?
    ): PanacheQuery<Entity> {
        throw KotlinJpaOperations.INSTANCE.implementationInjectionMissing()
    }

    /**
     * Find entities using a query and the given sort options, with optional indexed parameters.
     *
     * @param query a [query string][io.quarkus.hibernate.reactive.panache]
     * @param sort the sort strategy to use
     * @param params optional sequence of indexed parameters
     * @return a new [PanacheQuery] instance for the given query
     * @see .find
     * @see .find
     * @see .find
     * @see .list
     * @see .stream
     */
    @GenerateBridge
    fun find(
        query: String?,
        sort: Sort?,
        vararg params: Any?
    ): PanacheQuery<Entity> {
        throw KotlinJpaOperations.INSTANCE.implementationInjectionMissing()
    }

    /**
     * Find entities using a query, with named parameters.
     *
     * @param query a [query string][io.quarkus.hibernate.reactive.panache]
     * @param params [Map] of named parameters
     * @return a new [PanacheQuery] instance for the given query
     * @see .find
     * @see .find
     * @see .find
     * @see .list
     * @see .stream
     */
    @GenerateBridge
    fun find(
        query: String?,
        params: Map<String?, Any?>?
    ): PanacheQuery<Entity> {
        throw KotlinJpaOperations.INSTANCE.implementationInjectionMissing()
    }

    /**
     * Find entities using a query and the given sort options, with named parameters.
     *
     * @param query a [query string][io.quarkus.hibernate.reactive.panache]
     * @param sort the sort strategy to use
     * @param params [Map] of indexed parameters
     * @return a new [PanacheQuery] instance for the given query
     * @see .find
     * @see .find
     * @see .find
     * @see .list
     * @see .stream
     */
    @GenerateBridge
    fun find(
        query: String?,
        sort: Sort?,
        params: Map<String?, Any?>?
    ): PanacheQuery<Entity> {
        throw KotlinJpaOperations.INSTANCE.implementationInjectionMissing()
    }

    /**
     * Find entities using a query, with named parameters.
     *
     * @param query a [query string][io.quarkus.hibernate.reactive.panache]
     * @param params [Parameters] of named parameters
     * @return a new [PanacheQuery] instance for the given query
     * @see .find
     * @see .find
     * @see .find
     * @see .list
     * @see .stream
     */
    @GenerateBridge
    fun find(
        query: String?,
        params: Parameters?
    ): PanacheQuery<Entity> {
        throw KotlinJpaOperations.INSTANCE.implementationInjectionMissing()
    }

    /**
     * Find entities using a query and the given sort options, with named parameters.
     *
     * @param query a [query string][io.quarkus.hibernate.reactive.panache]
     * @param sort the sort strategy to use
     * @param params [Parameters] of indexed parameters
     * @return a new [PanacheQuery] instance for the given query
     * @see .find
     * @see .find
     * @see .find
     * @see .list
     * @see .stream
     */
    @GenerateBridge
    fun find(
        query: String?,
        sort: Sort?,
        params: Parameters?
    ): PanacheQuery<Entity> {
        throw KotlinJpaOperations.INSTANCE.implementationInjectionMissing()
    }

    /**
     * Find all entities of this type.
     *
     * @return a new [PanacheQuery] instance to find all entities of this type.
     * @see .findAll
     * @see .listAll
     * @see .streamAll
     */
    @GenerateBridge
    fun findAll(): PanacheQuery<Entity> {
        throw KotlinJpaOperations.INSTANCE.implementationInjectionMissing()
    }

    /**
     * Find all entities of this type, in the given order.
     *
     * @param sort the sort order to use
     * @return a new [PanacheQuery] instance to find all entities of this type.
     * @see .findAll
     * @see .listAll
     * @see .streamAll
     */
    @GenerateBridge
    fun findAll(sort: Sort?): PanacheQuery<Entity> {
        throw KotlinJpaOperations.INSTANCE.implementationInjectionMissing()
    }

    /**
     * Find entities matching a query, with optional indexed parameters.
     * This method is a shortcut for `find(query, params).list()`.
     *
     * @param query a [query string][io.quarkus.hibernate.reactive.panache]
     * @param params optional sequence of indexed parameters
     * @return a [List] containing all results, without paging
     * @see .list
     * @see .list
     * @see .list
     * @see .find
     * @see .stream
     */
    @GenerateBridge
    fun list(
        query: String?,
        vararg params: Any?
    ): Uni<List<Entity>> {
        throw KotlinJpaOperations.INSTANCE.implementationInjectionMissing()
    }

    /**
     * Find entities matching a query and the given sort options, with optional indexed parameters.
     * This method is a shortcut for `find(query, sort, params).list()`.
     *
     * @param query a [query string][io.quarkus.hibernate.reactive.panache]
     * @param sort the sort strategy to use
     * @param params optional sequence of indexed parameters
     * @return a [List] containing all results, without paging
     * @see .list
     * @see .list
     * @see .list
     * @see .find
     * @see .stream
     */
    @GenerateBridge
    fun list(
        query: String?,
        sort: Sort?,
        vararg params: Any?
    ): Uni<List<Entity>> {
        throw KotlinJpaOperations.INSTANCE.implementationInjectionMissing()
    }

    /**
     * Find entities matching a query, with named parameters.
     * This method is a shortcut for `find(query, params).list()`.
     *
     * @param query a [query string][io.quarkus.hibernate.reactive.panache]
     * @param params [Map] of named parameters
     * @return a [List] containing all results, without paging
     * @see .list
     * @see .list
     * @see .list
     * @see .find
     * @see .stream
     */
    @GenerateBridge
    fun list(
        query: String?,
        params: Map<String?, Any?>?
    ): Uni<List<Entity>> {
        throw KotlinJpaOperations.INSTANCE.implementationInjectionMissing()
    }

    /**
     * Find entities matching a query and the given sort options, with named parameters.
     * This method is a shortcut for `find(query, sort, params).list()`.
     *
     * @param query a [query string][io.quarkus.hibernate.reactive.panache]
     * @param sort the sort strategy to use
     * @param params [Map] of indexed parameters
     * @return a [List] containing all results, without paging
     * @see .list
     * @see .list
     * @see .list
     * @see .find
     * @see .stream
     */
    @GenerateBridge
    fun list(
        query: String?,
        sort: Sort?,
        params: Map<String?, Any?>?
    ): Uni<List<Entity>> {
        throw KotlinJpaOperations.INSTANCE.implementationInjectionMissing()
    }

    /**
     * Find entities matching a query, with named parameters.
     * This method is a shortcut for `find(query, params).list()`.
     *
     * @param query a [query string][io.quarkus.hibernate.reactive.panache]
     * @param params [Parameters] of named parameters
     * @return a [List] containing all results, without paging
     * @see .list
     * @see .list
     * @see .list
     * @see .find
     * @see .stream
     */
    @GenerateBridge
    fun list(
        query: String?,
        params: Parameters?
    ): Uni<List<Entity>> {
        throw KotlinJpaOperations.INSTANCE.implementationInjectionMissing()
    }

    /**
     * Find entities matching a query and the given sort options, with named parameters.
     * This method is a shortcut for `find(query, sort, params).list()`.
     *
     * @param query a [query string][io.quarkus.hibernate.reactive.panache]
     * @param sort the sort strategy to use
     * @param params [Parameters] of indexed parameters
     * @return a [List] containing all results, without paging
     * @see .list
     * @see .list
     * @see .list
     * @see .find
     * @see .stream
     */
    @GenerateBridge
    fun list(
        query: String?,
        sort: Sort?,
        params: Parameters?
    ): Uni<List<Entity>> {
        throw KotlinJpaOperations.INSTANCE.implementationInjectionMissing()
    }

    /**
     * Find all entities of this type.
     * This method is a shortcut for `findAll().list()`.
     *
     * @return a [List] containing all results, without paging
     * @see .listAll
     * @see .findAll
     * @see .streamAll
     */
    @GenerateBridge
    fun listAll(): Uni<List<Entity>> {
        throw KotlinJpaOperations.INSTANCE.implementationInjectionMissing()
    }

    /**
     * Find all entities of this type, in the given order.
     * This method is a shortcut for `findAll(sort).list()`.
     *
     * @param sort the sort order to use
     * @return a [List] containing all results, without paging
     * @see .listAll
     * @see .findAll
     * @see .streamAll
     */
    @GenerateBridge
    fun listAll(sort: Sort?): Uni<List<Entity>> {
        throw KotlinJpaOperations.INSTANCE.implementationInjectionMissing()
    }

    /**
     * Find entities matching a query, with optional indexed parameters.
     * This method is a shortcut for `find(query, params).stream()`.
     * It requires a transaction to work.
     * Without a transaction, the underlying cursor can be closed before the end of the stream.
     *
     * @param query a [query string][io.quarkus.hibernate.reactive.panache]
     * @param params optional sequence of indexed parameters
     * @return a [Stream] containing all results, without paging
     * @see .stream
     * @see .stream
     * @see .stream
     * @see .find
     * @see .list
     */
    @GenerateBridge
    fun stream(
        query: String?,
        vararg params: Any?
    ): Multi<Entity> {
        throw KotlinJpaOperations.INSTANCE.implementationInjectionMissing()
    }

    /**
     * Find entities matching a query and the given sort options, with optional indexed parameters.
     * This method is a shortcut for `find(query, sort, params).stream()`.
     * It requires a transaction to work.
     * Without a transaction, the underlying cursor can be closed before the end of the stream.
     *
     * @param query a [query string][io.quarkus.hibernate.reactive.panache]
     * @param sort the sort strategy to use
     * @param params optional sequence of indexed parameters
     * @return a [Stream] containing all results, without paging
     * @see .stream
     * @see .stream
     * @see .stream
     * @see .find
     * @see .list
     */
    @GenerateBridge
    fun stream(
        query: String?,
        sort: Sort?,
        vararg params: Any?
    ): Multi<Entity> {
        throw KotlinJpaOperations.INSTANCE.implementationInjectionMissing()
    }

    /**
     * Find entities matching a query, with named parameters.
     * This method is a shortcut for `find(query, params).stream()`.
     * It requires a transaction to work.
     * Without a transaction, the underlying cursor can be closed before the end of the stream.
     *
     * @param query a [query string][io.quarkus.hibernate.reactive.panache]
     * @param params [Map] of named parameters
     * @return a [Stream] containing all results, without paging
     * @see .stream
     * @see .stream
     * @see .stream
     * @see .find
     * @see .list
     */
    @GenerateBridge
    fun stream(
        query: String?,
        params: Map<String?, Any?>?
    ): Multi<Entity> {
        throw KotlinJpaOperations.INSTANCE.implementationInjectionMissing()
    }

    /**
     * Find entities matching a query and the given sort options, with named parameters.
     * This method is a shortcut for `find(query, sort, params).stream()`.
     * It requires a transaction to work.
     * Without a transaction, the underlying cursor can be closed before the end of the stream.
     *
     * @param query a [query string][io.quarkus.hibernate.reactive.panache]
     * @param sort the sort strategy to use
     * @param params [Map] of indexed parameters
     * @return a [Stream] containing all results, without paging
     * @see .stream
     * @see .stream
     * @see .stream
     * @see .find
     * @see .list
     */
    @GenerateBridge
    fun stream(
        query: String?,
        sort: Sort?,
        params: Map<String?, Any?>?
    ): Multi<Entity> {
        throw KotlinJpaOperations.INSTANCE.implementationInjectionMissing()
    }

    /**
     * Find entities matching a query, with named parameters.
     * This method is a shortcut for `find(query, params).stream()`.
     * It requires a transaction to work.
     * Without a transaction, the underlying cursor can be closed before the end of the stream.
     *
     * @param query a [query string][io.quarkus.hibernate.reactive.panache]
     * @param params [Parameters] of named parameters
     * @return a [Stream] containing all results, without paging
     * @see .stream
     * @see .stream
     * @see .stream
     * @see .find
     * @see .list
     */
    @GenerateBridge
    fun stream(
        query: String?,
        params: Parameters?
    ): Multi<Entity> {
        throw KotlinJpaOperations.INSTANCE.implementationInjectionMissing()
    }

    /**
     * Find entities matching a query and the given sort options, with named parameters.
     * This method is a shortcut for `find(query, sort, params).stream()`.
     * It requires a transaction to work.
     * Without a transaction, the underlying cursor can be closed before the end of the stream.
     *
     * @param query a [query string][io.quarkus.hibernate.reactive.panache]
     * @param sort the sort strategy to use
     * @param params [Parameters] of indexed parameters
     * @return a [Stream] containing all results, without paging
     * @see .stream
     * @see .stream
     * @see .stream
     * @see .find
     * @see .list
     */
    @GenerateBridge
    fun stream(
        query: String?,
        sort: Sort?,
        params: Parameters?
    ): Multi<Entity> {
        throw KotlinJpaOperations.INSTANCE.implementationInjectionMissing()
    }

    /**
     * Find all entities of this type.
     * This method is a shortcut for `findAll().stream()`.
     * It requires a transaction to work.
     * Without a transaction, the underlying cursor can be closed before the end of the stream.
     *
     * @return a [Stream] containing all results, without paging
     * @see .streamAll
     * @see .findAll
     * @see .listAll
     */
    @GenerateBridge
    fun streamAll(): Multi<Entity> {
        throw KotlinJpaOperations.INSTANCE.implementationInjectionMissing()
    }

    /**
     * Find all entities of this type, in the given order.
     * This method is a shortcut for `findAll(sort).stream()`.
     * It requires a transaction to work.
     * Without a transaction, the underlying cursor can be closed before the end of the stream.
     *
     * @param sort the sort order to use
     * @return a [Stream] containing all results, without paging
     * @see .streamAll
     * @see .findAll
     * @see .listAll
     */
    @GenerateBridge
    fun streamAll(sort: Sort?): Multi<Entity> {
        throw KotlinJpaOperations.INSTANCE.implementationInjectionMissing()
    }

    /**
     * Counts the number of this type of entity in the database.
     *
     * @return the number of this type of entity in the database.
     * @see .count
     * @see .count
     * @see .count
     */
    @GenerateBridge
    fun count(): Uni<Long?>? {
        throw KotlinJpaOperations.INSTANCE.implementationInjectionMissing()
    }

    /**
     * Counts the number of this type of entity matching the given query, with optional indexed parameters.
     *
     * @param query a [query string][io.quarkus.hibernate.reactive.panache]
     * @param params optional sequence of indexed parameters
     * @return the number of entities counted.
     * @see .count
     * @see .count
     * @see .count
     */
    @GenerateBridge
    fun count(query: String?, vararg params: Any?): Uni<Long?>? {
        throw KotlinJpaOperations.INSTANCE.implementationInjectionMissing()
    }

    /**
     * Counts the number of this type of entity matching the given query, with named parameters.
     *
     * @param query a [query string][io.quarkus.hibernate.reactive.panache]
     * @param params [Map] of named parameters
     * @return the number of entities counted.
     * @see .count
     * @see .count
     * @see .count
     */
    @GenerateBridge
    fun count(query: String?, params: Map<String?, Any?>?): Uni<Long?>? {
        throw KotlinJpaOperations.INSTANCE.implementationInjectionMissing()
    }

    /**
     * Counts the number of this type of entity matching the given query, with named parameters.
     *
     * @param query a [query string][io.quarkus.hibernate.reactive.panache]
     * @param params [Parameters] of named parameters
     * @return the number of entities counted.
     * @see .count
     * @see .count
     * @see .count
     */
    @GenerateBridge
    fun count(query: String?, params: Parameters?): Uni<Long?>? {
        throw KotlinJpaOperations.INSTANCE.implementationInjectionMissing()
    }

    /**
     * Delete all entities of this type from the database.
     *
     * WARNING: the default implementation of this method uses a bulk delete query and ignores
     * cascading rules from the JPA model.
     *
     * @return the number of entities deleted.
     * @see .delete
     * @see .delete
     * @see .delete
     */
    @GenerateBridge
    fun deleteAll(): Uni<Long?>? {
        throw KotlinJpaOperations.INSTANCE.implementationInjectionMissing()
    }

    /**
     * Delete an entity of this type by ID.
     *
     * @param id the ID of the entity to delete.
     * @return false if the entity was not deleted (not found).
     */
    @GenerateBridge
    fun deleteById(id: Any): Uni<Boolean> {
        throw KotlinJpaOperations.INSTANCE.implementationInjectionMissing()
    }

    /**
     * Delete all entities of this type matching the given query, with optional indexed parameters.
     *
     * WARNING: the default implementation of this method uses a bulk delete query and ignores
     * cascading rules from the JPA model.
     *
     * @param query a [query string][io.quarkus.hibernate.reactive.panache]
     * @param params optional sequence of indexed parameters
     * @return the number of entities deleted.
     * @see .deleteAll
     * @see .delete
     * @see .delete
     */
    @GenerateBridge
    fun delete(query: String?, vararg params: Any?): Uni<Long?>? {
        throw KotlinJpaOperations.INSTANCE.implementationInjectionMissing()
    }

    /**
     * Delete all entities of this type matching the given query, with named parameters.
     *
     * WARNING: the default implementation of this method uses a bulk delete query and ignores
     * cascading rules from the JPA model.
     *
     * @param query a [query string][io.quarkus.hibernate.reactive.panache]
     * @param params [Map] of named parameters
     * @return the number of entities deleted.
     * @see .deleteAll
     * @see .delete
     * @see .delete
     */
    @GenerateBridge
    fun delete(query: String?, params: Map<String?, Any?>?): Uni<Long?>? {
        throw KotlinJpaOperations.INSTANCE.implementationInjectionMissing()
    }

    /**
     * Delete all entities of this type matching the given query, with named parameters.
     *
     * WARNING: the default implementation of this method uses a bulk delete query and ignores
     * cascading rules from the JPA model.
     *
     * @param query a [query string][io.quarkus.hibernate.reactive.panache]
     * @param params [Parameters] of named parameters
     * @return the number of entities deleted.
     * @see .deleteAll
     * @see .delete
     * @see .delete
     */
    @GenerateBridge
    fun delete(query: String?, params: Parameters?): Uni<Long?>? {
        throw KotlinJpaOperations.INSTANCE.implementationInjectionMissing()
    }

    /**
     * Persist all given entities.
     *
     * @param entities the entities to persist
     * @return
     * @see .persist
     * @see .persist
     * @see .persist
     */
    @GenerateBridge(callSuperMethod = true)
    fun persist(entities: Iterable<*>?): Uni<Void?>? {
        return KotlinJpaOperations.INSTANCE.persist(entities)
    }

    /**
     * Persist all given entities.
     *
     * @param entities the entities to persist
     * @return
     * @see .persist
     * @see .persist
     * @see .persist
     */
    @GenerateBridge(callSuperMethod = true)
    fun persist(entities: Stream<*>?): Uni<Void?>? {
        return KotlinJpaOperations.INSTANCE.persist(entities)
    }

    /**
     * Persist all given entities.
     *
     * @param entities the entities to persist
     * @return
     * @see .persist
     * @see .persist
     * @see .persist
     */
    @GenerateBridge(callSuperMethod = true)
    fun persist(firstEntity: Any?, vararg entities: Any?): Uni<Void?>? {
        return KotlinJpaOperations.INSTANCE.persist(firstEntity, *entities)
    }

    /**
     * Update all entities of this type matching the given query, with optional indexed parameters.
     *
     * @param query a [query string][io.quarkus.hibernate.reactive.panache]
     * @param params optional sequence of indexed parameters
     * @return the number of entities updated.
     * @see .update
     * @see .update
     */
    @GenerateBridge
    fun update(query: String?, vararg params: Any?): Uni<Int?>? {
        throw KotlinJpaOperations.INSTANCE.implementationInjectionMissing()
    }

    /**
     * Update all entities of this type matching the given query, with named parameters.
     *
     * @param query a [query string][io.quarkus.hibernate.reactive.panache]
     * @param params [Map] of named parameters
     * @return the number of entities updated.
     * @see .update
     * @see .update
     */
    @GenerateBridge
    fun update(query: String?, params: Map<String?, Any?>?): Uni<Int?>? {
        throw KotlinJpaOperations.INSTANCE.implementationInjectionMissing()
    }

    /**
     * Update all entities of this type matching the given query, with named parameters.
     *
     * @param query a [query string][io.quarkus.hibernate.reactive.panache]
     * @param params [Parameters] of named parameters
     * @return the number of entities updated.
     * @see .update
     * @see .update
     */
    @GenerateBridge
    fun update(query: String?, params: Parameters?): Uni<Int?>? {
        throw KotlinJpaOperations.INSTANCE.implementationInjectionMissing()
    }
}
