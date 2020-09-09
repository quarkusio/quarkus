package io.quarkus.hibernate.orm.panache.kotlin

import com.fasterxml.jackson.annotation.JsonIgnore
import io.quarkus.hibernate.orm.panache.kotlin.runtime.JpaOperations
import javax.json.bind.annotation.JsonbTransient
import javax.persistence.EntityManager

/**
 * Represents an entity. If your Hibernate entities extend this class they gain auto-generated accessors
 * to all their public fields (unless annotated with [Transient]), as well as a lot of useful
 * methods. Unless you have a custom ID strategy, you should not extend this class directly but extend
 * [PanacheEntity] instead.
 *
 * @see PanacheEntity
 */
interface PanacheEntityBase {

    // Operations

    /**
     * Returns the default [EntityManager] for extra operations (eg. CriteriaQueries)
     *
     * @return the default [EntityManager]
     */
    @JsonbTransient
    @JsonIgnore
    fun getEntityManager(): EntityManager = JpaOperations.getEntityManager(this.javaClass)

    /**
     * Returns true if this entity is persistent in the database. If yes, all modifications to
     * its persistent fields will be automatically committed to the database at transaction
     * commit time.
     *
     * @return true if this entity is persistent in the database.
     */
    @JsonbTransient
    @JsonIgnore
    fun isPersistent(): Boolean = JpaOperations.isPersistent(this)

    /**
     * Persist this entity in the database, if not already persisted. This will set your ID field if it is not already set.
     *
     * @see PanacheEntityBase.isPersistent
     * @see PanacheEntityBase.flush
     * @see PanacheEntityBase.persistAndFlush
     */
    fun persist() {
        JpaOperations.persist(this)
    }

    /**
     * Persist this entity in the database, if not already persisted. This will set your ID field if it is not already set.
     * Then flushes all pending changes to the database.
     *
     * @see [PanacheEntityBase.isPersistent]
     * @see [PanacheEntityBase.flush]
     * @see [PanacheEntityBase.persist]
     */
    fun persistAndFlush() {
        JpaOperations.persist(this)
        JpaOperations.flush(this)
    }

    /**
     * Delete this entity from the database, if it is already persisted.
     *
     * @see [PanacheEntityBase.isPersistent]
     * @see [PanacheCompanion.deleteAll]
     */
    fun delete() {
        JpaOperations.delete(this)
    }

    /**
     * Flushes all pending changes to the database.
     */
    fun flush() {
        JpaOperations.flush()
    }
}
