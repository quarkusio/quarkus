package io.quarkus.hibernate.orm.panache.kotlin

import io.quarkus.hibernate.orm.panache.kotlin.runtime.JpaOperations
import io.quarkus.panache.common.Parameters
import io.quarkus.panache.common.Sort
import io.quarkus.panache.common.impl.GenerateBridge
import java.util.stream.Stream
import javax.persistence.EntityManager
import javax.persistence.LockModeType

/**
 * Defines methods to be used via the companion objects of entities.
 *
 * @param Entity the entity type
 */
interface PanacheCompanion<Entity : PanacheEntityBase, Id: Any> {

    // Operations
    /**
     * Returns the default [EntityManager] for extra operations (eg. CriteriaQueries)
     *
     * @return the default [EntityManager]
     */
    fun entityManager(): EntityManager = JpaOperations.getEntityManager()

    /**
     * Find an entity of this type by ID.
     *
     * @param id the ID of the entity to find.
     * @return the entity found, or `null` if not found.
     */
    @GenerateBridge(targetReturnTypeErased = true)
    fun findById(id: Id): Entity? = injectionMissing()

    /**
     * Find an entity of this type by ID and lock it.
     *
     * @param id the ID of the entity to find.
     * @param lockModeType the locking strategy to be used when retrieving the entity.
     * @return the entity found, or `null` if not found.
     */
    @GenerateBridge(targetReturnTypeErased = true)
    fun findById(id: Id, lockModeType: LockModeType): Entity? = injectionMissing()

    /**
     * Find entities using a query, with optional indexed parameters.
     *
     * @param query a query string
     * @param params optional sequence of indexed parameters
     * @return a new [PanacheQuery] instance for the given query
     * @see [PanacheCompanion.find]
     * @see [PanacheCompanion.list]
     * @see [PanacheCompanion.stream]
     */
    @GenerateBridge
    fun find(query: String, vararg params: Any): PanacheQuery<Entity> = injectionMissing()

    /**
     * Find entities using a query and the given sort options with optional indexed parameters.
     *
     * @param query a query string
     * @param sort the sort strategy to use
     * @param params optional sequence of indexed parameters
     * @return a new [PanacheQuery] instance for the given query
     * @see [PanacheCompanion.find]
     * @see [PanacheCompanion.list]
     * @see [PanacheCompanion.stream]
     */
    @GenerateBridge
    fun find(query: String, sort: Sort, vararg params: Any): PanacheQuery<Entity> = injectionMissing()

    /**
     * Find entities using a query, with named parameters.
     *
     * @param query a query string
     * @param params [Map] of named parameters
     * @return a new [PanacheQuery] instance for the given query
     * @see [PanacheCompanion.find]
     * @see [PanacheCompanion.list]
     * @see [PanacheCompanion.stream]
     */
    @GenerateBridge
    fun find(query: String, params: Map<String, Any>): PanacheQuery<Entity> = injectionMissing()

    /**
     * Find entities using a query and the given sort options, with named parameters.
     *
     * @param query a query string
     * @param sort the sort strategy to use
     * @param params [Map] of indexed parameters
     * @return a new [PanacheQuery] instance for the given query
     * @see [PanacheCompanion.find]
     * @see [PanacheCompanion.list]
     * @see [PanacheCompanion.stream]
     */
    @GenerateBridge
    fun find(query: String, sort: Sort, params: Map<String, Any>): PanacheQuery<Entity> = injectionMissing()

    /**
     * Find entities using a query, with named parameters.
     *
     * @param query a query string
     * @param params Parameters of named parameters
     * @return a new [PanacheQuery] instance for the given query
     * @see [PanacheCompanion.find]
     * @see [PanacheCompanion.list]
     * @see [PanacheCompanion.stream]
     */
    @GenerateBridge
    fun find(query: String, params: Parameters): PanacheQuery<Entity> = injectionMissing()

    /**
     * Find entities using a query and the given sort options, with named parameters.
     *
     * @param query a query string
     * @param sort the sort strategy to use
     * @param params Parameters of indexed parameters
     * @return a new [PanacheQuery] instance for the given query
     * @see [PanacheCompanion.find]
     * @see [PanacheCompanion.list]
     * @see [PanacheCompanion.stream]
     */
    @GenerateBridge
    fun find(query: String, sort: Sort, params: Parameters): PanacheQuery<Entity> = injectionMissing()

    /**
     * Find all entities of this type.
     *
     * @return a new [PanacheQuery] instance to find all entities of this type.
     * @see [PanacheCompanion.findAll]
     * @see [PanacheCompanion.listAll]
     * @see [PanacheCompanion.streamAll]
     */
    @GenerateBridge
    fun findAll(): PanacheQuery<Entity> = injectionMissing()

    /**
     * Find all entities of this type, in the given order.
     *
     * @param sort the sort order to use
     * @return a new [PanacheQuery] instance to find all entities of this type.
     * @see [PanacheCompanion.findAll]
     * @see [PanacheCompanion.listAll]
     * @see [PanacheCompanion.streamAll]
     */
    @GenerateBridge
    fun findAll(sort: Sort): PanacheQuery<Entity> = injectionMissing()

    /**
     * Find entities matching a query, with optional indexed parameters.
     * This method is a shortcut for `find(query, params).list()`.
     *
     * @param query a query string
     * @param params optional sequence of indexed parameters
     * @return a [List] containing all results, without paging
     * @see [PanacheCompanion.list]
     * @see [PanacheCompanion.find]
     * @see [PanacheCompanion.stream]
     */
    @GenerateBridge
    fun list(query: String, vararg params: Any): List<Entity> = injectionMissing()

    /**
     * Find entities matching a query and the given sort options, with optional indexed parameters.
     * This method is a shortcut for `find(query, sort, params).list()`.
     *
     * @param query a query string
     * @param sort the sort strategy to use
     * @param params optional sequence of indexed parameters
     * @return a [List] containing all results, without paging
     * @see [PanacheCompanion.list]
     * @see [PanacheCompanion.find]
     * @see [PanacheCompanion.stream]
     */
    @GenerateBridge
    fun list(query: String, sort: Sort, vararg params: Any): List<Entity> = injectionMissing()

    /**
     * Find entities matching a query, with named parameters.
     * This method is a shortcut for `find(query, params).list()`.
     *
     * @param query a query string
     * @param params [Map] of named parameters
     * @return a [List] containing all results, without paging
     * @see [PanacheCompanion.list]
     * @see [PanacheCompanion.find]
     * @see [PanacheCompanion.stream]
     */
    @GenerateBridge
    fun list(query: String, params: Map<String, Any>): List<Entity> = injectionMissing()

    /**
     * Find entities matching a query and the given sort options, with named parameters.
     * This method is a shortcut for `find(query, sort, params).list()`.
     *
     * @param query a query string
     * @param sort the sort strategy to use
     * @param params [Map] of indexed parameters
     * @return a [List] containing all results, without paging
     * @see [PanacheCompanion.list]
     * @see [PanacheCompanion.find]
     * @see [PanacheCompanion.stream]
     */
    @GenerateBridge
    fun list(query: String, sort: Sort, params: Map<String, Any>): List<Entity> = injectionMissing()

    /**
     * Find entities matching a query, with named parameters.
     * This method is a shortcut for `find(query, params).list()`.
     *
     * @param query a query string
     * @param params Parameters of named parameters
     * @return a [List] containing all results, without paging
     * @see [PanacheCompanion.list]
     * @see [PanacheCompanion.find]
     * @see [PanacheCompanion.stream]
     */
    @GenerateBridge
    fun list(query: String, params: Parameters): List<Entity> = injectionMissing()

    /**
     * Find entities matching a query and the given sort options, with named parameters.
     * This method is a shortcut for `find(query, sort, params).list()`.
     *
     * @param query a query string
     * @param sort the sort strategy to use
     * @param params Parameters of indexed parameters
     * @return a [List] containing all results, without paging
     * @see [PanacheCompanion.list]
     * @see [PanacheCompanion.find]
     * @see [PanacheCompanion.stream]
     */
    @GenerateBridge
    fun list(query: String, sort: Sort, params: Parameters): List<Entity> = injectionMissing()

    /**
     * Find all entities of this type.
     * This method is a shortcut for `findAll().list()`.
     *
     * @return a [List] containing all results, without paging
     * @see [PanacheCompanion.listAll]
     * @see [PanacheCompanion.findAll]
     * @see [PanacheCompanion.streamAll]
     */
    @GenerateBridge
    fun listAll(): List<Entity> = injectionMissing()

    /**
     * Find all entities of this type, in the given order.
     * This method is a shortcut for `findAll(sort).list()`.
     *
     * @param sort the sort order to use
     * @return a [List] containing all results, without paging
     * @see [PanacheCompanion.listAll]
     * @see [PanacheCompanion.findAll]
     * @see [PanacheCompanion.streamAll]
     */
    @GenerateBridge
    fun listAll(sort: Sort): List<Entity> = injectionMissing()

    /**
     * Find entities matching a query, with optional indexed parameters.
     * This method is a shortcut for `find(query, params).stream()`.
     * It requires a transaction to work.
     * Without a transaction, the underlying cursor can be closed before the end of the stream.
     *
     * @param query a query string
     * @param params optional sequence of indexed parameters
     * @return a Stream containing all results, without paging
     * @see [PanacheCompanion.stream]
     * @see [PanacheCompanion.find]
     * @see [PanacheCompanion.list]
     */
    @GenerateBridge
    fun stream(query: String, vararg params: Any): Stream<Entity> = injectionMissing()

    /**
     * Find entities matching a query and the given sort options, with optional indexed parameters.
     * This method is a shortcut for `find(query, sort, params).stream()`.
     * It requires a transaction to work.
     * Without a transaction, the underlying cursor can be closed before the end of the stream.
     *
     * @param query a query string
     * @param sort the sort strategy to use
     * @param params optional sequence of indexed parameters
     * @return a Stream containing all results, without paging
     * @see [PanacheCompanion.stream]
     * @see [PanacheCompanion.find]
     * @see [PanacheCompanion.list]
     */
    @GenerateBridge
    fun stream(query: String, sort: Sort, vararg params: Any): Stream<Entity> = injectionMissing()

    /**
     * Find entities matching a query, with named parameters.
     * This method is a shortcut for `find(query, params).stream()`.
     * It requires a transaction to work.
     * Without a transaction, the underlying cursor can be closed before the end of the stream.
     *
     * @param query a query string
     * @param params [Map] of named parameters
     * @return a Stream containing all results, without paging
     * @see [PanacheCompanion.stream]
     * @see [PanacheCompanion.find]
     * @see [PanacheCompanion.list]
     */
    @GenerateBridge
    fun stream(query: String, params: Map<String, Any>): Stream<Entity> = injectionMissing()

    /**
     * Find entities matching a query and the given sort options, with named parameters.
     * This method is a shortcut for `find(query, sort, params).stream()`.
     * It requires a transaction to work.
     * Without a transaction, the underlying cursor can be closed before the end of the stream.
     *
     * @param query a query string
     * @param sort the sort strategy to use
     * @param params [Map] of indexed parameters
     * @return a Stream containing all results, without paging
     * @see [PanacheCompanion.stream]
     * @see [PanacheCompanion.find]
     * @see [PanacheCompanion.list]
     */
    @GenerateBridge
    fun stream(query: String, sort: Sort, params: Map<String, Any>): Stream<Entity> = injectionMissing()

    /**
     * Find entities matching a query, with named parameters.
     * This method is a shortcut for `find(query, params).stream()`.
     * It requires a transaction to work.
     * Without a transaction, the underlying cursor can be closed before the end of the stream.
     *
     * @param query a query string
     * @param params Parameters of named parameters
     * @return a Stream containing all results, without paging
     * @see [PanacheCompanion.stream]
     * @see [PanacheCompanion.find]
     * @see [PanacheCompanion.list]
     */
    @GenerateBridge
    fun stream(query: String, params: Parameters): Stream<Entity> = injectionMissing()

    /**
     * Find entities matching a query and the given sort options, with named parameters.
     * This method is a shortcut for `find(query, sort, params).stream()`.
     * It requires a transaction to work.
     * Without a transaction, the underlying cursor can be closed before the end of the stream.
     *
     * @param query a query string
     * @param sort the sort strategy to use
     * @param params Parameters of indexed parameters
     * @return a Stream containing all results, without paging
     * @see [PanacheCompanion.stream]
     * @see [PanacheCompanion.find]
     * @see [PanacheCompanion.list]
     */
    @GenerateBridge
    fun stream(query: String, sort: Sort, params: Parameters): Stream<Entity> = injectionMissing()

    /**
     * Find all entities of this type.
     * This method is a shortcut for `findAll().stream()`.
     * It requires a transaction to work.
     * Without a transaction, the underlying cursor can be closed before the end of the stream.
     *
     * @return a Stream containing all results, without paging
     * @see [PanacheCompanion.streamAll]
     * @see [PanacheCompanion.findAll]
     * @see [PanacheCompanion.listAll]
     */
    @GenerateBridge
    fun streamAll(): Stream<Entity> = injectionMissing()

    /**
     * Find all entities of this type, in the given order.
     * This method is a shortcut for `findAll(sort).stream()`.
     * It requires a transaction to work.
     * Without a transaction, the underlying cursor can be closed before the end of the stream.
     *
     * @param sort the sort order to use
     * @return a Stream containing all results, without paging
     * @see [PanacheCompanion.streamAll]
     * @see [PanacheCompanion.findAll]
     * @see [PanacheCompanion.listAll]
     */
    @GenerateBridge
    fun streamAll(sort: Sort): Stream<Entity> = injectionMissing()

    /**
     * Counts the number of this type of entity in the database.
     *
     * @return the number of this type of entity in the database.
     * @see [PanacheCompanion.count]
     */
    @GenerateBridge
    fun count(): Long = injectionMissing()

    /**
     * Counts the number of this type of entity matching the given query, with optional indexed parameters.
     *
     * @param query a query string
     * @param params optional sequence of indexed parameters
     * @return the number of entities counted.
     * @see [PanacheCompanion.count]
     */
    @GenerateBridge
    fun count(query: String, vararg params: Any): Long = injectionMissing()

    /**
     * Counts the number of this type of entity matching the given query, with named parameters.
     *
     * @param query a query string
     * @param params [Map] of named parameters
     * @return the number of entities counted.
     * @see [PanacheCompanion.count]
     */
    @GenerateBridge
    fun count(query: String, params: Map<String, Any>): Long = injectionMissing()

    /**
     * Counts the number of this type of entity matching the given query, with named parameters.
     *
     * @param query a query string
     * @param params Parameters of named parameters
     * @return the number of entities counted.
     * @see [PanacheCompanion.count]
     */
    @GenerateBridge
    fun count(query: String, params: Parameters): Long = injectionMissing()

    /**
     * Delete all entities of this type from the database.
     *
     * @return the number of entities deleted.
     * @see [PanacheCompanion.delete]
     */
    @GenerateBridge
    fun deleteAll(): Long = injectionMissing()

    /**
     * Delete all entities of this type matching the given query, with optional indexed parameters.
     *
     * @param query a query string
     * @param params optional sequence of indexed parameters
     * @return the number of entities deleted.
     * @see [PanacheCompanion.deleteAll]
     * @see [PanacheCompanion.delete]
     */
    @GenerateBridge
    fun delete(query: String, vararg params: Any): Long = injectionMissing()

    /**
     * Delete all entities of this type matching the given query, with named parameters.
     *
     * @param query a query string
     * @param params [Map] of named parameters
     * @return the number of entities deleted.
     * @see [PanacheCompanion.deleteAll]
     * @see [PanacheCompanion.delete]
     */
    @GenerateBridge
    fun delete(query: String, params: Map<String, Any>): Long = injectionMissing()

    /**
     * Delete all entities of this type matching the given query, with named parameters.
     *
     * @param query a query string
     * @param params Parameters of named parameters
     * @return the number of entities deleted.
     * @see [PanacheCompanion.deleteAll]
     * @see [PanacheCompanion.delete]
     */
    @GenerateBridge
    fun delete(query: String, params: Parameters): Long = injectionMissing()

    /**
     * Delete an entity of this type by ID.
     *
     * @param id the ID of the entity to delete.
     * @return false if the entity was not deleted (not found).
     */
    @GenerateBridge
    fun deleteById(id: Id): Boolean = throw JpaOperations.implementationInjectionMissing()

    /**
     * Persist all given entities.
     *
     * @param entities the entities to persist
     * @see [PanacheCompanion.persist]
     */
    fun persist(entities: Iterable<Entity>) {
        JpaOperations.persist(entities)
    }

    /**
     * Persist all given entities.
     *
     * @param entities the entities to persist
     * @see [PanacheCompanion.persist]
     */
    fun persist(entities: Stream<Entity>) {
        JpaOperations.persist(entities)
    }

    /**
     * Persist all given entities.
     *
     * @param entities the entities to persist
     * @see [PanacheCompanion.persist]
     */
    fun persist(firstEntity: Entity, vararg entities: Entity) {
        JpaOperations.persist(firstEntity, *entities)
    }

    /**
     * Update all entities of this type matching the given query, with optional indexed parameters.
     *
     * @param query a query string
     * @param params optional sequence of indexed parameters
     * @return the number of entities updated.
     * @see [PanacheCompanion.update]
     */
    @GenerateBridge
    fun update(query: String, vararg params: Any): Int = injectionMissing()

    /**
     * Update all entities of this type matching the given query, with named parameters.
     *
     * @param query a query string
     * @param params [Map] of named parameters
     * @return the number of entities updated.
     * @see [PanacheCompanion.update]
     */
    @GenerateBridge
    fun update(query: String, params: Map<String, Any>): Int = injectionMissing()

    /**
     * Update all entities of this type matching the given query, with named parameters.
     *
     * @param query a query string
     * @param params Parameters of named parameters
     * @return the number of entities updated.
     * @see [PanacheCompanion.update]
     */
    @GenerateBridge
    fun update(query: String, params: Parameters): Int = injectionMissing()
}
