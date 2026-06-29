package io.quarkus.data.hibernate;

import io.quarkus.data.hibernate.stateless.blocking.BlockingRecordEntity;
import io.quarkus.data.hibernate.stateless.reactive.ReactiveRecordEntity;

/**
 * Represents an entity with stateless blocking operations.
 */
public class RecordEntity extends WithId.AutoLong implements BlockingRecordEntity {

    public interface CustomId extends BlockingRecordEntity {
    }

    public static class Reactive extends WithId.AutoLong implements ReactiveRecordEntity {
        public interface CustomId extends ReactiveRecordEntity {
        }
    }
}
