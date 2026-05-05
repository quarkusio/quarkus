package io.quarkus.hibernate.panache.deployment.test.security.entities;

import jakarta.data.repository.Insert;
import jakarta.persistence.Entity;

import io.quarkus.hibernate.panache.PanacheEntity;
import io.quarkus.hibernate.panache.PanacheRepository;
import io.quarkus.security.Authenticated;

@Entity
public class JakartaDataInsertEntity extends PanacheEntity {

    public String name;

    public interface MethodSecured extends PanacheRepository.Stateless<JakartaDataInsertEntity, Long> {
        @Authenticated
        @Insert
        void myInsert(JakartaDataInsertEntity entity);
    }

    @Authenticated
    public interface ClassSecured extends PanacheRepository.Stateless<JakartaDataInsertEntity, Long> {
        @Insert
        void myInsert(JakartaDataInsertEntity entity);
    }

    public interface InnerPanacheRepository extends PanacheRepository<JakartaDataInsertEntity> {
    }
}
