package io.quarkus.hibernate.panache.deployment.test.security.entities;

import java.util.List;

import jakarta.data.repository.Query;
import jakarta.persistence.Entity;

import io.quarkus.hibernate.panache.PanacheEntity;
import io.quarkus.hibernate.panache.PanacheRepository;
import io.quarkus.security.Authenticated;

@Entity
public class JakartaDataQueryEntity extends PanacheEntity {

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

    public interface InnerPanacheRepository extends PanacheRepository<JakartaDataQueryEntity> {
    }
}
