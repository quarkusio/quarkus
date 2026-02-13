package io.quarkus.hibernate.panache.stateless.blocking;

import java.util.Map;
import java.util.stream.Stream;

import io.quarkus.hibernate.panache.runtime.spi.PanacheBlockingOperations;
import io.quarkus.hibernate.panache.runtime.spi.PanacheOperations;
import io.quarkus.hibernate.panache.stateless.PanacheStatelessEntityOperations;

public interface PanacheStatelessBlockingEntity extends PanacheStatelessEntityOperations<Void, Boolean> {

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
    public default Void insert() {
        return operations().insert(this);
    }

    /**
     * Delete this entity from the database.
     *
     * @see #delete(String, Object...)
     * @see #delete(String, Map)
     * @see #deleteAll()
     */
    @Override
    public default Void delete() {
        return operations().delete(this);
    }

    /**
     * Update this entity in the database.
     */
    @Override
    public default Void update() {
        return operations().update(this);
    }
}
