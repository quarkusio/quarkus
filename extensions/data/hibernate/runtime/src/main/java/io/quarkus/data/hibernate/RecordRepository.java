package io.quarkus.data.hibernate;

import io.quarkus.data.hibernate.stateless.blocking.PanacheStatelessBlockingRepositoryBase;
import io.quarkus.data.hibernate.stateless.reactive.PanacheStatelessReactiveRepositoryBase;

public interface RecordRepository<Entity, Id> extends PanacheStatelessBlockingRepositoryBase<Entity, Id> {

    interface AutoLong<Entity> extends RecordRepository<Entity, Long> {
    }

    interface Reactive<Entity, Id> extends PanacheStatelessReactiveRepositoryBase<Entity, Id> {
    }
}
