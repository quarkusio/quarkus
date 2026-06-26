package io.quarkus.data.hibernate;

import io.quarkus.data.hibernate.managed.blocking.PanacheManagedBlockingEntity;
import io.quarkus.data.hibernate.managed.reactive.PanacheManagedReactiveEntity;

/**
 * Represents an entity with managed blocking operations.
 */
public class ManagedEntity extends WithId.AutoLong implements PanacheManagedBlockingEntity {

    public interface CustomId extends PanacheManagedBlockingEntity {
    }

    /**
     * Represents an entity with managed reactive operations.
     */
    public static class Reactive extends WithId.AutoLong implements PanacheManagedReactiveEntity {
        public interface CustomId extends PanacheManagedReactiveEntity {
        }
    }
}