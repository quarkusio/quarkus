package io.quarkus.hibernate.panache.managed;

import jakarta.json.bind.annotation.JsonbTransient;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.quarkus.hibernate.panache.PanacheEntityMarker;

public interface PanacheManagedEntityOperations<Completion, Confirmation> extends PanacheEntityMarker {
    /**
     * Persist this entity in the database, if not already persisted. This will set your ID field if it is not already set.
     *
     * @see #isPersistent()
     */
    public Completion persist();

    /**
     * Persist this entity in the database, if not already persisted. This will set your ID field if it is not already set.
     * Then flushes all pending changes to the database.
     *
     * @see #isPersistent()
     */
    public Completion persistAndFlush();

    /**
     * Delete this entity from the database, if it is already persisted.
     *
     * @see #isPersistent()
     */
    public Completion delete();

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
