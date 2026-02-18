package io.quarkus.hibernate.panache.stateless.reactive;

import io.quarkus.hibernate.panache.PanacheEntityMarker;
import io.quarkus.hibernate.panache.runtime.spi.PanacheOperations;
import io.quarkus.hibernate.panache.runtime.spi.PanacheReactiveOperations;
import io.quarkus.hibernate.panache.stateless.PanacheStatelessEntityOperations;
import io.smallrye.mutiny.Uni;

public interface PanacheStatelessReactiveEntity<Entity extends PanacheEntityMarker<Entity>>
        extends PanacheStatelessEntityOperations<Entity, Uni<Entity>, Uni<Boolean>> {

    private PanacheReactiveOperations operations() {
        return PanacheOperations.getReactiveManaged();
    }

    /**
     * Insert this entity in the database. This will set your ID field if it is not already set.
     */
    @Override
    public default Uni<Entity> insert() {
        return operations().insert(this).replaceWith((Entity) this);
    }

    /**
     * Delete this entity from the database.
     */
    @Override
    public default Uni<Entity> delete() {
        return operations().delete(this).replaceWith((Entity) this);
    }

    /**
     * Update this entity in the database.
     */
    @Override
    public default Uni<Entity> update() {
        return operations().update(this).replaceWith((Entity) this);
    }

    /**
     * Insert or update this entity in the database. An insert will be performed if the entity does not already exist
     * in the database, otherwise it will be updated. Note that you cannot upsert an entity with a null ID.
     */
    @Override
    public default Uni<Entity> upsert() {
        return operations().upsert(this).replaceWith((Entity) this);
    }
}
