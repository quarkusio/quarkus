@file:Suppress("unused")

package io.quarkus.mongodb.panache.kotlin

import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import io.quarkus.mongodb.panache.PanacheUpdate
import io.quarkus.mongodb.panache.kotlin.PanacheMongoEntityBase.Companion.operations
import io.quarkus.panache.common.Parameters
import io.quarkus.panache.common.Sort
import io.quarkus.panache.common.impl.GenerateBridge
import org.bson.Document
import java.util.stream.Stream

/**
 * Represents a Repository for a specific type of entity `Entity`, with an ID type
 * of `Id`. Implementing this repository will gain you the exact same useful methods
 * that are on [PanacheMongoEntityBase]. Unless you have a custom ID strategy, you should not
 * implement this interface directly but implement [PanacheMongoRepository] instead.
 *
 * @param Entity The type of entity to operate on
 * @param Id The ID type of the entity
 * @see PanacheMongoRepository<Id><Entity>
 */
interface PanacheMongoRepositoryBase<Entity: Any, Id: Any> {
    /**
     * Persist the given entity in the database.
     * This will set it's ID field if not already set.
     *
     * @param entity the entity to insert.
     * @see [persist]
     */
    fun persist(entity: Entity) = operations.persist(entity)

    /**
     * Update the given entity in the database.
     *
     * @param entity the entity to update.
     * @see [update]
     */
    fun update(entity: Entity) = operations.update(entity)

    /**
     * Persist the given entity in the database or update it if it already exist.
     *
     * @param entity the entity to update.
     * @see [persistOrUpdate]
     */
    fun persistOrUpdate(entity: Entity) = operations.persistOrUpdate(entity)

    /**
     * Delete the given entity from the database, if it is already persisted.
     *
     * @param entity the entity to delete.
     * @see [delete]
     * @see [deleteAll]
     */
    fun delete(entity: Entity) = operations.delete(entity)

    /**
     * Find an entity of this type by ID.
     *
     * @param id the ID of the entity to find.
     * @return the entity found, or `null` if not found.
     */
    @GenerateBridge(targetReturnTypeErased = true)
    fun findById(id: Id): Entity? = throw operations.implementationInjectionMissing()

    /**
     * Find entities using a query, with optional indexed parameters.
     *
     * @param query a query string
     * @param params optional sequence of indexed parameters
     * @return a new [PanacheQuery] instance for the given query
     * @see [find]
     * @see [list]
     * @see [stream]
     */
    @GenerateBridge
    fun find(query: String, vararg params: Any?): PanacheQuery<Entity> =
            throw operations.implementationInjectionMissing()

    /**
     * Find entities using a query and the given sort options, with optional indexed parameters.
     *
     * @param query a query string
     * @param sort the sort strategy to use
     * @param params optional sequence of indexed parameters
     * @return a new [PanacheQuery] instance for the given query
     * @see [find]
     * @see [list]
     * @see [stream]
     */
    @GenerateBridge
    fun find(query: String, sort: Sort, vararg params: Any?): PanacheQuery<Entity> =
            throw operations.implementationInjectionMissing()

    /**
     * Find entities using a query, with named parameters.
     *
     * @param query a query string
     * @param params [Map] of named parameters
     * @return a new [PanacheQuery] instance for the given query
     * @see [find]
     * @see [list]
     * @see [stream]
     */
    @GenerateBridge
    fun find(query: String, params: Map<String, Any?>): PanacheQuery<Entity> =
            throw operations.implementationInjectionMissing()

    /**
     * Find entities using a query and the given sort options, with named parameters.
     *
     * @param query a query string
     * @param sort the sort strategy to use
     * @param params [Map] of indexed parameters
     * @return a new [PanacheQuery] instance for the given query
     * @see [find]
     * @see [list]
     * @see [stream]
     */
    @GenerateBridge
    fun find(query: String, sort: Sort, params: Map<String, Any?>): PanacheQuery<Entity> =
            throw operations.implementationInjectionMissing()

    /**
     * Find entities using a query, with named parameters.
     *
     * @param query a query string
     * @param params [Parameters] of named parameters
     * @return a new [PanacheQuery] instance for the given query
     * @see [find]
     * @see [list]
     * @see [stream]
     */
    @GenerateBridge
    fun find(query: String, params: Parameters): PanacheQuery<Entity> =
            throw operations.implementationInjectionMissing()

    /**
     * Find entities using a query and the given sort options, with named parameters.
     *
     * @param query a query string
     * @param sort the sort strategy to use
     * @param params [Parameters] of indexed parameters
     * @return a new [PanacheQuery] instance for the given query
     * @see [find]
     * @see [list]
     * @see [stream]
     */
    @GenerateBridge
    fun find(query: String, sort: Sort, params: Parameters): PanacheQuery<Entity> =
            throw operations.implementationInjectionMissing()

    /**
     * Find entities using a BSON query.
     *
     * @param query a [Document] query
     * @return a new [PanacheQuery] instance for the given query
     * @see [find]
     * @see [list]
     * @see [stream]
     */
    @GenerateBridge
    fun find(query: Document): PanacheQuery<Entity> = throw operations.implementationInjectionMissing()

    /**
     * Find entities using a a BSON query and a BSON sort.
     *
     * @param query a [Document] query
     * @param sort the [Document] sort
     * @return a new [PanacheQuery] instance for the given query
     * @see [find]
     * @see [list]
     * @see [stream]
     */
    @GenerateBridge
    fun find(query: Document, sort: Document): PanacheQuery<Entity> = throw operations.implementationInjectionMissing()

    /**
     * Find all entities of this type.
     *
     * @return a new [PanacheQuery] instance to find all entities of this type.
     * @see [findAll]
     * @see [listAll]
     * @see [streamAll]
     */
    @GenerateBridge
    fun findAll(): PanacheQuery<Entity> = throw operations.implementationInjectionMissing()

    /**
     * Find all entities of this type, in the given order.
     *
     * @param sort the sort order to use
     * @return a new [PanacheQuery] instance to find all entities of this type.
     * @see [findAll]
     * @see [listAll]
     * @see [streamAll]
     */
    @GenerateBridge
    fun findAll(sort: Sort): PanacheQuery<Entity> = throw operations.implementationInjectionMissing()

    /**
     * Find entities matching a query, with optional indexed parameters.
     * This method is a shortcut for `find(query, params).list()`.
     *
     * @param query a query string
     * @param params optional sequence of indexed parameters
     * @return a [List] containing all results, without paging
     * @see [list]
     * @see [find]
     * @see [stream]
     */
    @GenerateBridge
    fun list(query: String, vararg params: Any?): List<Entity> = throw operations.implementationInjectionMissing()

    /**
     * Find entities matching a query and the given sort options, with optional indexed parameters.
     * This method is a shortcut for `find(query, sort, params).list()`.
     *
     * @param query a query string
     * @param sort the sort strategy to use
     * @param params optional sequence of indexed parameters
     * @return a [List] containing all results, without paging
     * @see [list]
     * @see [find]
     * @see [stream]
     */
    @GenerateBridge
    fun list(query: String, sort: Sort, vararg params: Any?): List<Entity> =
            throw operations.implementationInjectionMissing()

    /**
     * Find entities matching a query, with named parameters.
     * This method is a shortcut for `find(query, params).list()`.
     *
     * @param query a query string
     * @param params [Map] of named parameters
     * @return a [List] containing all results, without paging
     * @see [list]
     * @see [find]
     * @see [stream]
     */
    @GenerateBridge
    fun list(query: String, params: Map<String, Any?>): List<Entity> = throw operations.implementationInjectionMissing()

    /**
     * Find entities matching a query and the given sort options, with named parameters.
     * This method is a shortcut for `find(query, sort, params).list()`.
     *
     * @param query a query string
     * @param sort the sort strategy to use
     * @param params [Map] of indexed parameters
     * @return a [List] containing all results, without paging
     * @see [list]
     * @see [find]
     * @see [stream]
     */
    @GenerateBridge
    fun list(query: String, sort: Sort, params: Map<String, Any?>): List<Entity> =
            throw operations.implementationInjectionMissing()

    /**
     * Find entities matching a query, with named parameters.
     * This method is a shortcut for `find(query, params).list()`.
     *
     * @param query a query string
     * @param params [Parameters] of named parameters
     * @return a [List] containing all results, without paging
     * @see [list]
     * @see [find]
     * @see [stream]
     */
    @GenerateBridge
    fun list(query: String, params: Parameters): List<Entity> = throw operations.implementationInjectionMissing()

    /**
     * Find entities matching a query and the given sort options, with named parameters.
     * This method is a shortcut for `find(query, sort, params).list()`.
     *
     * @param query a query string
     * @param sort the sort strategy to use
     * @param params [Parameters] of indexed parameters
     * @return a [List] containing all results, without paging
     * @see [list]
     * @see [find]
     * @see [stream]
     */
    @GenerateBridge
    fun list(query: String, sort: Sort, params: Parameters): List<Entity> =
            throw operations.implementationInjectionMissing()

    /**
     * Find entities using a BSON query.
     * This method is a shortcut for `find(query).list()`.
     *
     * @param query a [Document] query
     * @return a [List] containing all results, without paging
     * @see [find]
     * @see [list]
     * @see [stream]
     */
    @GenerateBridge
    fun list(query: Document): List<Entity> = throw operations.implementationInjectionMissing()

    /**
     * Find entities using a a BSON query and a BSON sort.
     * This method is a shortcut for `find(query, sort).list()`.
     *
     * @param query a [Document] query
     * @param sort the [Document] sort
     * @return a [List] containing all results, without paging
     * @see [find]
     * @see [list]
     * @see [stream]
     */
    @GenerateBridge
    fun list(query: Document, sort: Document): List<Entity> = throw operations.implementationInjectionMissing()

    /**
     * Find all entities of this type.
     * This method is a shortcut for `findAll().list()`.
     *
     * @return a [List] containing all results, without paging
     * @see [listAll]
     * @see [findAll]
     * @see [streamAll]
     */
    @GenerateBridge
    fun listAll(): List<Entity> = throw operations.implementationInjectionMissing()

    /**
     * Find all entities of this type, in the given order.
     * This method is a shortcut for `findAll(sort).list()`.
     *
     * @param sort the sort order to use
     * @return a [List] containing all results, without paging
     * @see [listAll]
     * @see [findAll]
     * @see [streamAll]
     */
    @GenerateBridge
    fun listAll(sort: Sort): List<Entity> = throw operations.implementationInjectionMissing()

    /**
     * Find entities matching a query, with optional indexed parameters.
     * This method is a shortcut for `find(query, params).stream()`.
     *
     * @param query a query string
     * @param params optional sequence of indexed parameters
     * @return a [Stream] containing all results, without paging
     * @see [stream]
     * @see [find]
     * @see [list]
     */
    @GenerateBridge
    fun stream(query: String, vararg params: Any?): Stream<Entity> = throw operations.implementationInjectionMissing()

    /**
     * Find entities matching a query and the given sort options, with optional indexed parameters.
     * This method is a shortcut for `find(query, sort, params).stream()`.
     *
     * @param query a query string
     * @param sort the sort strategy to use
     * @param params optional sequence of indexed parameters
     * @return a [Stream] containing all results, without paging
     * @see [stream]
     * @see [find]
     * @see [list]
     */
    @GenerateBridge
    fun stream(query: String, sort: Sort, vararg params: Any?): Stream<Entity> =
            throw operations.implementationInjectionMissing()

    /**
     * Find entities matching a query, with named parameters.
     * This method is a shortcut for `find(query, params).stream()`.
     *
     * @param query a query string
     * @param params [Map] of named parameters
     * @return a [Stream] containing all results, without paging
     * @see [stream]
     * @see [find]
     * @see [list]
     */
    @GenerateBridge
    fun stream(query: String, params: Map<String, Any?>): Stream<Entity> =
            throw operations.implementationInjectionMissing()

    /**
     * Find entities matching a query and the given sort options, with named parameters.
     * This method is a shortcut for `find(query, sort, params).stream()`.
     *
     * @param query a query string
     * @param sort the sort strategy to use
     * @param params [Map] of indexed parameters
     * @return a [Stream] containing all results, without paging
     * @see [stream]
     * @see [find]
     * @see [list]
     */
    @GenerateBridge
    fun stream(query: String, sort: Sort, params: Map<String, Any?>): Stream<Entity> =
            throw operations.implementationInjectionMissing()

    /**
     * Find entities matching a query, with named parameters.
     * This method is a shortcut for `find(query, params).stream()`.
     *
     * @param query a query string
     * @param params [Parameters] of named parameters
     * @return a [Stream] containing all results, without paging
     * @see [stream]
     * @see [find]
     * @see [list]
     */
    @GenerateBridge
    fun stream(query: String, params: Parameters): Stream<Entity> = throw operations.implementationInjectionMissing()

    /**
     * Find entities matching a query and the given sort options, with named parameters.
     * This method is a shortcut for `find(query, sort, params).stream()`.
     *
     * @param query a query string
     * @param sort the sort strategy to use
     * @param params [Parameters] of indexed parameters
     * @return a [Stream] containing all results, without paging
     * @see [stream]
     * @see [find]
     * @see [list]
     */
    @GenerateBridge
    fun stream(query: String, sort: Sort, params: Parameters): Stream<Entity> =
            throw operations.implementationInjectionMissing()

    /**
     * Find entities using a BSON query.
     * This method is a shortcut for `find(query).stream()`.
     *
     * @param query a [Document] query
     * @return a [Stream] containing all results, without paging
     * @see [find]
     * @see [list]
     * @see [stream]
     */
    @GenerateBridge
    fun stream(query: Document): Stream<Entity> = throw operations.implementationInjectionMissing()

    /**
     * Find entities using a a BSON query and a BSON sort.
     * This method is a shortcut for `find(query, sort).stream()`.
     *
     * @param query a [Document] query
     * @param sort the [Document] sort
     * @return a [Stream] containing all results, without paging
     * @see [find]
     * @see [list]
     * @see [stream]
     */
    @GenerateBridge
    fun stream(query: Document, sort: Document): Stream<Entity> = throw operations.implementationInjectionMissing()

    /**
     * Find all entities of this type.
     * This method is a shortcut for `findAll().stream()`.
     *
     * @return a [Stream] containing all results, without paging
     * @see [streamAll]
     * @see [findAll]
     * @see [listAll]
     */
    @GenerateBridge
    fun streamAll(sort: Sort): Stream<Entity> = throw operations.implementationInjectionMissing()

    /**
     * Find all entities of this type, in the given order.
     * This method is a shortcut for `findAll(sort).stream()`.
     *
     * @return a [Stream] containing all results, without paging
     * @see [streamAll]
     * @see [findAll]
     * @see [listAll]
     */
    @GenerateBridge
    fun streamAll(): Stream<Entity> = throw operations.implementationInjectionMissing()

    /**
     * Counts the number of this type of entity in the database.
     *
     * @return the number of this type of entity in the database.
     * @see [count]
     */
    @GenerateBridge
    fun count(): Long = throw operations.implementationInjectionMissing()

    /**
     * Counts the number of this type of entity matching the given query, with optional indexed parameters.
     *
     * @param query a query string
     * @param params optional sequence of indexed parameters
     * @return the number of entities counted.
     * @see [count]
     */
    @GenerateBridge
    fun count(query: String, vararg params: Any?): Long = throw operations.implementationInjectionMissing()

    /**
     * Counts the number of this type of entity matching the given query, with named parameters.
     *
     * @param query a query string
     * @param params [Map] of named parameters
     * @return the number of entities counted.
     * @see [count]
     */
    @GenerateBridge
    fun count(query: String, params: Map<String, Any?>): Long = throw operations.implementationInjectionMissing()

    /**
     * Counts the number of this type of entity matching the given query, with named parameters.
     *
     * @param query a query string
     * @param params [Parameters] of named parameters
     * @return the number of entities counted.
     * @see [count]
     */
    @GenerateBridge
    fun count(query: String, params: Parameters): Long = throw operations.implementationInjectionMissing()

    /**
     * Counts the number of this type of entity matching the given query
     *
     * @param query a [Document] query
     * @return he number of entities counted.
     * @see [count]
     */
    @GenerateBridge
    fun count(query: Document): Long = throw operations.implementationInjectionMissing()

    /**
     * Delete all entities of this type from the database.
     *
     * @return the number of entities deleted.
     * @see [delete]
     */
    @GenerateBridge
    fun deleteAll(): Long = throw operations.implementationInjectionMissing()

    /**
     * Delete an entity of this type by ID.
     *
     * @param id the ID of the entity to delete.
     * @return false if the entity was not deleted (not found).
     */
    @GenerateBridge
    fun deleteById(id: Id): Boolean = throw operations.implementationInjectionMissing()

    /**
     * Delete all entities of this type matching the given query, with optional indexed parameters.
     *
     * @param query a query string
     * @param params optional sequence of indexed parameters
     * @return the number of entities deleted.
     * @see [deleteAll]
     * @see [delete]
     */
    @GenerateBridge
    fun delete(query: String, vararg params: Any?): Long = throw operations.implementationInjectionMissing()

    /**
     * Delete all entities of this type matching the given query, with named parameters.
     *
     * @param query a query string
     * @param params [Map] of named parameters
     * @return the number of entities deleted.
     * @see [deleteAll]
     * @see [delete]
     */
    @GenerateBridge
    fun delete(query: String, params: Map<String, Any?>): Long = throw operations.implementationInjectionMissing()

    /**
     * Delete all entities of this type matching the given query, with named parameters.
     *
     * @param query a query string
     * @param params [Parameters] of named parameters
     * @return the number of entities deleted.
     * @see [deleteAll]
     * @see [delete]
     */
    @GenerateBridge
    fun delete(query: String, params: Parameters): Long = throw operations.implementationInjectionMissing()

    /**
     * Delete all entities of this type matching the given query
     *
     * @param query a [Document] query
     * @return he number of entities counted.
     * @see [count]
     */
    @GenerateBridge
    fun delete(query: Document): Long = throw operations.implementationInjectionMissing()

    /**
     * Persist all given entities.
     *
     * @param entities the entities to insert
     * @see [persist]
     */
    fun persist(entities: Iterable<Entity>) = operations.persist(entities)

    /**
     * Persist all given entities.
     *
     * @param entities the entities to insert
     * @see [persist]
     */
    fun persist(entities: Stream<Entity>) = operations.persist(entities)

    /**
     * Persist all given entities.
     *
     * @param entities the entities to insert
     * @see [persist]
     */
    fun persist(firstEntity: Entity, vararg entities: Entity) = operations.persist(firstEntity, *entities)

    /**
     * Update all given entities.
     *
     * @param entities the entities to update
     * @see [update]
     */
    fun update(entities: Iterable<Entity>) = operations.update(entities)

    /**
     * Update all given entities.
     *
     * @param entities the entities to update
     * @see [update]
     */
    fun update(entities: Stream<Entity>) = operations.update(entities)

    /**
     * Update all given entities.
     *
     * @param entities the entities to update
     * @see [update]
     */
    fun update(firstEntity: Entity, vararg entities: Entity) = operations.update(firstEntity, *entities)

    /**
     * Persist all given entities or update them if they already exist.
     *
     * @param entities the entities to update
     * @see [persistOrUpdate]
     */
    fun persistOrUpdate(entities: Iterable<Entity>) = operations.persistOrUpdate(entities)

    /**
     * Persist all given entities or update them if they already exist.
     *
     * @param entities the entities to update
     * @see [persistOrUpdate]
     */
    fun persistOrUpdate(entities: Stream<Entity>) = operations.persistOrUpdate(entities)

    /**
     * Persist all given entities or update them if they already exist.
     *
     * @param entities the entities to update
     * @see [update]
     */
    fun persistOrUpdate(firstEntity: Entity, vararg entities: Entity) =
            operations.persistOrUpdate(firstEntity, *entities)

    /**
     * Update all entities of this type by the given update document, with optional indexed parameters.
     * The returned [PanacheUpdate] object will allow to restrict on which documents the update should be applied.
     *
     * @param update the update document, if it didn't contain `$set` we add it.
     * It can also be expressed as a query string.
     * @param params optional sequence of indexed parameters
     * @return a new [PanacheUpdate] instance for the given update document
     * @see [update]
     */
    @GenerateBridge
    fun update(update: String, vararg params: Any?): PanacheUpdate = throw operations.implementationInjectionMissing()

    /**
     * Update all entities of this type by the given update document, with named parameters.
     * The returned [PanacheUpdate] object will allow to restrict on which documents the update should be applied.
     *
     * @param update the update document, if it didn't contain `$set` we add it.
     * It can also be expressed as a query string.
     * @param params [Map] of named parameters
     * @return a new [PanacheUpdate] instance for the given update document
     * @see [update]
     */
    @GenerateBridge
    fun update(update: String, params: Map<String, Any?>): PanacheUpdate =
            throw operations.implementationInjectionMissing()

    /**
     * Update all entities of this type by the given update document, with named parameters.
     * The returned [PanacheUpdate] object will allow to restrict on which document the update should be applied.
     *
     * @param update the update document, if it didn't contain `$set` we add it.
     * It can also be expressed as a query string.
     * @param params [Parameters] of named parameters
     * @return a new [PanacheUpdate] instance for the given update document
     * @see [update]
     */
    @GenerateBridge
    fun update(update: String, params: Parameters): PanacheUpdate = throw operations.implementationInjectionMissing()

    /**
     * Allow to access the underlying Mongo Collection
     */
    @GenerateBridge
    fun mongoCollection(): MongoCollection<Entity> = throw operations.implementationInjectionMissing()

    /**
     * Allow to access the underlying Mongo Database.
     */
    @GenerateBridge
    fun mongoDatabase(): MongoDatabase = throw operations.implementationInjectionMissing()
}