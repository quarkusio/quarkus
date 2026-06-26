package io.quarkus.data.hibernate.stateless.blocking;

import java.util.stream.Stream;

import org.hibernate.StatelessSession;

import io.quarkus.data.hibernate.runtime.spi.PanacheBlockingOperations;
import io.quarkus.data.hibernate.runtime.spi.PanacheOperations;
import io.quarkus.data.hibernate.stateless.PanacheStatelessRepositoryOperations;
import io.quarkus.hibernate.orm.panache.common.runtime.AbstractJpaOperations;

public interface PanacheStatelessBlockingRepositoryOperations<Entity, Id>
        extends PanacheStatelessRepositoryOperations<Entity, StatelessSession, Void, Boolean, Id> {

    private Class<? extends Entity> getEntityClass() {
        return AbstractJpaOperations.getRepositoryEntityClass(getClass());
    }

    private PanacheBlockingOperations operations() {
        return PanacheOperations.getBlockingStateless();
    }

    // Operations

    @Override
    default StatelessSession getSession() {
        return operations().getStatelessSession(getEntityClass());
    }

    @Override
    default Void insert(Entity entity) {
        return operations().insert(entity);
    }

    @Override
    default Void delete(Entity entity) {
        return operations().delete(entity);
    }

    @Override
    default Void update(Entity entity) {
        return operations().update(entity);
    }

    @Override
    default Void upsert(Entity entity) {
        return operations().upsert(entity);
    }

    @Override
    default Void insert(Iterable<Entity> entities) {
        return operations().insert(entities);
    }

    @Override
    default Void insert(Stream<Entity> entities) {
        return operations().insert(entities);
    }

    @Override
    default Void insert(Entity firstEntity, @SuppressWarnings("unchecked") Entity... entities) {
        return operations().insert(firstEntity, entities);
    }
}
