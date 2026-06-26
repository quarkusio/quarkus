package io.quarkus.data.hibernate;

import io.quarkus.data.hibernate.stateless.blocking.PanacheStatelessBlockingEntity;
import io.quarkus.data.hibernate.stateless.reactive.PanacheStatelessReactiveEntity;

/**
 * Represents an entity with stateless blocking operations.
 */
public class RecordEntity extends WithId.AutoLong implements PanacheStatelessBlockingEntity {

    public interface CustomId extends PanacheStatelessBlockingEntity {
    }

    public static class Reactive extends WithId.AutoLong implements PanacheStatelessReactiveEntity {
        public interface CustomId extends PanacheStatelessReactiveEntity {
        }
    }
}
