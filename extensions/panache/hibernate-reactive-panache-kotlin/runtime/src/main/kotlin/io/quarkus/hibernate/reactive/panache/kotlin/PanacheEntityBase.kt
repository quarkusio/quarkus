package io.quarkus.hibernate.reactive.panache.kotlin;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.quarkus.hibernate.reactive.panache.kotlin.runtime.KotlinJpaOperations.Companion.INSTANCE
import io.smallrye.mutiny.Uni


import javax.json.bind.annotation.JsonbTransient;
import javax.persistence.Transient;


/**
 * Represents an entity. If your Hibernate entities extend this class they gain auto-generated accessors
 * to all their public fields (unless annotated with [Transient]), as well as a lot of useful
 * methods. Unless you have a custom ID strategy, you should not extend this class directly but extend
 * [PanacheEntity] instead.
 *
 * @see PanacheEntity
 */
interface PanacheEntityBase {


    /**
     * Persist this entity in the database, if not already persisted. This will set your ID field if it is not already set.
     *
     * @return
     *
     * @see .isPersistent
     * @see .persist
     * @see .persist
     * @see .persist
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : PanacheEntityBase> persist(): Uni<T> {
        return INSTANCE.persist(this)
            .map { this as T }
    }

    /**
     * Persist this entity in the database, if not already persisted. This will set your ID field if it is not already set.
     * Then flushes all pending changes to the database.
     *
     * @return
     *
     * @see .isPersistent
     * @see .persist
     * @see .persist
     * @see .persist
     */
    fun persistAndFlush(): Uni<PanacheEntityBase> {
        return INSTANCE.persist(this)
            .flatMap { INSTANCE.flush() }
            .map { this }
    }

    /**
     * Delete this entity from the database, if it is already persisted.
     *
     * @return
     *
     * @see .isPersistent
     * @see .delete
     * @see .delete
     * @see .delete
     * @see .deleteAll
     */
    fun delete(): Uni<Void?>? {
        return INSTANCE.delete(this)
    }

    /**
     * Returns true if this entity is persistent in the database. If yes, all modifications to
     * its persistent fields will be automatically committed to the database at transaction
     * commit time.
     *
     * @return true if this entity is persistent in the database.
     */
    @JsonbTransient // @JsonIgnore is here to avoid serialization of this property with jackson
    @JsonIgnore
    fun isPersistent(): Boolean {
        return INSTANCE.isPersistent(this)
    }

    /**
     * Flushes all pending changes to the database.
     *
     * @return
     */
    fun flush(): Uni<Void?>? {
        return INSTANCE.flush()
    }

}
