package io.quarkus.hibernate.panache.stateless.blocking;

import io.quarkus.hibernate.panache.PanacheEntityMarker;
import io.quarkus.hibernate.panache.runtime.spi.PanacheBlockingOperations;
import io.quarkus.hibernate.panache.runtime.spi.PanacheOperations;
import io.quarkus.hibernate.panache.stateless.PanacheStatelessEntityOperations;

public interface PanacheStatelessBlockingEntity<Entity extends PanacheEntityMarker<Entity>>
        extends PanacheStatelessEntityOperations<Entity, Entity, Boolean> {

    private PanacheBlockingOperations operations() {
        return PanacheOperations.getBlockingStateless();
    }

    /**
     * Insert this entity in the database. This will set your ID field if it is not already set.
     *
     * @see #insert(Iterable)
     * @see #insert(Stream)
     * @see #insert(Object, Object...)
     */
    @Override
    public default Entity insert() {
        operations().insert(this);
        return (Entity) this;
    }

    /**
     * Delete this entity from the database.
     *
     * @see #delete(String, Object...)
     * @see #delete(String, Map)
     * @see #deleteAll()
     */
    @Override
    public default Entity delete() {
        operations().delete(this);
        return (Entity) this;
    }

    /**
     * Update this entity in the database.
     */
    @Override
    public default Entity update() {
        operations().update(this);
        return (Entity) this;
    }

    /**
     * Insert or update this entity in the database. An insert will be performed if the entity does not already exist
     * in the database, otherwise it will be updated. Note that you cannot upsert an entity with a null ID.
     */
    @Override
    public default Entity upsert() {
        operations().upsert(this);
        return (Entity) this;
    }
}
