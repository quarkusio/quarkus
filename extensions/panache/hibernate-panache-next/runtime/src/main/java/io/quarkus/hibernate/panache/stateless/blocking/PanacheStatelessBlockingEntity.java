package io.quarkus.hibernate.panache.stateless.blocking;

import io.quarkus.hibernate.panache.runtime.spi.PanacheBlockingOperations;
import io.quarkus.hibernate.panache.runtime.spi.PanacheOperations;
import io.quarkus.hibernate.panache.stateless.PanacheStatelessEntityOperations;

public interface PanacheStatelessBlockingEntity extends PanacheStatelessEntityOperations<Void, Boolean> {

    private PanacheBlockingOperations operations() {
        return PanacheOperations.getBlockingStateless();
    }

    @Override
    public default Void insert() {
        return operations().insert(this);
    }

    @Override
    public default Void delete() {
        return operations().delete(this);
    }

    @Override
    public default Void update() {
        return operations().update(this);
    }

    @Override
    public default Void upsert() {
        return operations().upsert(this);
    }
}
