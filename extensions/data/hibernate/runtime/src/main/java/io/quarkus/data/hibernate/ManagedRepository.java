package io.quarkus.data.hibernate;

import io.quarkus.data.hibernate.managed.blocking.PanacheManagedBlockingRepositoryBase;
import io.quarkus.data.hibernate.managed.reactive.PanacheManagedReactiveRepositoryBase;

public interface ManagedRepository<Entity, Id> extends PanacheManagedBlockingRepositoryBase<Entity, Id> {

    interface AutoLong<Entity> extends ManagedRepository<Entity, Long> {
    }

    interface Reactive<Entity, Id> extends PanacheManagedReactiveRepositoryBase<Entity, Id> {
    }
}
