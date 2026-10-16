package io.quarkus.data.hibernate;

import io.quarkus.data.hibernate.managed.blocking.BlockingManagedEntity;
import io.quarkus.data.hibernate.managed.reactive.ReactiveManagedEntity;
import io.quarkus.data.hibernate.runtime.spi.PanacheBlockingOperations;
import io.quarkus.data.hibernate.runtime.spi.PanacheOperations;
import io.quarkus.data.hibernate.runtime.spi.PanacheReactiveOperations;
import io.quarkus.data.hibernate.stateless.blocking.BlockingRecordEntity;
import io.quarkus.data.hibernate.stateless.reactive.ReactiveRecordEntity;
import io.smallrye.mutiny.Uni;

public interface EntitySwitcher {
    default BlockingManagedEntity managedBlocking() {
        if (this instanceof BlockingManagedEntity) {
            return (BlockingManagedEntity) this;
        } else {
            return new PanacheManagedBlockingEntityOperationsImpl(this);
        }
    }

    default ReactiveManagedEntity managedReactive() {
        if (this instanceof ReactiveManagedEntity) {
            return (ReactiveManagedEntity) this;
        } else {
            return new PanacheManagedReactiveEntityOperationsImpl(this);
        }
    }

    default ReactiveRecordEntity statelessReactive() {
        if (this instanceof ReactiveRecordEntity) {
            return (ReactiveRecordEntity) this;
        } else {
            return new PanacheStatelessReactiveEntityOperationsImpl(this);
        }
    }

    default BlockingRecordEntity statelessBlocking() {
        if (this instanceof BlockingRecordEntity) {
            return (BlockingRecordEntity) this;
        } else {
            return new PanacheStatelessBlockingEntityOperationsImpl(this);
        }
    }

    // FIXME: move to runtime
    static class PanacheManagedBlockingEntityOperationsImpl implements BlockingManagedEntity {

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
    static class PanacheManagedReactiveEntityOperationsImpl implements ReactiveManagedEntity {

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
    static class PanacheStatelessReactiveEntityOperationsImpl implements ReactiveRecordEntity {

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
    static class PanacheStatelessBlockingEntityOperationsImpl implements BlockingRecordEntity {

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
