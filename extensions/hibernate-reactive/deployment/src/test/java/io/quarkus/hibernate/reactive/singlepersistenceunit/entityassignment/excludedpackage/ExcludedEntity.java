package io.quarkus.hibernate.reactive.singlepersistenceunit.entityassignment.excludedpackage;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public class ExcludedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "excludedSeq")
    public long id;

    public String name;

    public ExcludedEntity() {
    }

    public ExcludedEntity(String name) {
        this.name = name;
    }

}
