package io.quarkus.hibernate.orm.panache.kotlin

import io.quarkus.hibernate.orm.panache.common.runtime.AbstractJpaOperations
import io.quarkus.hibernate.orm.panache.common.runtime.AbstractJpaOperations.implementationInjectionMissing
import io.quarkus.hibernate.orm.panache.kotlin.runtime.KotlinJpaOperations.Companion.INSTANCE
import io.quarkus.panache.common.Parameters
import io.quarkus.panache.common.Sort
import io.quarkus.panache.common.impl.GenerateBridge
import java.util.stream.Stream
import javax.persistence.EntityManager
import javax.persistence.LockModeType
import kotlin.reflect.KClass

/**
 * Represents a Repository for a specific type of entity `Entity`, with an ID type of `Id`. Implementing this interface
 * will gain you the exact same useful methods that are on [PanacheEntity] and [PanacheCompanion].
 *
 * @param Entity The type of entity to operate on
 * @param Id The ID type of the entity
 */
interface PanacheRepositoryBase<Entity : Any, Id: Any> {

    /**
     * Returns the [EntityManager] for the <Entity> for extra operations (eg. CriteriaQueries)
     *
     * @return the [EntityManager] for the <Entity>
     */
    @GenerateBridge
    fun getEntityManager(): EntityManager = throw implementationInjectionMissing()

    /**
     * Returns the [EntityManager] tied to the given class for extra operations (eg. CriteriaQueries)
     *
     * @return the [EntityManager] tied to the given class
     *
     * @deprecated use {@link Panache#getEntityManager(Class)} instead to access an entity manager for any entity class
     */
    @GenerateBridge
    @Deprecated(message="use Panache.getEntityManager(Class) instead to access an entity manager for any entity class")
    fun getEntityManager(clazz: KClass<Any>): EntityManager = throw implementationInjectionMissing()

    /**
     * Persist the given entity in the database, if not already persisted.
     *
     * @param entity the entity to persist.
     * @see [PanacheRepositoryBase.isPersistent]
     * @see [PanacheRepositoryBase.persist]
     */
    fun persist(entity: Entity) {
        INSTANCE.persist(entity)
    }

    /**
     * Persist the given entity in the database, if not already persisted.
     * Then flushes all pending changes to the database.
     *
     * @param entity the entity to persist.
     * @see [PanacheRepositoryBase.isPersistent]
     * @see [PanacheRepositoryBase.persist]
     */
    fun persistAndFlush(entity: Entity) {
        INSTANCE.persist(entity)
        INSTANCE.flush(entity)
    }

    /**
     * Delete the given entity from the database, if it is already persisted.
     *
     * @param entity the entity to delete.
     * @see [PanacheRepositoryBase.isPersistent]
     * @see [PanacheRepositoryBase.delete]
     * @see [PanacheRepositoryBase.deleteAll]
     */
    fun delete(entity: Entity) {
        INSTANCE.delete(entity)
    }

    /**
     * Returns true if the given entity is persistent in the database. If yes, all modifications to
     * its persistent fields will be automatically committed to the database at transaction
     * commit time.
     *
     * @param entity the entity to check
     * @return true if the entity is persistent in the database.
     */
    fun isPersistent(entity: Entity): Boolean = INSTANCE.isPersistent(entity)

    /**
     * Flushes all pending changes to the database using the default EntityManager.
     */
    fun flush() {
        getEntityManager().flush()
    }

    /**
     * Find an entity of this type by ID.
     *
     * @param id the ID of the entity to find.
     * @return the entity found, or `null` if not found.
     */
    @GenerateBridge(targetReturnTypeErased = true)
    fun findById(id: Id): Entity? = throw implementationInjectionMissing()

    /**
     * Find an entity of this type by ID and lock it.
     *
     * @param id the ID of the entity to find.
     * @param lockModeType the locking strategy to be used when retrieving the entity.
     * @return the entity found, or `null` if not found.
     */
    @GenerateBridge(targetReturnTypeErased = true)
    fun findById(id: Id, lockModeType: LockModeType): Entity? = throw implementationInjectionMissing()

    /**
     * Find entities using a query, with optional indexed parameters.
     *
     * @param query a query string
     * @param params optional sequence of indexed parameters
     * @return a new [PanacheQuery] instance for the given query
     * @see [PanacheRepositoryBase.find]
     * @see [PanacheRepositoryBase.list]
     * @see [PanacheRepositoryBase.stream]
     */
    @GenerateBridge
    fun find(query: String, vararg params: Any): PanacheQuery<Entity> = throw implementationInjectionMissing()

    /**
     * Find entities using a query and the given sort options, with optional indexed parameters.
     *
     * @param query a query string
     * @param sort the sort strategy to use
     * @param params optional sequence of indexed parameters
     * @return a new [PanacheQuery] instance for the given query
     * @see [PanacheRepositoryBase.find]
     * @see [PanacheRepositoryBase.list]
     * @see [PanacheRepositoryBase.stream]
     */
    @GenerateBridge
    fun find(query: String, sort: Sort, vararg params: Any): PanacheQuery<Entity> = throw implementationInjectionMissing()

    /**
     * Find entities using a query, with named parameters.
     *
     * @param query a query string
     * @param params [Map] of named parameters
     * @return a new [PanacheQuery] instance for the given query
     * @see [PanacheRepositoryBase.find]
     * @see [PanacheRepositoryBase.list]
     * @see [PanacheRepositoryBase.stream]
     */
    @GenerateBridge
    fun find(query: String, params: Map<String, Any>): PanacheQuery<Entity> = throw implementationInjectionMissing()

    /**
     * Find entities using a query and the given sort options, with named parameters.
     *
     * @param query a query string
     * @param sort the sort strategy to use
     * @param params [Map] of indexed parameters
     * @return a new [PanacheQuery] instance for the given query
     * @see [PanacheRepositoryBase.find]
     * @see [PanacheRepositoryBase.list]
     * @see [PanacheRepositoryBase.stream]
     */
    @GenerateBridge
    fun find(query: String, sort: Sort, params: Map<String, Any>): PanacheQuery<Entity> = throw implementationInjectionMissing()

    /**
     * Find entities using a query, with named parameters.
     *
     * @param query a query string
     * @param params Parameters of named parameters
     * @return a new [PanacheQuery] instance for the given query
     * @see [PanacheRepositoryBase.find]
     * @see [PanacheRepositoryBase.list]
     * @see [PanacheRepositoryBase.stream]
     */
    @GenerateBridge
    fun find(query: String, params: Parameters): PanacheQuery<Entity> = throw implementationInjectionMissing()

    /**
     * Find entities using a query and the given sort options, with named parameters.
     *
     * @param query a query string
     * @param sort the sort strategy to use
     * @param params Parameters of indexed parameters
     * @return a new [PanacheQuery] instance for the given query
     * @see [PanacheRepositoryBase.find]
     * @see [PanacheRepositoryBase.list]
     * @see [PanacheRepositoryBase.stream]
     */
    @GenerateBridge
    fun find(query: String, sort: Sort, params: Parameters): PanacheQuery<Entity> = throw implementationInjectionMissing()

    /**
     * Find all entities of this type.
     *
     * @return a new [PanacheQuery] instance to find all entities of this type.
     * @see [PanacheRepositoryBase.findAll]
     * @see [PanacheRepositoryBase.listAll]
     * @see [PanacheRepositoryBase.streamAll]
     */
    @GenerateBridge
    fun findAll(): PanacheQuery<Entity> = throw implementationInjectionMissing()

    /**
     * Find all entities of this type, in the given order.
     *
     * @param sort the sort order to use
     * @return a new [PanacheQuery] instance to find all entities of this type.
     * @see [PanacheRepositoryBase.findAll]
     * @see [PanacheRepositoryBase.listAll]
     * @see [PanacheRepositoryBase.streamAll]
     */
    @GenerateBridge
    fun findAll(sort: Sort): PanacheQuery<Entity> = throw implementationInjectionMissing()

    /**
     * Find entities matching a query, with optional indexed parameters.
     * This method is a shortcut for `find(query, params).list()`.
     *
     * @param query a query string
     * @param params optional sequence of indexed parameters
     * @return a [List] containing all results, without paging
     * @see [PanacheRepositoryBase.list]
     * @see [PanacheRepositoryBase.find]
     * @see [PanacheRepositoryBase.stream]
     */
    @GenerateBridge
    fun list(query: String, vararg params: Any): List<Entity> = throw implementationInjectionMissing()

    /**
     * Find entities matching a query and the given sort options, with optional indexed parameters.
     * This method is a shortcut for `find(query, sort, params).list()`.
     *
     * @param query a query string
     * @param sort the sort strategy to use
     * @param params optional sequence of indexed parameters
     * @return a [List] containing all results, without paging
     * @see [PanacheRepositoryBase.list]
     * @see [PanacheRepositoryBase.find]
     * @see [PanacheRepositoryBase.stream]
     */
    @GenerateBridge
    fun list(query: String, sort: Sort, vararg params: Any): List<Entity> = throw implementationInjectionMissing()

    /**
     * Find entities matching a query, with named parameters.
     * This method is a shortcut for `find(query, params).list()`.
     *
     * @param query a query string
     * @param params [Map] of named parameters
     * @return a [List] containing all results, without paging
     * @see [PanacheRepositoryBase.list]
     * @see [PanacheRepositoryBase.find]
     * @see [PanacheRepositoryBase.stream]
     */
    @GenerateBridge
    fun list(query: String, params: Map<String, Any>): List<Entity> = throw implementationInjectionMissing()

    /**
     * Find entities matching a query and the given sort options, with named parameters.
     * This method is a shortcut for `find(query, sort, params).list()`.
     *
     * @param query a query string
     * @param sort the sort strategy to use
     * @param params [Map] of indexed parameters
     * @return a [List] containing all results, without paging
     * @see [PanacheRepositoryBase.list]
     * @see [PanacheRepositoryBase.find]
     * @see [PanacheRepositoryBase.stream]
     */
    @GenerateBridge
    fun list(query: String, sort: Sort, params: Map<String, Any>): List<Entity> = throw implementationInjectionMissing()

    /**
     * Find entities matching a query, with named parameters.
     * This method is a shortcut for `find(query, params).list()`.
     *
     * @param query a query string
     * @param params Parameters of named parameters
     * @return a [List] containing all results, without paging
     * @see [PanacheRepositoryBase.list]
     * @see [PanacheRepositoryBase.find]
     * @see [PanacheRepositoryBase.stream]
     */
    @GenerateBridge
    fun list(query: String, params: Parameters): List<Entity> = throw implementationInjectionMissing()

    /**
     * Find entities matching a query and the given sort options, with named parameters.
     * This method is a shortcut for `find(query, sort, params).list()`.
     *
     * @param query a query string
     * @param sort the sort strategy to use
     * @param params Parameters of indexed parameters
     * @return a [List] containing all results, without paging
     * @see [PanacheRepositoryBase.list]
     * @see [PanacheRepositoryBase.find]
     * @see [PanacheRepositoryBase.stream]
     */
    @GenerateBridge
    fun list(query: String, sort: Sort, params: Parameters): List<Entity> = throw implementationInjectionMissing()

    /**
     * Find all entities of this type.
     * This method is a shortcut for `findAll().list()`.
     *
     * @return a [List] containing all results, without paging
     * @see [PanacheRepositoryBase.listAll]
     * @see [PanacheRepositoryBase.findAll]
     * @see [PanacheRepositoryBase.streamAll]
     */
    @GenerateBridge
    fun listAll(): List<Entity> = throw implementationInjectionMissing()

    /**
     * Find all entities of this type, in the given order.
     * This method is a shortcut for `findAll(sort).list()`.
     *
     * @param sort the sort order to use
     * @return a [List] containing all results, without paging
     * @see [PanacheRepositoryBase.listAll]
     * @see [PanacheRepositoryBase.findAll]
     * @see [PanacheRepositoryBase.streamAll]
     */
    @GenerateBridge
    fun listAll(sort: Sort): List<Entity> = throw implementationInjectionMissing()

    /**
     * Find entities matching a query, with optional indexed parameters.
     * This method is a shortcut for `find(query, params).stream()`.
     * It requires a transaction to work.
     * Without a transaction, the underlying cursor can be closed before the end of the stream.
     *
     * @param query a query string
     * @param params optional sequence of indexed parameters
     * @return a Stream containing all results, without paging
     * @see [PanacheRepositoryBase.stream]
     * @see [PanacheRepositoryBase.find]
     * @see [PanacheRepositoryBase.list]
     */
    @GenerateBridge
    fun stream(query: String, vararg params: Any): Stream<Entity> = throw implementationInjectionMissing()

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
     * @see [PanacheRepositoryBase.stream]
     * @see [PanacheRepositoryBase.find]
     * @see [PanacheRepositoryBase.list]
     */
    @GenerateBridge
    fun stream(query: String, sort: Sort, vararg params: Any): Stream<Entity> = throw implementationInjectionMissing()

    /**
     * Find entities matching a query, with named parameters.
     * This method is a shortcut for `find(query, params).stream()`.
     * It requires a transaction to work.
     * Without a transaction, the underlying cursor can be closed before the end of the stream.
     *
     * @param query a query string
     * @param params [Map] of named parameters
     * @return a Stream containing all results, without paging
     * @see [PanacheRepositoryBase.stream]
     * @see [PanacheRepositoryBase.find]
     * @see [PanacheRepositoryBase.list]
     */
    @GenerateBridge
    fun stream(query: String, params: Map<String, Any>): Stream<Entity> = throw implementationInjectionMissing()

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
     * @see [PanacheRepositoryBase.stream]
     * @see [PanacheRepositoryBase.find]
     * @see [PanacheRepositoryBase.list]
     */
    @GenerateBridge
    fun stream(query: String, sort: Sort, params: Map<String, Any>): Stream<Entity> = throw implementationInjectionMissing()

    /**
     * Find entities matching a query, with named parameters.
     * This method is a shortcut for `find(query, params).stream()`.
     * It requires a transaction to work.
     * Without a transaction, the underlying cursor can be closed before the end of the stream.
     *
     * @param query a query string
     * @param params Parameters of named parameters
     * @return a Stream containing all results, without paging
     * @see [PanacheRepositoryBase.stream]
     * @see [PanacheRepositoryBase.find]
     * @see [PanacheRepositoryBase.list]
     */
    @GenerateBridge
    fun stream(query: String, params: Parameters): Stream<Entity> = throw implementationInjectionMissing()

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
     * @see [PanacheRepositoryBase.stream]
     * @see [PanacheRepositoryBase.find]
     * @see [PanacheRepositoryBase.list]
     */
    @GenerateBridge
    fun stream(query: String, sort: Sort, params: Parameters): Stream<Entity> = throw implementationInjectionMissing()

    /**
     * Find all entities of this type.
     * This method is a shortcut for `findAll().stream()`.
     * It requires a transaction to work.
     * Without a transaction, the underlying cursor can be closed before the end of the stream.
     *
     * @return a Stream containing all results, without paging
     * @see [PanacheRepositoryBase.streamAll]
     * @see [PanacheRepositoryBase.findAll]
     * @see [PanacheRepositoryBase.listAll]
     */
    @GenerateBridge
    fun streamAll(): Stream<Entity> = throw implementationInjectionMissing()

    /**
     * Find all entities of this type, in the given order.
     * This method is a shortcut for `findAll(sort).stream()`.
     * It requires a transaction to work.
     * Without a transaction, the underlying cursor can be closed before the end of the stream.
     *
     * @param sort the sort order to use
     * @return a Stream containing all results, without paging
     * @see [PanacheRepositoryBase.streamAll]
     * @see [PanacheRepositoryBase.findAll]
     * @see [PanacheRepositoryBase.listAll]
     */
    @GenerateBridge
    fun streamAll(sort: Sort): Stream<Entity> = throw implementationInjectionMissing()

    /**
     * Counts the number of this type of entity in the database.
     *
     * @return the number of this type of entity in the database.
     * @see [PanacheRepositoryBase.count]
     */
    @GenerateBridge
    fun count(): Long = throw implementationInjectionMissing()

    /**
     * Counts the number of this type of entity matching the given query, with optional indexed parameters.
     *
     * @param query a query string
     * @param params optional sequence of indexed parameters
     * @return the number of entities counted.
     * @see [PanacheRepositoryBase.count]
     */
    @GenerateBridge
    fun count(query: String, vararg params: Any): Long = throw implementationInjectionMissing()

    /**
     * Counts the number of this type of entity matching the given query, with named parameters.
     *
     * @param query a query string
     * @param params [Map] of named parameters
     * @return the number of entities counted.
     * @see [PanacheRepositoryBase.count]
     */
    @GenerateBridge
    fun count(query: String, params: Map<String, Any>): Long = throw implementationInjectionMissing()

    /**
     * Counts the number of this type of entity matching the given query, with named parameters.
     *
     * @param query a query string
     * @param params Parameters of named parameters
     * @return the number of entities counted.
     * @see [PanacheRepositoryBase.count]
     */
    @GenerateBridge
    fun count(query: String, params: Parameters): Long = throw implementationInjectionMissing()

    /**
     * Delete all entities of this type from the database.
     *
     * WARNING: the default implementation of this method uses a bulk delete query and ignores
     * cascading rules from the JPA model.
     *
     * @return the number of entities deleted.
     * @see [PanacheRepositoryBase.delete]
     */
    @GenerateBridge
    fun deleteAll(): Long = throw implementationInjectionMissing()

    /**
     * Delete all entities of this type matching the given query, with optional indexed parameters.
     *
     * WARNING: the default implementation of this method uses a bulk delete query and ignores
     * cascading rules from the JPA model.
     *
     * @param query a query string
     * @param params optional sequence of indexed parameters
     * @return the number of entities deleted.
     * @see [PanacheRepositoryBase.deleteAll]
     * @see [PanacheRepositoryBase.delete]
     */
    @GenerateBridge
    fun delete(query: String, vararg params: Any): Long = throw implementationInjectionMissing()

    /**
     * Delete all entities of this type matching the given query, with named parameters.
     *
     * WARNING: the default implementation of this method uses a bulk delete query and ignores
     * cascading rules from the JPA model.
     *
     * @param query a query string
     * @param params [Map] of named parameters
     * @return the number of entities deleted.
     * @see [PanacheRepositoryBase.deleteAll]
     * @see [PanacheRepositoryBase.delete]
     */
    @GenerateBridge
    fun delete(query: String, params: Map<String, Any>): Long = throw implementationInjectionMissing()

    /**
     * Delete all entities of this type matching the given query, with named parameters.
     *
     * WARNING: the default implementation of this method uses a bulk delete query and ignores
     * cascading rules from the JPA model.
     *
     * @param query a query string
     * @param params Parameters of named parameters
     * @return the number of entities deleted.
     * @see [PanacheRepositoryBase.deleteAll]
     * @see [PanacheRepositoryBase.delete]
     */
    @GenerateBridge
    fun delete(query: String, params: Parameters): Long = throw implementationInjectionMissing()

    /**
     * Delete an entity of this type by ID.
     *
     * @param id the ID of the entity to delete.
     * @return false if the entity was not deleted (not found).
     */
    @GenerateBridge
    fun deleteById(id: Id): Boolean = throw implementationInjectionMissing()

    /**
     * Persist all given entities.
     *
     * @param entities the entities to persist
     * @see [PanacheRepositoryBase.persist]
     */
    fun persist(entities: Iterable<Entity>) {
        INSTANCE.persist(entities)
    }

    /**
     * Persist all given entities.
     *
     * @param entities the entities to persist
     * @see [PanacfluheRepositoryBase.persist]
     */
    fun persist(entities: Stream<Entity>) {
        INSTANCE.persist(entities)
    }

    /**
     * Persist all given entities.
     *
     * @param entities the entities to persist
     * @see [PanacheRepositoryBase.persist]
     */
    fun persist(firstEntity: Entity, vararg entities: Entity) {
        INSTANCE.persist(firstEntity, *entities)
    }

    /**
     * Update all entities of this type matching the given query, with optional indexed parameters.
     *
     * @param query a query string
     * @param params optional sequence of indexed parameters
     * @return the number of entities updated.
     * @see [PanacheRepositoryBase.update]
     */
    @GenerateBridge
    fun update(query: String, vararg params: Any): Int = throw implementationInjectionMissing()

    /**
     * Update all entities of this type matching the given query, with named parameters.
     *
     * @param query a query string
     * @param params [Map] of named parameters
     * @return the number of entities updated.
     * @see [PanacheRepositoryBase.update]
     */
    @GenerateBridge
    fun update(query: String, params: Map<String, Any>): Int = throw implementationInjectionMissing()

    /**
     * Update all entities of this type matching the given query, with named parameters.
     *
     * @param query a query string
     * @param params Parameters of named parameters
     * @return the number of entities updated.
     * @see [PanacheRepositoryBase.update]
     */
    @GenerateBridge
    fun update(query: String, params: Parameters): Int = throw implementationInjectionMissing()
}
