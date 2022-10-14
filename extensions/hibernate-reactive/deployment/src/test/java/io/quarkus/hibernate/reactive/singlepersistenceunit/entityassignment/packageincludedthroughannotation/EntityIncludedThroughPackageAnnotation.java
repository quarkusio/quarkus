package io.quarkus.hibernate.reactive.singlepersistenceunit.entityassignment.packageincludedthroughannotation;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public class EntityIncludedThroughPackageAnnotation {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "includedSeq")
    public long id;

    public String name;

    public EntityIncludedThroughPackageAnnotation() {
    }

    public EntityIncludedThroughPackageAnnotation(String name) {
        this.name = name;
    }

}
