package io.quarkus.hibernate.panache.managed.reactive;

import jakarta.json.bind.annotation.JsonbTransient;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.quarkus.hibernate.panache.PanacheEntityMarker;
import io.quarkus.hibernate.panache.managed.PanacheManagedEntityOperations;
import io.quarkus.hibernate.panache.runtime.spi.PanacheOperations;
import io.quarkus.hibernate.panache.runtime.spi.PanacheReactiveOperations;
import io.smallrye.mutiny.Uni;

public interface PanacheManagedReactiveEntity<Entity extends PanacheEntityMarker<Entity>>
        extends PanacheManagedEntityOperations<Entity, Uni<Entity>, Uni<Boolean>> {

    private PanacheReactiveOperations operations() {
        return PanacheOperations.getReactiveManaged();
    }

    /**
     * Persist this entity in the database, if not already persisted. This will set your ID field if it is not already set.
     *
     * @see #isPersistent()
     * @see #persist(Iterable)
     * @see #persist(Stream)
     * @see #persist(Object, Object...)
     */
    @Override
    public default Uni<Entity> persist() {
        return operations().persist(this).replaceWith((Entity) this);
    }

    /**
     * Persist this entity in the database, if not already persisted. This will set your ID field if it is not already set.
     * Then flushes all pending changes to the database.
     *
     * @see #isPersistent()
     * @see #persist(Iterable)
     * @see #persist(Stream)
     * @see #persist(Object, Object...)
     */
    @Override
    public default Uni<Entity> persistAndFlush() {
        return operations().persistAndFlush(this).replaceWith((Entity) this);
    }

    /**
     * Delete this entity from the database, if it is already persisted.
     *
     * @see #isPersistent()
     * @see #delete(String, Object...)
     * @see #delete(String, Map)
     * @see #deleteAll()
     */
    @Override
    public default Uni<Entity> delete() {
        return operations().delete(this).replaceWith((Entity) this);
    }

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
    @Override
    public default Uni<Boolean> isPersistent() {
        return operations().isPersistent(this);
    }

}
