package io.quarkus.mongodb.panache.kotlin

import io.quarkus.mongodb.panache.kotlin.runtime.KotlinMongoOperations.Companion.INSTANCE

/**
 * Represents an entity. If your Mongo entities extend this class they gain auto-generated accessors
 * to all their public fields, as well as a lot of useful methods. Unless you have a custom ID
 * strategy, you should not extend this class directly but extend [PanacheMongoEntity] instead.
 *
 * @see [PanacheMongoEntity]
 */
@Suppress("unused")
abstract class PanacheMongoEntityBase {

    /** Persist this entity in the database. This will set its ID field if not already set. */
    open fun persist() {
        INSTANCE.persist(this)
    }

    /** Update this entity in the database. */
    open fun update() {
        INSTANCE.update(this)
    }

    /** Persist this entity in the database or update it if it already exists. */
    open fun persistOrUpdate() {
        INSTANCE.persistOrUpdate(this)
    }

    /**
     * Delete this entity from the database if it is already persisted.
     *
     * @see [PanacheMongoCompanionBase.deleteAll]
     */
    open fun delete() {
        INSTANCE.delete(this)
    }
}
