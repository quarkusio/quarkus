package io.quarkus.data.hibernate;

import io.quarkus.data.hibernate.managed.blocking.PanacheManagedBlockingRepositoryBase;
import io.quarkus.data.hibernate.managed.reactive.PanacheManagedReactiveRepositoryBase;
import io.quarkus.data.hibernate.stateless.blocking.PanacheStatelessBlockingRepositoryBase;
import io.quarkus.data.hibernate.stateless.reactive.PanacheStatelessReactiveRepositoryBase;
import io.quarkus.hibernate.orm.panache.common.runtime.AbstractJpaOperations;

public interface RepositorySwitcher<Entity, Id> {
    default PanacheManagedBlockingRepositoryBase<Entity, Id> managedBlocking() {
        // FIXME: generate in impl
        throw AbstractJpaOperations.implementationInjectionMissing();
    }

    default PanacheManagedReactiveRepositoryBase<Entity, Id> managedReactive() {
        // FIXME: generate in impl
        throw AbstractJpaOperations.implementationInjectionMissing();
    }

    default PanacheStatelessBlockingRepositoryBase<Entity, Id> statelessBlocking() {
        // FIXME: generate in impl
        throw AbstractJpaOperations.implementationInjectionMissing();
    }

    default PanacheStatelessReactiveRepositoryBase<Entity, Id> statelessReactive() {
        // FIXME: generate in impl
        throw AbstractJpaOperations.implementationInjectionMissing();
    }
}
