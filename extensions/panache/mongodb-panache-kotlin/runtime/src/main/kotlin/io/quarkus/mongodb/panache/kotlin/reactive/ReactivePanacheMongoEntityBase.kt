package io.quarkus.mongodb.panache.kotlin.reactive

import io.quarkus.mongodb.panache.kotlin.reactive.runtime.KotlinReactiveMongoOperations
import io.smallrye.mutiny.Uni

/**
 * Represents an entity. If your Mongo entities extend this class they gain auto-generated accessors
 * to all their public fields, as well as a lot of useful
 * methods. Unless you have a custom ID strategy, you should not extend this class directly but extend
 * [ReactivePanacheMongoEntity] instead.
 *
 * @see ReactivePanacheMongoEntity
 */
abstract class ReactivePanacheMongoEntityBase {
    /**
     * Defines internal implementation details for use by quarkus.  Use of these members is highly discouraged as the
     * implementation may change without warning.
     */
    companion object {
        /**
         * Provides the default implementations for quarkus to wire up.  Should not be used by third party developers.
         */
        @JvmStatic
        val operations = KotlinReactiveMongoOperations()
    }

    /**
     * Persist this entity in the database.
     * This will set it's ID field if not already set.
     *
     * @see .persist
     * @see .persist
     * @see .persist
     */
    fun persist(): Uni<Void> = operations.persist(this)

    /**
     * Update this entity in the database.
     *
     * @see .update
     * @see .update
     * @see .update
     */
    fun update(): Uni<Void> = operations.update(this)

    /**
     * Persist this entity in the database or update it if it already exist.
     *
     * @see .persistOrUpdate
     * @see .persistOrUpdate
     * @see .persistOrUpdate
     */
    fun persistOrUpdate(): Uni<Void> = operations.persistOrUpdate(this)

    /**
     * Delete this entity from the database, if it is already persisted.
     *
     * @see .delete
     * @see .delete
     * @see .delete
     * @see .deleteAll
     */
    fun delete(): Uni<Void> = operations.delete(this)
}