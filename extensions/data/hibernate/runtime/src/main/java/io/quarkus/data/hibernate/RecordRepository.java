package io.quarkus.data.hibernate;

import io.quarkus.data.hibernate.record.blocking.BlockingRecordRepositoryBase;
import io.quarkus.data.hibernate.record.reactive.ReactiveRecordRepositoryBase;

public interface RecordRepository<Entity> extends BlockingRecordRepositoryBase<Entity, Long> {

    interface CustomId<Entity, Id> extends BlockingRecordRepositoryBase<Entity, Id> {
    }

    interface Reactive<Entity> extends ReactiveRecordRepositoryBase<Entity, Long> {
        interface CustomId<Entity, Id> extends ReactiveRecordRepositoryBase<Entity, Id> {
        }
    }
}
