package io.quarkus.data.hibernate.deployment.test.security.entities;

import jakarta.persistence.Entity;

import io.quarkus.data.hibernate.PanacheEntity;
import io.quarkus.data.hibernate.PanacheRepository;

@Entity
public class StandaloneRepoEntity extends PanacheEntity {

    public String name;

    public interface InnerPanacheRepository extends PanacheRepository<StandaloneRepoEntity> {
    }
}
