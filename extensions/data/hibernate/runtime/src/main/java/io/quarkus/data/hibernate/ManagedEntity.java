package io.quarkus.data.hibernate;

import io.quarkus.data.hibernate.managed.blocking.PanacheManagedBlockingEntity;
import io.quarkus.data.hibernate.managed.reactive.PanacheManagedReactiveEntity;

/**
 * Represents an entity with managed blocking operations.
 */
public interface ManagedEntity extends PanacheManagedBlockingEntity {

    class AutoLong extends WithId.AutoLong implements ManagedEntity {
    }

    /**
     * Represents an entity with managed reactive operations.
     */
    interface Reactive extends PanacheManagedReactiveEntity {

    }
}