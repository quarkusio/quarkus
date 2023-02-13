package io.quarkus.hibernate.orm.singlepersistenceunit.entityassignment.packageincludedthroughannotation;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

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
