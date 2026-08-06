package io.quarkus.data.hibernate.deployment;

import java.io.IOException;

import jakarta.persistence.Entity;

import org.jboss.jandex.IndexView;
import org.jboss.jandex.Indexer;

import io.quarkus.data.hibernate.ManagedEntity;
import io.quarkus.data.hibernate.managed.blocking.BlockingManagedEntity;
import io.quarkus.data.hibernate.managed.reactive.ReactiveManagedEntity;
import io.quarkus.data.hibernate.managed.reactive.ReactiveManagedRepositoryBase;
import io.quarkus.data.hibernate.stateless.reactive.ReactiveRecordEntity;
import io.quarkus.data.hibernate.stateless.reactive.ReactiveRecordRepositoryBase;

final class ReactiveQuarkusDataValidatorTestHelper {

    private ReactiveQuarkusDataValidatorTestHelper() {
    }

    static IndexView indexOf(Class<?>... classes) {
        try {
            Indexer indexer = new Indexer();
            indexer.indexClass(BlockingManagedEntity.class);
            indexer.indexClass(ReactiveManagedEntity.class);
            indexer.indexClass(ReactiveRecordEntity.class);
            indexer.indexClass(ReactiveManagedRepositoryBase.class);
            indexer.indexClass(ReactiveRecordRepositoryBase.class);
            indexer.indexClass(ManagedEntity.class);
            indexer.indexClass(ManagedEntity.Reactive.class);
            indexer.indexClass(Entity.class);
            for (Class<?> clazz : classes) {
                indexer.indexClass(clazz);
            }
            return indexer.complete();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
