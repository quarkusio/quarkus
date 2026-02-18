package io.quarkus.hibernate.panache;

import io.quarkus.hibernate.panache.managed.blocking.PanacheManagedBlockingEntity;
import io.quarkus.hibernate.panache.managed.reactive.PanacheManagedReactiveEntity;
import io.quarkus.hibernate.panache.stateless.blocking.PanacheStatelessBlockingEntity;
import io.quarkus.hibernate.panache.stateless.reactive.PanacheStatelessReactiveEntity;

/**
 * This is just an alias for {@link PanacheManagedBlockingEntity} and {@link WithId.AutoLong}
 * for people coming from Panache 1
 */
public abstract class PanacheEntity<Entity extends PanacheEntityMarker<Entity>> extends WithId.AutoLong
        implements PanacheManagedBlockingEntity<Entity> {

    /**
     * Represents an entity with managed blocking operations.
     */
    public interface Managed<Entity extends PanacheEntityMarker<Entity>> extends PanacheManagedBlockingEntity<Entity> {
    }

    /**
     * Represents an entity with stateless blocking operations.
     */
    public interface Stateless<Entity extends PanacheEntityMarker<Entity>> extends PanacheStatelessBlockingEntity<Entity> {
    }

    /**
     * Represents an entity with managed reactive operations.
     */
    public interface Reactive<Entity extends PanacheEntityMarker<Entity>> extends PanacheManagedReactiveEntity<Entity> {
        /**
         * Represents an entity with stateless reactive operations.
         */
        public interface Stateless<Entity extends PanacheEntityMarker<Entity>> extends PanacheStatelessReactiveEntity<Entity> {
        }
    }
}
