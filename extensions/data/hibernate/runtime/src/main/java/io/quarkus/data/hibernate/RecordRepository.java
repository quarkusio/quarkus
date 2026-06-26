package io.quarkus.data.hibernate;

import io.quarkus.data.hibernate.stateless.blocking.PanacheStatelessBlockingRepositoryBase;
import io.quarkus.data.hibernate.stateless.reactive.PanacheStatelessReactiveRepositoryBase;

public interface RecordRepository<Entity> extends PanacheStatelessBlockingRepositoryBase<Entity, Long> {

    interface CustomId<Entity, Id> extends PanacheStatelessBlockingRepositoryBase<Entity, Id> {
    }

    interface Reactive<Entity> extends PanacheStatelessReactiveRepositoryBase<Entity, Long> {
        interface CustomId<Entity, Id> extends PanacheStatelessReactiveRepositoryBase<Entity, Id> {
        }
    }
}
