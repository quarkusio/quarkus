package io.quarkus.hibernate.panache.stateless.reactive;

import io.quarkus.hibernate.panache.runtime.spi.PanacheOperations;
import io.quarkus.hibernate.panache.runtime.spi.PanacheReactiveOperations;
import io.quarkus.hibernate.panache.stateless.PanacheStatelessEntityOperations;
import io.smallrye.mutiny.Uni;

public interface PanacheStatelessReactiveEntity extends PanacheStatelessEntityOperations<Uni<Void>, Uni<Boolean>> {

    private PanacheReactiveOperations operations() {
        return PanacheOperations.getReactiveManaged();
    }

    @Override
    public default Uni<Void> insert() {
        return operations().insert(this);
    }

    @Override
    public default Uni<Void> delete() {
        return operations().delete(this);
    }

    @Override
    public default Uni<Void> update() {
        return operations().update(this);
    }

    @Override
    public default Uni<Void> upsert() {
        return operations().upsert(this);
    }
}
