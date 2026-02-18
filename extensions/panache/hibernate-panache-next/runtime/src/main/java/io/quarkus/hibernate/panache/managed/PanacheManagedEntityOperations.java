package io.quarkus.hibernate.panache.managed;

import jakarta.json.bind.annotation.JsonbTransient;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.quarkus.hibernate.panache.PanacheEntityMarker;

public interface PanacheManagedEntityOperations<Entity extends PanacheEntityMarker<Entity>, CompletionEntity, Confirmation>
        extends PanacheEntityMarker<Entity> {
    /**
     * Persist this entity in the database, if not already persisted. This will set your ID field if it is not already set.
     *
     * @return this entity
     * @see #isPersistent()
     * @see #persist(Iterable)
     * @see #persist(Stream)
     * @see #persist(Object, Object...)
     */
    public CompletionEntity persist();

    /**
     * Persist this entity in the database, if not already persisted. This will set your ID field if it is not already set.
     * Then flushes all pending changes to the database.
     *
     * @return this entity
     * @see #isPersistent()
     * @see #persist(Iterable)
     * @see #persist(Stream)
     * @see #persist(Object, Object...)
     */
    public CompletionEntity persistAndFlush();

    /**
     * Delete this entity from the database, if it is already persisted.
     *
     * @return this entity
     * @see #isPersistent()
     * @see #delete(String, Object...)
     * @see #delete(String, Map)
     * @see #deleteAll()
     */
    public CompletionEntity delete();

    /**
     * Returns true if this entity is persistent in the database. If yes, all modifications to
     * its persistent fields will be automatically committed to the database at transaction
     * commit time.
     *
     * @return true if this entity is persistent in the database.
     */
    @JsonbTransient
    // @JsonIgnore is here to avoid serialization of this property with jackson
    @JsonIgnore
    public Confirmation isPersistent();

}
