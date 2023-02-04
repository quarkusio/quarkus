package io.quarkus.hibernate.reactive.panache.kotlin

import com.fasterxml.jackson.annotation.JsonIgnore
import io.quarkus.hibernate.reactive.panache.kotlin.runtime.KotlinJpaOperations.Companion.INSTANCE
import io.smallrye.common.annotation.CheckReturnValue
import io.smallrye.mutiny.Uni
import jakarta.json.bind.annotation.JsonbTransient

interface PanacheEntityBase {
    /**
     * Returns true if this entity is persistent in the database. If yes, all modifications to
     * its persistent fields will be automatically committed to the database at transaction
     * commit time.
     *
     * @return true if this entity is persistent in the database.
     */
    @JsonbTransient
    @JsonIgnore
    fun isPersistent(): Boolean = INSTANCE.isPersistent(this)

    /**
     * Persist this entity in the database.
     * This will set its ID field if not already set.
     */
    @CheckReturnValue
    fun <T : PanacheEntityBase> persist(): Uni<T> {
        return INSTANCE.persist(this).map { this as T }
    }

    /**
     * Flushes all pending changes to the database.
     *
     * @return
     */
    @CheckReturnValue
    fun flush(): Uni<Void> {
        return INSTANCE.flush()
    }

    /**
     * Persist this entity in the database, if not already persisted. This will set your ID field if it is not already set.
     * Then flushes all pending changes to the database.
     *
     * @see [isPersistent] isPersistent
     * @see [flush] flush
     * @see [persist] persist
     */
    @Suppress("UNCHECKED_CAST")
    @CheckReturnValue
    fun <T : PanacheEntityBase> persistAndFlush(): Uni<T> {
        return INSTANCE
            .persist(this)
            .flatMap { INSTANCE.flush() }
            .map { this as T }
    }

    /**
     * Delete this entity from the database if it is already persisted.
     *
     * @see [deleteAll]
     */
    fun delete(): Uni<Void> = INSTANCE.delete(this)
}
