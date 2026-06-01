package io.quarkus.data.hibernate;

import io.quarkus.data.hibernate.managed.blocking.PanacheManagedBlockingRepositoryBase;
import io.quarkus.data.hibernate.managed.reactive.PanacheManagedReactiveRepositoryBase;
import io.quarkus.data.hibernate.stateless.blocking.PanacheStatelessBlockingRepositoryBase;
import io.quarkus.data.hibernate.stateless.reactive.PanacheStatelessReactiveRepositoryBase;

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
