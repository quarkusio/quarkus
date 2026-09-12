package io.quarkus.data.hibernate;

import io.quarkus.arc.Arc;
import io.quarkus.data.hibernate.managed.blocking.BlockingManagedRepositoryBase;
import io.quarkus.data.hibernate.managed.reactive.ReactiveManagedRepositoryBase;
import io.quarkus.data.hibernate.stateless.blocking.BlockingRecordRepositoryBase;
import io.quarkus.data.hibernate.stateless.reactive.ReactiveRecordRepositoryBase;
import io.quarkus.hibernate.orm.panache.common.runtime.AbstractJpaOperations;

public interface RepositorySwitcher<Entity, Id> {
    default BlockingManagedRepositoryBase<Entity, Id> managedBlocking() {
        if (this instanceof BlockingManagedRepositoryBase) {
            return (BlockingManagedRepositoryBase<Entity, Id>) this;
        }
        return findRepository(BlockingManagedRepositoryBase.class);
    }

    default ReactiveManagedRepositoryBase<Entity, Id> managedReactive() {
        if (this instanceof ReactiveManagedRepositoryBase) {
            return (ReactiveManagedRepositoryBase<Entity, Id>) this;
        }
        return findRepository(ReactiveManagedRepositoryBase.class);
    }

    default BlockingRecordRepositoryBase<Entity, Id> statelessBlocking() {
        if (this instanceof BlockingRecordRepositoryBase) {
            return (BlockingRecordRepositoryBase<Entity, Id>) this;
        }
        return findRepository(BlockingRecordRepositoryBase.class);
    }

    default ReactiveRecordRepositoryBase<Entity, Id> statelessReactive() {
        if (this instanceof ReactiveRecordRepositoryBase) {
            return (ReactiveRecordRepositoryBase<Entity, Id>) this;
        }
        return findRepository(ReactiveRecordRepositoryBase.class);
    }

    @SuppressWarnings("unchecked")
    private <T> T findRepository(Class<T> targetType) {
        Class<?> entityClass = AbstractJpaOperations.getRepositoryEntityClass(getClass());
        Class<? extends T> repoClass = AbstractJpaOperations.findRepositoryClass(entityClass, targetType);
        if (repoClass == null) {
            throw new IllegalStateException(
                    "No " + targetType.getSimpleName() + " repository found for entity " + entityClass.getName());
        }
        return Arc.container().select(repoClass).get();
    }
}
