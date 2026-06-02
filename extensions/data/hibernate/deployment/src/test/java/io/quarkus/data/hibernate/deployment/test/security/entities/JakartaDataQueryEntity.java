package io.quarkus.data.hibernate.deployment.test.security.entities;

import java.util.List;

import jakarta.data.repository.Query;
import jakarta.persistence.Entity;

import io.quarkus.data.hibernate.ManagedEntity;
import io.quarkus.data.hibernate.ManagedRepository;
import io.quarkus.security.Authenticated;

@Entity
public class JakartaDataQueryEntity extends ManagedEntity.AutoLong {

    public String name;

    public interface MethodSecured {
        @Authenticated
        @Query("from JakartaDataQueryEntity where name = :name")
        List<JakartaDataQueryEntity> findByName(String name);
    }

    @Authenticated
    public interface ClassSecured {
        @Query("from JakartaDataQueryEntity where name = :name")
        List<JakartaDataQueryEntity> findByName(String name);
    }

    public interface InnerPanacheRepository extends ManagedRepository.AutoLong<JakartaDataQueryEntity> {
    }
}
