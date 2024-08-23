package io.quarkus.hibernate.reactive.mapping.id.optimizer.optimizer;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;

@Entity
public class EntityWithSequenceGenerator {

    @Id
    @GeneratedValue(generator = "seq_gen")
    @SequenceGenerator(name = "seq_gen")
    Long id;

    public EntityWithSequenceGenerator() {
    }

}
