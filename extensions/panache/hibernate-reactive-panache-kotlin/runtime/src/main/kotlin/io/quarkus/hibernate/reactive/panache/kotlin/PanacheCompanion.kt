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
 * Defines methods to be used via the companion objects of entities.
 *
 * @param Entity the entity type
 */
interface PanacheCompanion<Entity : PanacheEntityBase> : PanacheCompanionBase<Entity, Long>

interface PanacheCompanionBase<Entity : PanacheEntityBase, Id : Any> {
    /**
     * Returns the current [Mutiny.Session]
     *
     * @return the current [Mutiny.Session]
     */
    fun getSession() = AbstractJpaOperations.getSession()

    /**
     * Find an entity of this type by ID.
     *
     * @param id the ID of the entity to find.
     * @return the entity found, or `null` if not found.
     */
    @GenerateBridge
    fun findById(id: Id): Uni<Entity?> = injectionMissing()

    /**
     * Find an entity of this type by ID and lock it.
     *
     * @param id the ID of the entity to find.
     * @param lockModeType the locking strategy to be used when retrieving the entity.
     * @return the entity found, or `null` if not found.
     */
    @CheckReturnValue
    @GenerateBridge
    fun findById(id: Id, lockModeType: LockModeType): Uni<Entity> = injectionMissing()

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
    fun find(query: String, vararg params: Any): PanacheQuery<Entity> = injectionMissing()

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
    fun find(query: String, sort: Sort, vararg params: Any): PanacheQuery<Entity> = injectionMissing()

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
    fun find(query: String, params: Map<String, Any>): PanacheQuery<Entity> = injectionMissing()

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
    fun find(query: String, sort: Sort, params: Map<String, Any>): PanacheQuery<Entity> = injectionMissing()

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
    fun find(query: String, params: Parameters): PanacheQuery<Entity> = injectionMissing()

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
    fun find(query: String, sort: Sort, params: Parameters): PanacheQuery<Entity> = injectionMissing()

    /**
     * Find all entities of this type.
     *
     * @return a new [PanacheQuery] instance to find all entities of this type.
     * @see [listAll] listAll
     * @see [streamAll] streamAll
     */
    @GenerateBridge
    fun findAll(): PanacheQuery<Entity> = injectionMissing()

    /**
     * Find all entities of this type, in the given order.
     *
     * @param sort the sort order to use
     * @return a new [PanacheQuery] instance to find all entities of this type.
     * @see [listAll] listAll
     * @see [streamAll] streamAll
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
     * @see [find] find
     * @see [stream] stream
     */
    @CheckReturnValue
    @GenerateBridge
    fun list(query: String, vararg params: Any): Uni<List<Entity>> = injectionMissing()

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
    fun list(query: String, sort: Sort, vararg params: Any): Uni<List<Entity>> = injectionMissing()

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
    fun list(query: String, params: Map<String, Any>): Uni<List<Entity>> = injectionMissing()

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
    fun list(query: String, sort: Sort, params: Map<String, Any>): Uni<List<Entity>> = injectionMissing()

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
    fun list(query: String, params: Parameters): Uni<List<Entity>> = injectionMissing()

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
    fun list(query: String, sort: Sort, params: Parameters): Uni<List<Entity>> = injectionMissing()

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
    fun listAll(): Uni<List<Entity>> = injectionMissing()

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
    fun listAll(sort: Sort): Uni<List<Entity>> = injectionMissing()

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
    fun stream(query: String, vararg params: Any): Multi<Entity> = injectionMissing()

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
    fun stream(query: String, sort: Sort, vararg params: Any): Multi<Entity> = injectionMissing()

    /**
     * Find entities matching a query, with named parameters.
     * This method is a shortcut for `find(query, params).stream()`.
     * It requires a transaction to work.
     * Without a transaction, the underlying cursor can be closed before the end of the stream.
     *
     * @param query a query string
     * @param params [Map] of named parameters
     * @return a [Stream] containing all results, without paging
     * @see [find] find
     * @see [list] list
     */
    @CheckReturnValue
    @GenerateBridge
    fun stream(query: String, params: Map<String, Any>): Multi<Entity> = injectionMissing()

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
    fun stream(query: String, sort: Sort, params: Map<String, Any>): Multi<Entity> = injectionMissing()

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
    fun stream(query: String, params: Parameters): Multi<Entity> = injectionMissing()

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
    fun stream(query: String, sort: Sort, params: Parameters): Multi<Entity> = injectionMissing()

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
    fun streamAll(): Multi<Entity> = injectionMissing()

    /**
     * Find all entities of this type, in the given order.
     * This method is a shortcut for `findAll(sort).stream()`.
     * It requires a transaction to work.
     * Without a transaction, the underlying cursor can be closed before the end of the stream.
     *
     * @param sort the sort order to use
     * @return a [Stream] containing all results, without paging
     * @see [findAll] findAll
     * @see [listAll] listAll
     */
    @CheckReturnValue
    @GenerateBridge
    fun streamAll(sort: Sort): Multi<Entity> = injectionMissing()

    /**
     * Counts the number of this type of entity in the database.
     *
     * @return the number of this type of entity in the database.
     */
    @CheckReturnValue
    @GenerateBridge
    fun count(): Uni<Long> = injectionMissing()

    /**
     * Counts the number of this type of entity matching the given query, with optional indexed parameters.
     *
     * @param query a query string
     * @param params optional sequence of indexed parameters
     * @return the number of entities counted.
     */
    @CheckReturnValue
    @GenerateBridge
    fun count(query: String, vararg params: Any): Uni<Long> = injectionMissing()

    /**
     * Counts the number of this type of entity matching the given query, with named parameters.
     *
     * @param query a query string
     * @param params [Map] of named parameters
     * @return the number of entities counted.
     */
    @CheckReturnValue
    @GenerateBridge
    fun count(query: String, params: Map<String, Any>): Uni<Long> = injectionMissing()

    /**
     * Counts the number of this type of entity matching the given query, with named parameters.
     *
     * @param query a query string
     * @param params [Parameters] of named parameters
     * @return the number of entities counted.
     */
    @CheckReturnValue
    @GenerateBridge
    fun count(query: String, params: Parameters): Uni<Long> = injectionMissing()

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
    fun deleteAll(): Uni<Long> = injectionMissing()

    /**
     * Delete an entity of this type by ID.
     *
     * @param id the ID of the entity to delete.
     * @return false if the entity was not deleted (not found).
     */
    @CheckReturnValue
    @GenerateBridge
    fun deleteById(id: Any): Uni<Boolean> = injectionMissing()

    /**
     * Delete all entities of this type matching the given query, with optional indexed parameters.
     *
     * WARNING: the default implementation of this method uses a bulk delete query and ignores
     * cascading rules from the JPA model.
     *
     * @param query a query string
     * @param params optional sequence of indexed parameters
     * @return the number of entities deleted.
     * @see [deleteAll]
     */
    @CheckReturnValue
    @GenerateBridge
    fun delete(query: String, vararg params: Any): Uni<Long> = injectionMissing()

    /**
     * Delete all entities of this type matching the given query, with named parameters.
     *
     * WARNING: the default implementation of this method uses a bulk delete query and ignores
     * cascading rules from the JPA model.
     *
     * @param query a query string
     * @param params [Map] of named parameters
     * @return the number of entities deleted.
     * @see [deleteAll]
     */
    @CheckReturnValue
    @GenerateBridge
    fun delete(query: String, params: Map<String, Any>): Uni<Long> = injectionMissing()

    /**
     * Delete all entities of this type matching the given query, with named parameters.
     *
     * WARNING: the default implementation of this method uses a bulk delete query and ignores
     * cascading rules from the JPA model.
     *
     * @param query a query string
     * @param params [Parameters] of named parameters
     * @return the number of entities deleted.
     * @see [deleteAll]
     */
    @CheckReturnValue
    @GenerateBridge
    fun delete(query: String, params: Parameters): Uni<Long> = injectionMissing()

    /**
     * Persist all given entities.
     *
     * @param entities the entities to persist
     * @return
     */
    @CheckReturnValue
    fun persist(entities: Iterable<Entity>) = INSTANCE.persist(entities)

    /**
     * Persist all given entities.
     *
     * @param entities the entities to persist
     * @return
     */
    @CheckReturnValue
    fun persist(entities: Stream<Entity>) = INSTANCE.persist(entities)

    /**
     * Persist all given entities.
     *
     * @param entities the entities to persist
     * @return
     */
    @CheckReturnValue
    fun persist(firstEntity: Entity, vararg entities: Entity) =
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
    fun update(query: String, vararg params: Any): Uni<Int> = injectionMissing()

    /**
     * Update all entities of this type matching the given query, with named parameters.
     *
     * @param query a query string
     * @param params [Map] of named parameters
     * @return the number of entities updated.
     */
    @CheckReturnValue
    @GenerateBridge
    fun update(query: String, params: Map<String, Any>): Uni<Int> = injectionMissing()

    /**
     * Update all entities of this type matching the given query, with named parameters.
     *
     * @param query a query string
     * @param params [Parameters] of named parameters
     * @return the number of entities updated.
     */
    @CheckReturnValue
    @GenerateBridge
    fun update(query: String, params: Parameters): Uni<Int> = injectionMissing()

    private fun injectionMissing(): Nothing = throw INSTANCE.implementationInjectionMissing()
}
