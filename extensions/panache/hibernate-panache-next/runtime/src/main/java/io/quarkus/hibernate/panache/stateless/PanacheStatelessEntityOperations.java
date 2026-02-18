package io.quarkus.hibernate.panache.stateless;

import io.quarkus.hibernate.panache.PanacheEntityMarker;

public interface PanacheStatelessEntityOperations<Entity extends PanacheEntityMarker<Entity>, CompletionEntity, Confirmation>
        extends PanacheEntityMarker<Entity> {
    /**
     * Insert this entity in the database. This will set your ID field if it is not already set.
     *
     * @return this entity
     */
    public CompletionEntity insert();

    /**
     * Delete this entity from the database, if it is already persisted.
     *
     * @return this entity
     */
    public CompletionEntity delete();

    /**
     * Update this entity to the database.
     *
     * @return this entity
     */
    public CompletionEntity update();

    /**
     * Insert or update this entity in the database. An insert will be performed if the entity does not already exist
     * in the database, otherwise it will be updated. Note that you cannot upsert an entity with a null ID.
     *
     * @return this entity
     */
    public CompletionEntity upsert();
}
