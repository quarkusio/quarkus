package io.quarkus.data.hibernate.deployment.test.security.entities;

import java.util.List;

import jakarta.data.repository.Find;
import jakarta.persistence.Entity;

import io.quarkus.data.hibernate.ManagedEntity;
import io.quarkus.data.hibernate.ManagedRepository;
import io.quarkus.security.Authenticated;

@Entity
public class JakartaDataFindEntity extends ManagedEntity.AutoLong {

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

    public interface InnerPanacheRepository extends ManagedRepository.AutoLong<JakartaDataFindEntity> {
    }
}
