package io.quarkus.hibernate.panache;

import io.quarkus.hibernate.panache.managed.blocking.PanacheManagedBlockingRepositoryBase;
import io.quarkus.hibernate.panache.managed.reactive.PanacheManagedReactiveRepositoryBase;
import io.quarkus.hibernate.panache.stateless.blocking.PanacheStatelessBlockingRepositoryBase;
import io.quarkus.hibernate.panache.stateless.reactive.PanacheStatelessReactiveRepositoryBase;

public interface PanacheRepository<Entity> extends PanacheManagedBlockingRepositoryBase<Entity, Long> {
    public interface Managed<Entity, Id> extends PanacheManagedBlockingRepositoryBase<Entity, Id> {
    }

    public interface Stateless<Entity, Id> extends PanacheStatelessBlockingRepositoryBase<Entity, Id> {
    }

    public interface Reactive<Entity, Id> extends PanacheManagedReactiveRepositoryBase<Entity, Id> {

        public interface Stateless<Entity, Id> extends PanacheStatelessReactiveRepositoryBase<Entity, Id> {
        }
    }
}
