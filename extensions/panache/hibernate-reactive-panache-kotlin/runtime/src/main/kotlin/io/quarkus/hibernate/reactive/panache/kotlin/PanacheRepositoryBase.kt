package io.quarkus.hibernate.reactive.panache.kotlin

import io.quarkus.hibernate.reactive.panache.common.runtime.AbstractJpaOperations
import io.quarkus.hibernate.reactive.panache.kotlin.runtime.KotlinJpaOperations.Companion.INSTANCE
import io.quarkus.panache.common.Parameters
import io.quarkus.panache.common.Sort
import io.quarkus.panache.common.impl.GenerateBridge
import io.smallrye.common.annotation.CheckReturnValue
import io.smallrye.mutiny.Multi
import io.smallrye.mutiny.Uni
import jakarta.persistence.LockModeType
import org.hibernate.reactive.mutiny.Mutiny
import java.util.stream.Stream

/**
 * Represents a Repository for a specific type of entity `Entity`, with an ID type
 * of `Id`. Implementing this repository will gain you the exact same useful methods
 * that are on {@link PanacheEntityBase}. Unless you have a custom ID strategy, you should not
 * implement this interface directly but implement {@link PanacheRepository} instead.
 *
 * @param Entity The type of entity to operate on
 * @param Id The ID type of the entity
 */
interface PanacheRepositoryBase<Entity : Any, Id : Any> {

    /**
     * Returns the current [Mutiny.Session]
     *
     * @return the current [Mutiny.Session]
     */
    fun getSession(): Uni<Mutiny.Session> = AbstractJpaOperations.getSession()

    @CheckReturnValue
    fun persist(entity: Entity): Uni<Entity> = INSTANCE.persist(entity).map { entity }

    /**
     * Persist the given entity in the database, if not already persisted.
     * Then flushes all pending changes to the database.
     *
     * @param entity the entity to persist.
     * @return the entity
     * @see [isPersistent] isPersistent
     * @see [persist] persist
     */
    @CheckReturnValue
    fun persistAndFlush(entity: Entity): Uni<Entity> = INSTANCE.persist(entity)
        .flatMap { INSTANCE.flush() }
        .map { entity }

    /**
     * Delete the given entity from the database, if it is already persisted.
     *
     * @param entity the entity to delete.
     * @return nothing
     * @see [isPersistent] isPersistent
     * @see [deleteAll] deleteAll
     */
    @CheckReturnValue
    fun delete(entity: Entity): Uni<Void> = INSTANCE.delete(entity)

    /**
     * Returns true if the given entity is persistent in the database. If yes, all modifications to
     * its persistent fields will be automatically committed to the database at transaction
     * commit time.
     *
     * @param entity the entity to check
     * @return true if the entity is persistent in the database.
     */
    fun isPersistent(entity: Entity) = INSTANCE.isPersistent(entity)

    /**
     * Flushes all pending changes to the database.
     *
     * @return nothing
     */
    @CheckReturnValue
    fun flush(): Uni<Void> = INSTANCE.flush()

    // Queries
    /**
     * Find an entity of this type by ID.
     *
     * @param id the ID of the entity to find.
     * @return the entity found, or `null` if not found.
     */
    @CheckReturnValue
    @GenerateBridge
    fun findById(id: Id): Uni<Entity> = throw INSTANCE.implementationInjectionMissing()

    /**
     * Find an entity of this type by ID and lock it.
     *
     * @param id the ID of the entity to find.
     * @param lockModeType the locking strategy to be used when retrieving the entity.
     * @return the entity found, or `null` if not found.
     */
    @CheckReturnValue
    @GenerateBridge
    fun findById(id: Id, lockModeType: LockModeType): Uni<Entity> = throw INSTANCE.implementationInjectionMissing()

    /**
     * Find entities using a query, with optional indexed parameters.
     *
     * @param query a query string
     * @param params optional sequence of indexed parameters
     * @return a new [PanacheQuery] instance for the given query
     * @see [list] list
     * @see [stream] stream
     */
    @GenerateBridge
    fun find(query: String, vararg params: Any): PanacheQuery<Entity> = throw INSTANCE.implementationInjectionMissing()

    /**
     * Find entities using a query and the given sort options, with optional indexed parameters.
     *
     * @param query a query string
     * @param sort the sort strategy to use
     * @param params optional sequence of indexed parameters
     * @return a new [PanacheQuery] instance for the given query
     * @see [list] list
     * @see [stream] stream
     */
    @GenerateBridge
    fun find(query: String, sort: Sort, vararg params: Any): PanacheQuery<Entity> =
        throw INSTANCE.implementationInjectionMissing()

    /**
     * Find entities using a query, with named parameters.
     *
     * @param query a query string
     * @param params [Map] of named parameters
     * @return a new [PanacheQuery] instance for the given query
     * @see [list] list
     * @see [stream] stream
     */
    @GenerateBridge
    fun find(query: String, params: Map<String, Any>): PanacheQuery<Entity> =
        throw INSTANCE.implementationInjectionMissing()

    /**
     * Find entities using a query and the given sort options, with named parameters.
     *
     * @param query a query string
     * @param sort the sort strategy to use
     * @param params [Map] of indexed parameters
     * @return a new [PanacheQuery] instance for the given query
     * @see [list] list
     * @see [stream] stream
     */
    @GenerateBridge
    fun find(query: String, sort: Sort, params: Map<String, Any>): PanacheQuery<Entity> =
        throw INSTANCE.implementationInjectionMissing()

    /**
     * Find entities using a query, with named parameters.
     *
     * @param query a query string
     * @param params [Parameters] of named parameters
     * @return a new [PanacheQuery] instance for the given query
     * @see [list] list
     * @see [stream] stream
     */
    @GenerateBridge
    fun find(query: String, params: Parameters): PanacheQuery<Entity> = throw INSTANCE.implementationInjectionMissing()

    /**
     * Find entities using a query and the given sort options, with named parameters.
     *
     * @param query a query string
     * @param sort the sort strategy to use
     * @param params [Parameters] of indexed parameters
     * @return a new [PanacheQuery] instance for the given query
     * @see [list] list
     * @see [stream] stream
     */
    @GenerateBridge
    fun find(query: String, sort: Sort, params: Parameters): PanacheQuery<Entity> =
        throw INSTANCE.implementationInjectionMissing()

    /**
     * Find all entities of this type.
     *
     * @return a new [PanacheQuery] instance to find all entities of this type.
     * @see [listAll] listAll
     * @see [streamAll] streamAll
     */
    @GenerateBridge
    fun findAll(): PanacheQuery<Entity> = throw INSTANCE.implementationInjectionMissing()

    /**
     * Find all entities of this type, in the given order.
     *
     * @param sort the sort order to use
     * @return a new [PanacheQuery] instance to find all entities of this type.
     * @see [listAll] listAll
     * @see [streamAll] streamAll
     */
    @GenerateBridge
    fun findAll(sort: Sort): PanacheQuery<Entity> = throw INSTANCE.implementationInjectionMissing()

    /**
     * Find entities matching a query, with optional indexed parameters.
     * This method is a shortcut for `find(query, params).list()`.
     *
     * @param query a query string
     * @param params optional sequence of indexed parameters
     * @return a [List] containing all results, without paging
     * @see [find] find
     * @see [stream] stream
     */
    @CheckReturnValue
    @GenerateBridge
    fun list(query: String, vararg params: Any): Uni<List<Entity>> = throw INSTANCE.implementationInjectionMissing()

    /**
     * Find entities matching a query and the given sort options, with optional indexed parameters.
     * This method is a shortcut for `find(query, sort, params).list()`.
     *
     * @param query a query string
     * @param sort the sort strategy to use
     * @param params optional sequence of indexed parameters
     * @return a [List] containing all results, without paging
     * @see [find] find
     * @see [stream] stream
     */
    @CheckReturnValue
    @GenerateBridge
    fun list(query: String, sort: Sort, vararg params: Any): Uni<List<Entity>> =
        throw INSTANCE.implementationInjectionMissing()

    /**
     * Find entities matching a query, with named parameters.
     * This method is a shortcut for `find(query, params).list()`.
     *
     * @param query a query string
     * @param params [Map] of named parameters
     * @return a [List] containing all results, without paging
     * @see [find] find
     * @see [stream] stream
     */
    @CheckReturnValue
    @GenerateBridge
    fun list(query: String, params: Map<String, Any>): Uni<List<Entity>> =
        throw INSTANCE.implementationInjectionMissing()

    /**
     * Find entities matching a query and the given sort options, with named parameters.
     * This method is a shortcut for `find(query, sort, params).list()`.
     *
     * @param query a query string
     * @param sort the sort strategy to use
     * @param params [Map] of indexed parameters
     * @return a [List] containing all results, without paging
     * @see [find] find
     * @see [stream] stream
     */
    @CheckReturnValue
    @GenerateBridge
    fun list(query: String, sort: Sort, params: Map<String, Any>): Uni<List<Entity>> =
        throw INSTANCE.implementationInjectionMissing()

    /**
     * Find entities matching a query, with named parameters.
     * This method is a shortcut for `find(query, params).list()`.
     *
     * @param query a query string
     * @param params [Parameters] of named parameters
     * @return a [List] containing all results, without paging
     * @see [find] find
     * @see [stream] stream
     */
    @CheckReturnValue
    @GenerateBridge
    fun list(query: String, params: Parameters): Uni<List<Entity>> = throw INSTANCE.implementationInjectionMissing()

    /**
     * Find entities matching a query and the given sort options, with named parameters.
     * This method is a shortcut for `find(query, sort, params).list()`.
     *
     * @param query a query string
     * @param sort the sort strategy to use
     * @param params [Parameters] of indexed parameters
     * @return a [List] containing all results, without paging
     * @see [find] find
     * @see [stream] stream
     */
    @CheckReturnValue
    @GenerateBridge
    fun list(query: String, sort: Sort, params: Parameters): Uni<List<Entity>> =
        throw INSTANCE.implementationInjectionMissing()

    /**
     * Find all entities of this type.
     * This method is a shortcut for `findAll().list()`.
     *
     * @return a [List] containing all results, without paging
     * @see [findAll] findAll
     * @see [streamAll] streamAll
     */
    @CheckReturnValue
    @GenerateBridge
    fun listAll(): Uni<List<Entity>> {
        throw INSTANCE.implementationInjectionMissing()
    }

    /**
     * Find all entities of this type, in the given order.
     * This method is a shortcut for `findAll(sort).list()`.
     *
     * @param sort the sort order to use
     * @return a [List] containing all results, without paging
     * @see [findAll] findAll
     * @see [streamAll] streamAll
     */
    @CheckReturnValue
    @GenerateBridge
    fun listAll(sort: Sort): Uni<List<Entity>> = throw INSTANCE.implementationInjectionMissing()

    /**
     * Find entities matching a query, with optional indexed parameters.
     * This method is a shortcut for `find(query, params).stream()`.
     * It requires a transaction to work.
     * Without a transaction, the underlying cursor can be closed before the end of the stream.
     *
     * @param query a query string
     * @param params optional sequence of indexed parameters
     * @return a [Stream] containing all results, without paging
     * @see [find] find
     * @see [list] list
     */
    @CheckReturnValue
    @GenerateBridge
    fun stream(query: String, vararg params: Any): Multi<Entity> = throw INSTANCE.implementationInjectionMissing()

    /**
     * Find entities matching a query and the given sort options, with optional indexed parameters.
     * This method is a shortcut for `find(query, sort, params).stream()`.
     * It requires a transaction to work.
     * Without a transaction, the underlying cursor can be closed before the end of the stream.
     *
     * @param query a query string
     * @param sort the sort strategy to use
     * @param params optional sequence of indexed parameters
     * @return a [Stream] containing all results, without paging
     * @see [find] find
     * @see [list] list
     */
    @CheckReturnValue
    @GenerateBridge
    fun stream(query: String, sort: Sort, vararg params: Any): Multi<Entity> =
        throw INSTANCE.implementationInjectionMissing()

    /**
     * Find entities matching a query, with named parameters.
     * This method is a shortcut for `find(query, params).stream()`.
     * It requires a transaction to work.
     * Without a transaction, the underlying cursor can be closed before the end of the stream.
     *
     * @param query a query string
     * @param params [Map] of named parameters
     * @return a [Stream] containing all results, without paging
     * @see [stream] stream
     * @see [find] find
     * @see [list] list
     */
    @CheckReturnValue
    @GenerateBridge
    fun stream(query: String, params: Map<String, Any>): Multi<Entity> =
        throw INSTANCE.implementationInjectionMissing()

    /**
     * Find entities matching a query and the given sort options, with named parameters.
     * This method is a shortcut for `find(query, sort, params).stream()`.
     * It requires a transaction to work.
     * Without a transaction, the underlying cursor can be closed before the end of the stream.
     *
     * @param query a query string
     * @param sort the sort strategy to use
     * @param params [Map] of indexed parameters
     * @return a [Stream] containing all results, without paging
     * @see [find] find
     * @see [list] list
     */
    @CheckReturnValue
    @GenerateBridge
    fun stream(query: String, sort: Sort, params: Map<String, Any>): Multi<Entity> =
        throw INSTANCE.implementationInjectionMissing()

    /**
     * Find entities matching a query, with named parameters.
     * This method is a shortcut for `find(query, params).stream()`.
     * It requires a transaction to work.
     * Without a transaction, the underlying cursor can be closed before the end of the stream.
     *
     * @param query a query string
     * @param params [Parameters] of named parameters
     * @return a [Stream] containing all results, without paging
     * @see [find] find
     * @see [list] list
     */
    @CheckReturnValue
    @GenerateBridge
    fun stream(query: String, params: Parameters): Multi<Entity> = throw INSTANCE.implementationInjectionMissing()

    /**
     * Find entities matching a query and the given sort options, with named parameters.
     * This method is a shortcut for `find(query, sort, params).stream()`.
     * It requires a transaction to work.
     * Without a transaction, the underlying cursor can be closed before the end of the stream.
     *
     * @param query a query string
     * @param sort the sort strategy to use
     * @param params [Parameters] of indexed parameters
     * @return a [Stream] containing all results, without paging
     * @see [find] find
     * @see [list] list
     */
    @CheckReturnValue
    @GenerateBridge
    fun stream(query: String, sort: Sort, params: Parameters): Multi<Entity> =
        throw INSTANCE.implementationInjectionMissing()

    /**
     * Find all entities of this type.
     * This method is a shortcut for `findAll().stream()`.
     * It requires a transaction to work.
     * Without a transaction, the underlying cursor can be closed before the end of the stream.
     *
     * @return a [Stream] containing all results, without paging
     * @see [findAll] findAll
     * @see [listAll] listAll
     */
    @CheckReturnValue
    @GenerateBridge
    fun streamAll(sort: Sort): Multi<Entity> = throw INSTANCE.implementationInjectionMissing()

    /**
     * Find all entities of this type, in the given order.
     * This method is a shortcut for `findAll().stream()`.
     * It requires a transaction to work.
     * Without a transaction, the underlying cursor can be closed before the end of the stream.
     *
     * @return a [Stream] containing all results, without paging
     * @see [findAll] findAll
     * @see [listAll] listAll
     */
    @CheckReturnValue
    @GenerateBridge
    fun streamAll(): Multi<Entity> = throw INSTANCE.implementationInjectionMissing()

    /**
     * Counts the number of this type of entity in the database.
     *
     * @return the number of this type of entity in the database.
     */
    @CheckReturnValue
    @GenerateBridge
    fun count(): Uni<Long> = throw INSTANCE.implementationInjectionMissing()

    /**
     * Counts the number of this type of entity matching the given query, with optional indexed parameters.
     *
     * @param query a query string
     * @param params optional sequence of indexed parameters
     * @return the number of entities counted.
     */
    @CheckReturnValue
    @GenerateBridge
    fun count(query: String, vararg params: Any): Uni<Long> = throw INSTANCE.implementationInjectionMissing()

    /**
     * Counts the number of this type of entity matching the given query, with named parameters.
     *
     * @param query a query string
     * @param params [Map] of named parameters
     * @return the number of entities counted.
     */
    @CheckReturnValue
    @GenerateBridge
    fun count(query: String, params: Map<String, Any>): Uni<Long> = throw INSTANCE.implementationInjectionMissing()

    /**
     * Counts the number of this type of entity matching the given query, with named parameters.
     *
     * @param query a query string
     * @param params [Parameters] of named parameters
     * @return the number of entities counted.
     */
    @CheckReturnValue
    @GenerateBridge
    fun count(query: String, params: Parameters): Uni<Long> = throw INSTANCE.implementationInjectionMissing()

    /**
     * Delete all entities of this type from the database.
     *
     * WARNING: the default implementation of this method uses a bulk delete query and ignores
     * cascading rules from the JPA model.
     *
     * @return the number of entities deleted.
     * @see [delete] delete
     */
    @CheckReturnValue
    @GenerateBridge
    fun deleteAll(): Uni<Long> = throw INSTANCE.implementationInjectionMissing()

    /**
     * Delete an entity of this type by ID.
     *
     * @param id the ID of the entity to delete.
     * @return false if the entity was not deleted (not found).
     */
    @CheckReturnValue
    @GenerateBridge
    fun deleteById(id: Id): Uni<Boolean> = throw INSTANCE.implementationInjectionMissing()

    /**
     * Delete all entities of this type matching the given query, with optional indexed parameters.
     *
     * WARNING: the default implementation of this method uses a bulk delete query and ignores
     * cascading rules from the JPA model.
     *
     * @param query a query string
     * @param params optional sequence of indexed parameters
     * @return the number of entities deleted.
     * @see [deleteAll] deleteAll
     */
    @CheckReturnValue
    @GenerateBridge
    fun delete(query: String, vararg params: Any): Uni<Long> = throw INSTANCE.implementationInjectionMissing()

    /**
     * Delete all entities of this type matching the given query, with named parameters.
     *
     * WARNING: the default implementation of this method uses a bulk delete query and ignores
     * cascading rules from the JPA model.
     *
     * @param query a query string
     * @param params [Map] of named parameters
     * @return the number of entities deleted.
     * @see [deleteAll] deleteAll
     */
    @CheckReturnValue
    @GenerateBridge
    fun delete(query: String, params: Map<String, Any>): Uni<Long> = throw INSTANCE.implementationInjectionMissing()

    /**
     * Delete all entities of this type matching the given query, with named parameters.
     *
     * WARNING: the default implementation of this method uses a bulk delete query and ignores
     * cascading rules from the JPA model.
     *
     * @param query a query string
     * @param params [Parameters] of named parameters
     * @return the number of entities deleted.
     * @see [deleteAll] deleteAll
     */
    @CheckReturnValue
    @GenerateBridge
    fun delete(query: String, params: Parameters): Uni<Long> = throw INSTANCE.implementationInjectionMissing()

    /**
     * Persist all given entities.
     *
     * @param entities the entities to persist
     * @return nothing
     */
    @CheckReturnValue
    fun persist(entities: Iterable<Entity>): Uni<Void> = INSTANCE.persist(entities)

    /**
     * Persist all given entities.
     *
     * @param entities the entities to persist
     * @return nothing
     */
    @CheckReturnValue
    fun persist(entities: Stream<Entity>): Uni<Void> = INSTANCE.persist(entities)

    /**
     * Persist all given entities.
     *
     * @param entities the entities to persist
     * @return nothing
     */
    @CheckReturnValue
    fun persist(firstEntity: Entity, vararg entities: Entity): Uni<Void> =
        INSTANCE.persist(listOf(firstEntity) + listOf(*entities))

    /**
     * Update all entities of this type matching the given query, with optional indexed parameters.
     *
     * @param query a query string
     * @param params optional sequence of indexed parameters
     * @return the number of entities updated.
     */
    @CheckReturnValue
    @GenerateBridge
    fun update(query: String, vararg params: Any): Uni<Int> = throw INSTANCE.implementationInjectionMissing()

    /**
     * Update all entities of this type matching the given query, with named parameters.
     *
     * @param query a query string
     * @param params [Map] of named parameters
     * @return the number of entities updated.
     */
    @CheckReturnValue
    @GenerateBridge
    fun update(query: String, params: Map<String, Any>): Uni<Int> = throw INSTANCE.implementationInjectionMissing()

    /**
     * Update all entities of this type matching the given query, with named parameters.
     *
     * @param query a query string
     * @param params [Parameters] of named parameters
     * @return the number of entities updated.
     */
    @CheckReturnValue
    @GenerateBridge
    fun update(query: String, params: Parameters): Uni<Int> = throw INSTANCE.implementationInjectionMissing()
}
