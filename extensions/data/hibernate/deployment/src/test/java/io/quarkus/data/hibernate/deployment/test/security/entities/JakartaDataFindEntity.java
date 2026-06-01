package io.quarkus.data.hibernate.deployment.test.security.entities;

import java.util.List;

import jakarta.data.repository.Find;
import jakarta.persistence.Entity;

import io.quarkus.data.hibernate.PanacheEntity;
import io.quarkus.data.hibernate.PanacheRepository;
import io.quarkus.security.Authenticated;

@Entity
public class JakartaDataFindEntity extends PanacheEntity {

    public String name;

    public interface MethodSecured {
        @Authenticated
        @Find
        List<JakartaDataFindEntity> findByName(String name);
    }

    @Authenticated
    public interface ClassSecured {
        @Find
        List<JakartaDataFindEntity> findByName(String name);
    }

    public interface InnerPanacheRepository extends PanacheRepository<JakartaDataFindEntity> {
    }
}
