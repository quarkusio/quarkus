package io.quarkus.hibernate.panache.deployment.test.security.entities;

import jakarta.data.repository.Update;
import jakarta.persistence.Entity;

import io.quarkus.hibernate.panache.PanacheEntity;
import io.quarkus.hibernate.panache.PanacheRepository;
import io.quarkus.security.Authenticated;

@Entity
public class JakartaDataUpdateEntity extends PanacheEntity {

    public String name;

    public interface MethodSecured extends PanacheRepository.Stateless<JakartaDataUpdateEntity, Long> {
        @Authenticated
        @Update
        void myUpdate(JakartaDataUpdateEntity entity);
    }

    @Authenticated
    public interface ClassSecured extends PanacheRepository.Stateless<JakartaDataUpdateEntity, Long> {
        @Update
        void myUpdate(JakartaDataUpdateEntity entity);
    }

    public interface InnerPanacheRepository extends PanacheRepository<JakartaDataUpdateEntity> {

    }
}
