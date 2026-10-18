package io.quarkus.hibernate.reactive.mapping.id.optimizer.optimizer;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@Entity
public class EntityWithDefaultGenerator {

    @Id
    @GeneratedValue
    Long id;

    public EntityWithDefaultGenerator() {
    }

}
