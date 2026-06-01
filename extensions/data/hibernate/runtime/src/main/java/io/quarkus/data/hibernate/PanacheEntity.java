package io.quarkus.data.hibernate;

import io.quarkus.data.hibernate.managed.blocking.PanacheManagedBlockingEntity;
import io.quarkus.data.hibernate.managed.reactive.PanacheManagedReactiveEntity;
import io.quarkus.data.hibernate.stateless.blocking.PanacheStatelessBlockingEntity;
import io.quarkus.data.hibernate.stateless.reactive.PanacheStatelessReactiveEntity;

/**
 * This is just an alias for {@link PanacheManagedBlockingEntity} and {@link WithId.AutoLong}
 * for people coming from Panache 1
 */
public abstract class PanacheEntity extends WithId.AutoLong implements PanacheManagedBlockingEntity {

    /**
     * Represents an entity with managed blocking operations.
     */
    public interface Managed extends PanacheManagedBlockingEntity {
    }

    /**
     * Represents an entity with stateless blocking operations.
     */
    public interface Stateless extends PanacheStatelessBlockingEntity {
    }

    /**
     * Represents an entity with managed reactive operations.
     */
    public interface Reactive extends PanacheManagedReactiveEntity {
        /**
         * Represents an entity with stateless reactive operations.
         */
        public interface Stateless extends PanacheStatelessReactiveEntity {
        }
    }
}
