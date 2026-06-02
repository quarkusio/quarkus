package io.quarkus.data.hibernate.deployment.test.security.entities;

import jakarta.data.repository.Insert;
import jakarta.persistence.Entity;

import io.quarkus.data.hibernate.ManagedEntity;
import io.quarkus.data.hibernate.ManagedRepository;
import io.quarkus.data.hibernate.RecordRepository;
import io.quarkus.security.Authenticated;

@Entity
public class JakartaDataInsertEntity extends ManagedEntity.AutoLong {

    public String name;

    public interface MethodSecured extends RecordRepository<JakartaDataInsertEntity, Long> {
        @Authenticated
        @Insert
        void myInsert(JakartaDataInsertEntity entity);
    }

    @Authenticated
    public interface ClassSecured extends RecordRepository<JakartaDataInsertEntity, Long> {
        @Insert
        void myInsert(JakartaDataInsertEntity entity);
    }

    public interface InnerPanacheRepository extends ManagedRepository.AutoLong<JakartaDataInsertEntity> {
    }
}
