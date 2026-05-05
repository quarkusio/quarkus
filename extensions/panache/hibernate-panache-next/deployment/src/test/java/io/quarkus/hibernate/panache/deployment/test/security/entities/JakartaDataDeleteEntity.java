package io.quarkus.hibernate.panache.deployment.test.security.entities;

import jakarta.data.repository.Delete;
import jakarta.persistence.Entity;

import io.quarkus.hibernate.panache.PanacheEntity;
import io.quarkus.hibernate.panache.managed.reactive.PanacheManagedReactiveRepository;
import io.quarkus.security.Authenticated;
import io.smallrye.mutiny.Uni;

@Entity
public class JakartaDataDeleteEntity extends PanacheEntity {

    public String name;

    public interface MethodSecured {
        @Authenticated
        @Delete
        Uni<Integer> deleteByName(String name);
    }

    @Authenticated
    public interface ClassSecured {
        @Delete
        Uni<Integer> deleteByName(String name);
    }

    public interface InnerPanacheRepository extends PanacheManagedReactiveRepository<JakartaDataDeleteEntity> {
    }
}
