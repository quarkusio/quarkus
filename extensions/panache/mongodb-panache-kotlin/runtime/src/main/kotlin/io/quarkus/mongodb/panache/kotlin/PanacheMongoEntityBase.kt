package io.quarkus.mongodb.panache.kotlin

import io.quarkus.mongodb.panache.kotlin.runtime.KotlinMongoOperations

/**
 * Represents an entity. If your Mongo entities extend this class they gain auto-generated accessors
 * to all their public fields, as well as a lot of useful methods. Unless you have a custom ID strategy, you
 * should not extend this class directly but extend [PanacheMongoEntity] instead.
 *
 * @see PanacheMongoEntity
 */
@Suppress("unused")
abstract class PanacheMongoEntityBase {
    /**
     * Defines internal implementation details for use by quarkus.  Use of these members is highly discouraged as the
     * implementation may change without warning.
     */
    companion object {
        /**
         * Provides the default implementations for quarkus to wire up.  Should not be used by third party developers.
         */
        @JvmStatic
        val operations = KotlinMongoOperations()
    }

    /**
     * Persist this entity in the database.  This will set its ID field if not already set.
     *
     * @see [persist]
     */
    fun persist() {
        operations.persist(this)
    }

    /**
     * Update this entity in the database.
     *
     * @see [update]
     */
    fun update() {
        operations.update(this)
    }

    /**
     * Persist this entity in the database or update it if it already exists.
     *
     * @see [persistOrUpdate]
     */
    fun persistOrUpdate() {
        operations.persistOrUpdate(this)
    }

    /**
     * Delete this entity from the database if it is already persisted.
     *
     * @see [delete]
     * @see [PanacheMongoCompanionBase.deleteAll]
     */
    fun delete() {
        operations.delete(this)
    }
}