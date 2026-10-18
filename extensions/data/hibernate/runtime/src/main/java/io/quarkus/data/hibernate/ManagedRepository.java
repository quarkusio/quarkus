package io.quarkus.data.hibernate;

import io.quarkus.data.hibernate.managed.blocking.BlockingManagedRepositoryBase;
import io.quarkus.data.hibernate.managed.reactive.ReactiveManagedRepositoryBase;

public interface ManagedRepository<Entity> extends BlockingManagedRepositoryBase<Entity, Long> {

    interface CustomId<Entity, Id> extends BlockingManagedRepositoryBase<Entity, Id> {
    }

    interface Reactive<Entity> extends ReactiveManagedRepositoryBase<Entity, Long> {
        interface CustomId<Entity, Id> extends ReactiveManagedRepositoryBase<Entity, Id> {
        }
    }
}
