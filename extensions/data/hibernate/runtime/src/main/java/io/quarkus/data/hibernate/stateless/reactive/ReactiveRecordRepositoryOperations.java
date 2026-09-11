package io.quarkus.data.hibernate.stateless.reactive;

import java.util.stream.Stream;

import org.hibernate.reactive.mutiny.Mutiny;

import io.quarkus.data.hibernate.runtime.spi.PanacheOperations;
import io.quarkus.data.hibernate.runtime.spi.PanacheReactiveOperations;
import io.quarkus.data.hibernate.stateless.RecordRepositoryOperations;
import io.smallrye.mutiny.Uni;

public interface ReactiveRecordRepositoryOperations<Entity, Id>
        extends RecordRepositoryOperations<Entity, Uni<Mutiny.StatelessSession>, Uni<Void>, Uni<Boolean>, Id> {

    // See BlockingManagedRepositoryOperations for the explanation of the doGetEntityClass() pattern.

    private Class<? extends Entity> doGetEntityClass() {
        throw new UnsupportedOperationException(
                "doGetEntityClass() should be provided by the generated repository implementation");
    }

    private Class<? extends Entity> getEntityClass() {
        return doGetEntityClass();
    }

    private PanacheReactiveOperations operations() {
        return PanacheOperations.getReactiveStateless();
    }

    // Operations

    @Override
    default Uni<Mutiny.StatelessSession> getSession() {
        return operations().getStatelessSession(getEntityClass());
    }

    @Override
    default Uni<Void> insert(Entity entity) {
        return operations().insert(entity);
    }

    @Override
    default Uni<Void> delete(Entity entity) {
        return operations().delete(entity);
    }

    @Override
    default Uni<Void> update(Entity entity) {
        return operations().update(entity);
    }

    @Override
    default Uni<Void> upsert(Entity entity) {
        return operations().upsert(entity);
    }

    @Override
    default Uni<Void> insert(Iterable<Entity> entities) {
        return operations().insert(entities);
    }

    @Override
    default Uni<Void> insert(Stream<Entity> entities) {
        return operations().insert(entities);
    }

    @Override
    default Uni<Void> insert(Entity firstEntity, @SuppressWarnings("unchecked") Entity... entities) {
        return operations().insert(firstEntity, entities);
    }
}
