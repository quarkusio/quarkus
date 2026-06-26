package io.quarkus.data.hibernate.deployment.test.security.entities;

import jakarta.data.repository.Update;
import jakarta.persistence.Entity;

import io.quarkus.data.hibernate.ManagedEntity;
import io.quarkus.data.hibernate.ManagedRepository;
import io.quarkus.data.hibernate.RecordRepository;
import io.quarkus.security.Authenticated;

@Entity
public class JakartaDataUpdateEntity extends ManagedEntity {

    public String name;

    public interface MethodSecured extends RecordRepository<JakartaDataUpdateEntity> {
        @Authenticated
        @Update
        void myUpdate(JakartaDataUpdateEntity entity);
    }

    @Authenticated
    public interface ClassSecured extends RecordRepository<JakartaDataUpdateEntity> {
        @Update
        void myUpdate(JakartaDataUpdateEntity entity);
    }

    public interface InnerPanacheRepository extends ManagedRepository<JakartaDataUpdateEntity> {

    }
}
