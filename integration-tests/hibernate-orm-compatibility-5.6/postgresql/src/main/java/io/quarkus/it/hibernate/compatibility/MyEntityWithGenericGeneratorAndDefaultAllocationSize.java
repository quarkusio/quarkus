package io.quarkus.it.hibernate.compatibility;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import org.hibernate.annotations.GenericGenerator;

@Entity(name = "myentity_gengendefallocsize")
public class MyEntityWithGenericGeneratorAndDefaultAllocationSize {
    @Id
    @GeneratedValue(generator = "gengendefallocsize")
    @GenericGenerator(name = "gengendefallocsize", strategy = "sequence")
    public Long id;

}
