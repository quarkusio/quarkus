package io.quarkus.hibernate.panache.stateless.reactive;

import io.quarkus.hibernate.panache.runtime.spi.PanacheOperations;
import io.quarkus.hibernate.panache.runtime.spi.PanacheReactiveOperations;
import io.quarkus.hibernate.panache.stateless.PanacheStatelessEntityOperations;
import io.smallrye.mutiny.Uni;

public interface PanacheStatelessReactiveEntity extends PanacheStatelessEntityOperations<Uni<Void>, Uni<Boolean>> {

    private PanacheReactiveOperations operations() {
        return PanacheOperations.getReactiveManaged();
    }

    /**
     * Insert this entity in the database. This will set your ID field if it is not already set.
     */
    @Override
    public default Uni<Void> insert() {
        return operations().insert(this);
    }

    /**
     * Delete this entity from the database.
     */
    @Override
    public default Uni<Void> delete() {
        return operations().delete(this);
    }

    /**
     * Update this entity in the database.
     */
    @Override
    public default Uni<Void> update() {
        return operations().update(this);
    }

    /**
     * Insert or update this entity in the database. An insert will be performed if the entity does not already exist
     * in the database, otherwise it will be updated. Note that you cannot upsert an entity with a null ID.
     */
    @Override
    public default Uni<Void> upsert() {
        return operations().upsert(this);
    }
}
