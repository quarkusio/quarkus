package io.quarkus.hibernate.panache.managed.blocking;

import java.util.stream.Stream;

import org.hibernate.Session;

import io.quarkus.hibernate.orm.panache.common.runtime.AbstractJpaOperations;
import io.quarkus.hibernate.panache.managed.PanacheManagedRepositoryOperations;
import io.quarkus.hibernate.panache.runtime.spi.PanacheBlockingOperations;
import io.quarkus.hibernate.panache.runtime.spi.PanacheOperations;

public interface PanacheManagedBlockingRepositoryOperations<Entity, Id>
        extends PanacheManagedRepositoryOperations<Entity, Session, Void, Boolean, Id> {

    private Class<? extends Entity> getEntityClass() {
        return AbstractJpaOperations.getRepositoryEntityClass(getClass());
    }

    private PanacheBlockingOperations operations() {
        return PanacheOperations.getBlockingManaged();
    }

    // Operations

    @Override
    default Session getSession() {
        return operations().getSession(getEntityClass());
    }

    @Override
    default Void persist(Entity entity) {
        return operations().persist(entity);
    }

    @Override
    default Void persistAndFlush(Entity entity) {
        return operations().persistAndFlush(entity);
    }

    @Override
    default Void delete(Entity entity) {
        return operations().delete(entity);
    }

    @Override
    default Boolean isPersistent(Entity entity) {
        return operations().isPersistent(entity);
    }

    @Override
    default Void flush() {
        return operations().flush(getEntityClass());
    }

    @Override
    default Void persist(Iterable<Entity> entities) {
        return operations().persist(entities);
    }

    @Override
    default Void persist(Stream<Entity> entities) {
        return operations().persist(entities);
    }

    @Override
    default Void persist(Entity firstEntity, @SuppressWarnings("unchecked") Entity... entities) {
        return operations().persist(firstEntity, entities);
    }
}
