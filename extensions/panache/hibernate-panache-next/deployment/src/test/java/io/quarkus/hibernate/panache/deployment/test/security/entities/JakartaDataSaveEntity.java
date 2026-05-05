package io.quarkus.hibernate.panache.deployment.test.security.entities;

import jakarta.data.repository.Save;
import jakarta.persistence.Entity;

import io.quarkus.hibernate.panache.PanacheEntity;
import io.quarkus.hibernate.panache.PanacheRepository;
import io.quarkus.security.Authenticated;

@Entity
public class JakartaDataSaveEntity extends PanacheEntity {

    public String name;

    public interface MethodSecured extends PanacheRepository.Stateless<JakartaDataSaveEntity, Long> {
        @Authenticated
        @Save
        void mySave(JakartaDataSaveEntity entity);
    }

    @Authenticated
    public interface ClassSecured extends PanacheRepository.Stateless<JakartaDataSaveEntity, Long> {
        @Save
        void mySave(JakartaDataSaveEntity entity);
    }

    public interface InnerPanacheRepository extends PanacheRepository<JakartaDataSaveEntity> {
    }
}
