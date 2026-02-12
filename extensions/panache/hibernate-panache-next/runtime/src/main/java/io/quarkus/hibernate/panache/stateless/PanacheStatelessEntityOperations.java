package io.quarkus.hibernate.panache.stateless;

import io.quarkus.hibernate.panache.PanacheEntityMarker;

public interface PanacheStatelessEntityOperations<Completion, Confirmation> extends PanacheEntityMarker {
    /**
     * Insert this entity in the database. This will set your ID field if it is not already set.
     */
    public Completion insert();

    /**
     * Delete this entity from the database, if it is already persisted.
     */
    public Completion delete();

    /**
     * Update this entity to the database.
     */
    public Completion update();

    /**
     * Insert or update this entity in the database. An insert will be performed if the entity does not already exist
     * in the database, otherwise it will be updated. Note that you cannot upsert an entity with a null ID.
     */
    public Completion upsert();
}
