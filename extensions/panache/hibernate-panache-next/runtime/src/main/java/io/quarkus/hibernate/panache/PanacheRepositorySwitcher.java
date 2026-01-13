package io.quarkus.hibernate.panache;

import io.quarkus.hibernate.orm.panache.common.runtime.AbstractJpaOperations;
import io.quarkus.hibernate.panache.managed.blocking.PanacheManagedBlockingRepositoryBase;
import io.quarkus.hibernate.panache.managed.reactive.PanacheManagedReactiveRepositoryBase;
import io.quarkus.hibernate.panache.stateless.blocking.PanacheStatelessBlockingRepositoryBase;
import io.quarkus.hibernate.panache.stateless.reactive.PanacheStatelessReactiveRepositoryBase;

public interface PanacheRepositorySwitcher<Entity, Id> {
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
