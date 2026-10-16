package io.quarkus.data.hibernate.deployment.test.security.entities;

import jakarta.data.repository.Delete;
import jakarta.persistence.Entity;

import io.quarkus.data.hibernate.ManagedEntity;
import io.quarkus.data.hibernate.ManagedRepository;
import io.quarkus.security.Authenticated;
import io.smallrye.mutiny.Uni;

@Entity
public class JakartaDataDeleteEntity extends ManagedEntity {

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

    public interface InnerPanacheRepository extends ManagedRepository.Reactive<JakartaDataDeleteEntity> {
    }
}
