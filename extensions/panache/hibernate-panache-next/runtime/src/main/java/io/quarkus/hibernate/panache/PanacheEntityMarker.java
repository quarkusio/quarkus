package io.quarkus.hibernate.panache;

import io.quarkus.hibernate.panache.managed.blocking.PanacheManagedBlockingEntity;
import io.quarkus.hibernate.panache.managed.reactive.PanacheManagedReactiveEntity;
import io.quarkus.hibernate.panache.runtime.spi.PanacheBlockingOperations;
import io.quarkus.hibernate.panache.runtime.spi.PanacheOperations;
import io.quarkus.hibernate.panache.runtime.spi.PanacheReactiveOperations;
import io.quarkus.hibernate.panache.stateless.blocking.PanacheStatelessBlockingEntity;
import io.quarkus.hibernate.panache.stateless.reactive.PanacheStatelessReactiveEntity;
import io.smallrye.mutiny.Uni;

public interface PanacheEntityMarker {
    default PanacheManagedBlockingEntity managedBlocking() {
        if (this instanceof PanacheManagedBlockingEntity) {
            return (PanacheManagedBlockingEntity) this;
        } else {
            return new PanacheManagedBlockingEntityOperationsImpl(this);
        }
    }

    default PanacheManagedReactiveEntity managedReactive() {
        if (this instanceof PanacheManagedReactiveEntity) {
            return (PanacheManagedReactiveEntity) this;
        } else {
            return new PanacheManagedReactiveEntityOperationsImpl(this);
        }
    }

    default PanacheStatelessReactiveEntity statelessReactive() {
        if (this instanceof PanacheStatelessReactiveEntity) {
            return (PanacheStatelessReactiveEntity) this;
        } else {
            return new PanacheStatelessReactiveEntityOperationsImpl(this);
        }
    }

    default PanacheStatelessBlockingEntity statelessBlocking() {
        if (this instanceof PanacheStatelessBlockingEntity) {
            return (PanacheStatelessBlockingEntity) this;
        } else {
            return new PanacheStatelessBlockingEntityOperationsImpl(this);
        }
    }

    // FIXME: move to runtime
    static class PanacheManagedBlockingEntityOperationsImpl implements PanacheManagedBlockingEntity {

        private Object entity;

        public PanacheManagedBlockingEntityOperationsImpl(Object entity) {
            this.entity = entity;
        }

        private PanacheBlockingOperations operations() {
            return PanacheOperations.getBlockingManaged();
        }

        @Override
        public Void persist() {
            return operations().persist(entity);
        }

        @Override
        public Void persistAndFlush() {
            return operations().persistAndFlush(entity);
        }

        @Override
        public Void delete() {
            return operations().delete(entity);
        }

        @Override
        public Boolean isPersistent() {
            return operations().isPersistent(entity);
        }
    }

    // FIXME: move to runtime
    static class PanacheManagedReactiveEntityOperationsImpl implements PanacheManagedReactiveEntity {

        private Object entity;

        public PanacheManagedReactiveEntityOperationsImpl(Object entity) {
            this.entity = entity;
        }

        private PanacheReactiveOperations operations() {
            return PanacheOperations.getReactiveManaged();
        }

        @Override
        public Uni<Void> persist() {
            return operations().persist(entity);
        }

        @Override
        public Uni<Void> persistAndFlush() {
            return operations().persistAndFlush(entity);
        }

        @Override
        public Uni<Void> delete() {
            return operations().delete(entity);
        }

        @Override
        public Uni<Boolean> isPersistent() {
            return operations().isPersistent(entity);
        }
    }

    // FIXME: move to runtime
    static class PanacheStatelessReactiveEntityOperationsImpl implements PanacheStatelessReactiveEntity {

        private Object entity;

        public PanacheStatelessReactiveEntityOperationsImpl(Object entity) {
            this.entity = entity;
        }

        private PanacheReactiveOperations operations() {
            return PanacheOperations.getReactiveStateless();
        }

        @Override
        public Uni<Void> insert() {
            return operations().insert(entity);
        }

        @Override
        public Uni<Void> update() {
            return operations().update(entity);
        }

        @Override
        public Uni<Void> upsert() {
            return operations().upsert(entity);
        }

        @Override
        public Uni<Void> delete() {
            return operations().delete(entity);
        }
    }

    // FIXME: move to runtime
    static class PanacheStatelessBlockingEntityOperationsImpl implements PanacheStatelessBlockingEntity {

        private Object entity;

        public PanacheStatelessBlockingEntityOperationsImpl(Object entity) {
            this.entity = entity;
        }

        private PanacheBlockingOperations operations() {
            return PanacheOperations.getBlockingStateless();
        }

        @Override
        public Void insert() {
            return operations().insert(entity);
        }

        @Override
        public Void update() {
            return operations().update(entity);
        }

        @Override
        public Void upsert() {
            return operations().upsert(entity);
        }

        @Override
        public Void delete() {
            return operations().delete(entity);
        }
    }
}
