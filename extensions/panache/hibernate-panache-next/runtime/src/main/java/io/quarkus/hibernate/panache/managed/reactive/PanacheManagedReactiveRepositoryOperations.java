package io.quarkus.hibernate.panache.managed.reactive;

import java.util.stream.Stream;

import org.hibernate.reactive.mutiny.Mutiny;

import io.quarkus.hibernate.orm.panache.common.runtime.AbstractJpaOperations;
import io.quarkus.hibernate.panache.managed.PanacheManagedRepositoryOperations;
import io.quarkus.hibernate.panache.runtime.spi.PanacheOperations;
import io.quarkus.hibernate.panache.runtime.spi.PanacheReactiveOperations;
import io.smallrye.mutiny.Uni;

public interface PanacheManagedReactiveRepositoryOperations<Entity, Id>
        extends PanacheManagedRepositoryOperations<Entity, Uni<Mutiny.Session>, Uni<Void>, Uni<Boolean>, Id> {

    private Class<? extends Entity> getEntityClass() {
        return AbstractJpaOperations.getRepositoryEntityClass(getClass());
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
