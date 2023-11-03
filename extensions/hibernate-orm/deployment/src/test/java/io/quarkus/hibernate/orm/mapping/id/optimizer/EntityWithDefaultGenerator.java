package io.quarkus.hibernate.orm.mapping.id.optimizer;

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
