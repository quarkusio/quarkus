package io.quarkus.hibernate.reactive.mapping.id.optimizer.optimizer;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.TableGenerator;

@Entity
public class EntityWithTableGenerator {

    @Id
    @GeneratedValue(generator = "tab_gen")
    @TableGenerator(name = "tab_gen")
    Long id;

    public EntityWithTableGenerator() {
    }

}
