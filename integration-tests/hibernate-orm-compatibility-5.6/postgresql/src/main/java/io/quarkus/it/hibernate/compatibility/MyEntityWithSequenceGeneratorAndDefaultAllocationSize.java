package io.quarkus.it.hibernate.compatibility;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;

@Entity(name = "myentity_seqgendefallocsize")
public class MyEntityWithSequenceGeneratorAndDefaultAllocationSize {
    @Id
    @GeneratedValue(generator = "seqgendefallocsize")
    @SequenceGenerator(name = "seqgendefallocsize")
    public Long id;

}
