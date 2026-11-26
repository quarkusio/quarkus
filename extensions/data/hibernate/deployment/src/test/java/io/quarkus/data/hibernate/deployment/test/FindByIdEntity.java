package io.quarkus.data.hibernate.deployment.test;

import jakarta.persistence.Entity;

import io.quarkus.data.hibernate.ManagedEntity;
import io.quarkus.data.hibernate.ManagedRepository;
import io.quarkus.data.hibernate.RecordRepository;

@Entity
public class FindByIdEntity extends ManagedEntity {
    public String foo;

    interface ManagedBlockingRepo extends ManagedRepository<FindByIdEntity> {
    }

    interface StatelessBlockingRepo extends RecordRepository<FindByIdEntity> {
    }

    interface ManagedReactiveRepo extends ManagedRepository.Reactive<FindByIdEntity> {
    }

    interface StatelessReactiveRepo extends RecordRepository.Reactive<FindByIdEntity> {
    }
}
