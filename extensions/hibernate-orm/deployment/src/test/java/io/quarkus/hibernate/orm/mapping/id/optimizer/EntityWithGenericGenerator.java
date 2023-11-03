package io.quarkus.hibernate.orm.mapping.id.optimizer;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.id.enhanced.SequenceStyleGenerator;

@Entity
public class EntityWithGenericGenerator {

    @Id
    @GeneratedValue(generator = "gen_gen")
    @GenericGenerator(name = "gen_gen", type = SequenceStyleGenerator.class)
    Long id;

    public EntityWithGenericGenerator() {
    }

}
