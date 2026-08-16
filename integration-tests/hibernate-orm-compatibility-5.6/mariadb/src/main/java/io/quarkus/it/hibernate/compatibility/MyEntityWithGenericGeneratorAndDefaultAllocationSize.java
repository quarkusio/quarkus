package io.quarkus.it.hibernate.compatibility;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.id.enhanced.SequenceStyleGenerator;

@Entity(name = "myentity_gengendefallocsize")
public class MyEntityWithGenericGeneratorAndDefaultAllocationSize {
    @Id
    @GeneratedValue
    @GenericGenerator(type = SequenceStyleGenerator.class)
    public Long id;

}
