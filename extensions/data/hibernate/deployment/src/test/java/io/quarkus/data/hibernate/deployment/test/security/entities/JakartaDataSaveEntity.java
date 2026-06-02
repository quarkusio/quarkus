package io.quarkus.data.hibernate.deployment.test.security.entities;

import jakarta.data.repository.Save;
import jakarta.persistence.Entity;

import io.quarkus.data.hibernate.ManagedEntity;
import io.quarkus.data.hibernate.ManagedRepository;
import io.quarkus.data.hibernate.RecordRepository;
import io.quarkus.security.Authenticated;

@Entity
public class JakartaDataSaveEntity extends ManagedEntity.AutoLong {

    public String name;

    public interface MethodSecured extends RecordRepository<JakartaDataSaveEntity, Long> {
        @Authenticated
        @Save
        void mySave(JakartaDataSaveEntity entity);
    }

    @Authenticated
    public interface ClassSecured extends RecordRepository<JakartaDataSaveEntity, Long> {
        @Save
        void mySave(JakartaDataSaveEntity entity);
    }

    public interface InnerPanacheRepository extends ManagedRepository.AutoLong<JakartaDataSaveEntity> {
    }
}
