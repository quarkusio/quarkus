package io.quarkus.hibernate.reactive.singlepersistenceunit.entityassignment.packageincludedthroughconfig;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public class EntityIncludedThroughPackageConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "includedSeq")
    public long id;

    public String name;

    public EntityIncludedThroughPackageConfig() {
    }

    public EntityIncludedThroughPackageConfig(String name) {
        this.name = name;
    }

}
