package io.quarkus.hibernate.panache.deployment.test.security.entities;

import jakarta.persistence.Entity;

import io.quarkus.hibernate.panache.PanacheEntity;
import io.quarkus.hibernate.panache.PanacheRepository;

@Entity
public class StandaloneRepoEntity extends PanacheEntity {

    public String name;

    public interface InnerPanacheRepository extends PanacheRepository<StandaloneRepoEntity> {
    }
}
