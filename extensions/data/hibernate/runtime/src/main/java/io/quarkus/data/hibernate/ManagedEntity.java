package io.quarkus.data.hibernate;

import io.quarkus.data.hibernate.managed.blocking.BlockingManagedEntity;
import io.quarkus.data.hibernate.managed.reactive.ReactiveManagedEntity;

/**
 * Represents an entity with managed blocking operations.
 */
public class ManagedEntity extends WithId.AutoLong implements BlockingManagedEntity {

    public interface CustomId extends BlockingManagedEntity {
    }

    /**
     * Represents an entity with managed reactive operations.
     */
    public static class Reactive extends WithId.AutoLong implements ReactiveManagedEntity {
        public interface CustomId extends ReactiveManagedEntity {
        }
    }
}