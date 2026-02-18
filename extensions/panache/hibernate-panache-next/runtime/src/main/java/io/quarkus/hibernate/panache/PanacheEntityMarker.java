package io.quarkus.hibernate.panache;

import io.quarkus.hibernate.panache.managed.blocking.PanacheManagedBlockingEntity;
import io.quarkus.hibernate.panache.managed.reactive.PanacheManagedReactiveEntity;
import io.quarkus.hibernate.panache.runtime.spi.PanacheBlockingOperations;
import io.quarkus.hibernate.panache.runtime.spi.PanacheOperations;
import io.quarkus.hibernate.panache.runtime.spi.PanacheReactiveOperations;
import io.quarkus.hibernate.panache.stateless.blocking.PanacheStatelessBlockingEntity;
import io.quarkus.hibernate.panache.stateless.reactive.PanacheStatelessReactiveEntity;
import io.smallrye.mutiny.Uni;

public interface PanacheEntityMarker<Entity extends PanacheEntityMarker<Entity>> {
    default PanacheManagedBlockingEntity<Entity> managedBlocking() {
        if (this instanceof PanacheManagedBlockingEntity) {
            return (PanacheManagedBlockingEntity) this;
        } else {
            return new PanacheManagedBlockingEntityOperationsImpl(this);
        }
    }

    default PanacheManagedReactiveEntity<Entity> managedReactive() {
        if (this instanceof PanacheManagedReactiveEntity) {
            return (PanacheManagedReactiveEntity) this;
        } else {
            return new PanacheManagedReactiveEntityOperationsImpl(this);
        }
    }

    default PanacheStatelessReactiveEntity<Entity> statelessReactive() {
        if (this instanceof PanacheStatelessReactiveEntity) {
            return (PanacheStatelessReactiveEntity) this;
        } else {
            return new PanacheStatelessReactiveEntityOperationsImpl(this);
        }
    }

    default PanacheStatelessBlockingEntity<Entity> statelessBlocking() {
        if (this instanceof PanacheStatelessBlockingEntity) {
            return (PanacheStatelessBlockingEntity) this;
        } else {
            return new PanacheStatelessBlockingEntityOperationsImpl(this);
        }
    }

    // FIXME: move to runtime
    static class PanacheManagedBlockingEntityOperationsImpl<Entity extends PanacheEntityMarker<Entity>>
            implements PanacheManagedBlockingEntity<Entity> {

        private Entity entity;

        public PanacheManagedBlockingEntityOperationsImpl(Entity entity) {
            this.entity = entity;
        }

        private PanacheBlockingOperations operations() {
            return PanacheOperations.getBlockingManaged();
        }

        @Override
        public Entity persist() {
            operations().persist(entity);
            return entity;
        }

        @Override
        public Entity persistAndFlush() {
            operations().persistAndFlush(entity);
            return entity;
        }

        @Override
        public Entity delete() {
            operations().delete(entity);
            return entity;
        }

        @Override
        public Boolean isPersistent() {
            return operations().isPersistent(entity);
        }
    }

    // FIXME: move to runtime
    static class PanacheManagedReactiveEntityOperationsImpl<Entity extends PanacheEntityMarker<Entity>>
            implements PanacheManagedReactiveEntity<Entity> {

        private Entity entity;

        public PanacheManagedReactiveEntityOperationsImpl(Entity entity) {
            this.entity = entity;
        }

        private PanacheReactiveOperations operations() {
            return PanacheOperations.getReactiveManaged();
        }

        @Override
        public Uni<Entity> persist() {
            return operations().persist(entity).replaceWith(entity);
        }

        @Override
        public Uni<Entity> persistAndFlush() {
            return operations().persistAndFlush(entity).replaceWith(entity);
        }

        @Override
        public Uni<Entity> delete() {
            return operations().delete(entity).replaceWith(entity);
        }

        @Override
        public Uni<Boolean> isPersistent() {
            return operations().isPersistent(entity);
        }
    }

    // FIXME: move to runtime
    static class PanacheStatelessReactiveEntityOperationsImpl<Entity extends PanacheEntityMarker<Entity>>
            implements PanacheStatelessReactiveEntity<Entity> {

        private Entity entity;

        public PanacheStatelessReactiveEntityOperationsImpl(Entity entity) {
            this.entity = entity;
        }

        private PanacheReactiveOperations operations() {
            return PanacheOperations.getReactiveStateless();
        }

        @Override
        public Uni<Entity> insert() {
            return operations().insert(entity).replaceWith(entity);
        }

        @Override
        public Uni<Entity> update() {
            return operations().update(entity).replaceWith(entity);
        }

        @Override
        public Uni<Entity> upsert() {
            return operations().upsert(entity).replaceWith(entity);
        }

        @Override
        public Uni<Entity> delete() {
            return operations().delete(entity).replaceWith(entity);
        }
    }

    // FIXME: move to runtime
    static class PanacheStatelessBlockingEntityOperationsImpl<Entity extends PanacheEntityMarker<Entity>>
            implements PanacheStatelessBlockingEntity<Entity> {

        private Entity entity;

        public PanacheStatelessBlockingEntityOperationsImpl(Entity entity) {
            this.entity = entity;
        }

        private PanacheBlockingOperations operations() {
            return PanacheOperations.getBlockingStateless();
        }

        @Override
        public Entity insert() {
            operations().insert(entity);
            return entity;
        }

        @Override
        public Entity update() {
            operations().update(entity);
            return entity;
        }

        @Override
        public Entity upsert() {
            operations().upsert(entity);
            return entity;
        }

        @Override
        public Entity delete() {
            operations().delete(entity);
            return entity;
        }
    }
}
