package io.quarkus.data.hibernate;

import io.quarkus.data.hibernate.stateless.blocking.PanacheStatelessBlockingEntity;
import io.quarkus.data.hibernate.stateless.reactive.PanacheStatelessReactiveEntity;

/**
 * Represents an entity with stateless blocking operations.
 */
public interface RecordEntity extends PanacheStatelessBlockingEntity {

    class AutoLong extends WithId.AutoLong implements RecordEntity {
    }

    interface Reactive extends PanacheStatelessReactiveEntity {
    }
}
