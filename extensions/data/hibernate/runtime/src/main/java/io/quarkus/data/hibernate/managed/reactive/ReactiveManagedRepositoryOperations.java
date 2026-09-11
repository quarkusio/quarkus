package io.quarkus.data.hibernate.managed.reactive;

import java.util.stream.Stream;

import org.hibernate.reactive.mutiny.Mutiny;

import io.quarkus.data.hibernate.managed.ManagedRepositoryOperations;
import io.quarkus.data.hibernate.runtime.spi.PanacheOperations;
import io.quarkus.data.hibernate.runtime.spi.PanacheReactiveOperations;
import io.smallrye.mutiny.Uni;

public interface ReactiveManagedRepositoryOperations<Entity, Id>
        extends ManagedRepositoryOperations<Entity, Uni<Mutiny.Session>, Uni<Void>, Uni<Boolean>, Id> {

    // See BlockingManagedRepositoryOperations for the explanation of the doGetEntityClass() pattern.

    private Class<? extends Entity> doGetEntityClass() {
        throw new UnsupportedOperationException(
                "doGetEntityClass() should be provided by the generated repository implementation");
    }

    private Class<? extends Entity> getEntityClass() {
        return doGetEntityClass();
    }

    private PanacheReactiveOperations operations() {
        return PanacheOperations.getReactiveManaged();
    }

    // Operations

    @Override
    default Uni<Mutiny.Session> getSession() {
        return operations().getSession(getEntityClass());
    }

    @Override
    default Uni<Void> persist(Entity entity) {
        return operations().persist(entity);
    }

    @Override
    default Uni<Void> persistAndFlush(Entity entity) {
        return operations().persistAndFlush(entity);
    }

    @Override
    default Uni<Void> delete(Entity entity) {
        return operations().delete(entity);
    }

    @Override
    default Uni<Boolean> isPersistent(Entity entity) {
        return operations().isPersistent(entity);
    }

    @Override
    default Uni<Void> flush() {
        return operations().flush(getEntityClass());
    }

    @Override
    default Uni<Void> persist(Iterable<Entity> entities) {
        return operations().persist(entities);
    }

    @Override
    default Uni<Void> persist(Stream<Entity> entities) {
        return operations().persist(entities);
    }

    @Override
    default Uni<Void> persist(Entity firstEntity, @SuppressWarnings("unchecked") Entity... entities) {
        return operations().persist(firstEntity, entities);
    }
}
