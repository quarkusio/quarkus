package io.quarkus.data.hibernate;

import io.quarkus.data.hibernate.managed.blocking.BlockingManagedRepositoryBase;
import io.quarkus.data.hibernate.managed.reactive.ReactiveManagedRepositoryBase;
import io.quarkus.data.hibernate.stateless.blocking.BlockingRecordRepositoryBase;
import io.quarkus.data.hibernate.stateless.reactive.ReactiveRecordRepositoryBase;
import io.quarkus.hibernate.orm.panache.common.runtime.AbstractJpaOperations;

public interface RepositorySwitcher<Entity, Id> {
    default BlockingManagedRepositoryBase<Entity, Id> managedBlocking() {
        // FIXME: generate in impl
        throw AbstractJpaOperations.implementationInjectionMissing();
    }

    default ReactiveManagedRepositoryBase<Entity, Id> managedReactive() {
        // FIXME: generate in impl
        throw AbstractJpaOperations.implementationInjectionMissing();
    }

    default BlockingRecordRepositoryBase<Entity, Id> statelessBlocking() {
        // FIXME: generate in impl
        throw AbstractJpaOperations.implementationInjectionMissing();
    }

    default ReactiveRecordRepositoryBase<Entity, Id> statelessReactive() {
        // FIXME: generate in impl
        throw AbstractJpaOperations.implementationInjectionMissing();
    }
}
