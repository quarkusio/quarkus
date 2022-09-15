package io.quarkus.hibernate.orm.singlepersistenceunit.entityassignment.excludedpackage;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

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
