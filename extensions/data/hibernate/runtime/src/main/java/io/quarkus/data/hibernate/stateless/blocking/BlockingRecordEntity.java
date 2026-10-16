package io.quarkus.data.hibernate.stateless.blocking;

import io.quarkus.data.hibernate.runtime.spi.PanacheBlockingOperations;
import io.quarkus.data.hibernate.runtime.spi.PanacheOperations;
import io.quarkus.data.hibernate.stateless.RecordEntityOperations;

public interface BlockingRecordEntity extends RecordEntityOperations<Void, Boolean> {

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
