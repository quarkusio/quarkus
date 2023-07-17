package io.quarkus.hibernate.orm.singlepersistenceunit.entityassignment.packageincludedthroughconfig;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

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
