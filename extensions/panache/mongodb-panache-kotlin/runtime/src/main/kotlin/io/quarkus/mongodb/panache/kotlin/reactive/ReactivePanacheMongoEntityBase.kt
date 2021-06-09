package io.quarkus.mongodb.panache.kotlin.reactive

import io.quarkus.mongodb.panache.kotlin.reactive.runtime.KotlinReactiveMongoOperations.Companion.INSTANCE
import io.smallrye.mutiny.Uni

/**
 * Represents an entity. If your Mongo entities extend this class they gain auto-generated accessors
 * to all their public fields, as well as a lot of useful
 * methods. Unless you have a custom ID strategy, you should not extend this class directly but extend
 * [ReactivePanacheMongoEntity] instead.
 *
 * @see [ReactivePanacheMongoEntity]
 */
abstract class ReactivePanacheMongoEntityBase {
    /**
     * Persist this entity in the database.
     * This will set it's ID field if not already set.
     *
     * @see [persist]
     */
    fun <T : ReactivePanacheMongoEntityBase> persist(): Uni<T> = INSTANCE.persist(this).map { this as T }

    /**
     * Update this entity in the database.
     *
     * @see [update]
     */
    fun <T : ReactivePanacheMongoEntityBase> update(): Uni<T> = INSTANCE.update(this).map { this as T }

    /**
     * Persist this entity in the database or update it if it already exist.
     *
     * @see [persistOrUpdate]
     */
    fun <T : ReactivePanacheMongoEntityBase> persistOrUpdate(): Uni<T> = INSTANCE.persistOrUpdate(this).map { this as T }

    /**
     * Delete this entity from the database, if it is already persisted.
     *
     * @see [delete]
     * @see [ReactivePanacheMongoCompanionBase.deleteAll]
     */
    fun delete(): Uni<Void> = INSTANCE.delete(this)
}