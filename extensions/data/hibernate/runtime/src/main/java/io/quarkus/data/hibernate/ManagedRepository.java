package io.quarkus.data.hibernate;

import io.quarkus.data.hibernate.managed.blocking.PanacheManagedBlockingRepositoryBase;
import io.quarkus.data.hibernate.managed.reactive.PanacheManagedReactiveRepositoryBase;

public interface ManagedRepository<Entity> extends PanacheManagedBlockingRepositoryBase<Entity, Long> {

    interface CustomId<Entity, Id> extends PanacheManagedBlockingRepositoryBase<Entity, Id> {
    }

    interface Reactive<Entity> extends PanacheManagedReactiveRepositoryBase<Entity, Long> {
        interface CustomId<Entity, Id> extends PanacheManagedReactiveRepositoryBase<Entity, Id> {
        }
    }
}
