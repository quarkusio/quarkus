package io.quarkus.it.hibernate.compatibility;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity(name = "myentity_gengendefallocsize")
public class MyEntityWithGenericGeneratorAndDefaultAllocationSize {
    @Id
    @GeneratedValue(generator = "gengendefallocsize")
    @GenericGenerator(name = "gengendefallocsize", strategy = "sequence")
    public Long id;

}
