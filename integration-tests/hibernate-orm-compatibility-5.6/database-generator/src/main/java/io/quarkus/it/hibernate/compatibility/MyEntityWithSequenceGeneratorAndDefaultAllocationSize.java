package io.quarkus.it.hibernate.compatibility;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;

@Entity(name = "myentity_seqgendefallocsize")
public class MyEntityWithSequenceGeneratorAndDefaultAllocationSize {
    @Id
    @GeneratedValue(generator = "seqgendefallocsize")
    @SequenceGenerator(name = "seqgendefallocsize")
    public Long id;

}
