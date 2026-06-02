package io.quarkus.data.hibernate.deployment.test.security.entities;

import jakarta.persistence.Entity;

import io.quarkus.data.hibernate.ManagedEntity;
import io.quarkus.data.hibernate.ManagedRepository;

@Entity
public class StandaloneRepoEntity extends ManagedEntity {

    public String name;

    public interface InnerPanacheRepository extends ManagedRepository<StandaloneRepoEntity> {
    }
}
