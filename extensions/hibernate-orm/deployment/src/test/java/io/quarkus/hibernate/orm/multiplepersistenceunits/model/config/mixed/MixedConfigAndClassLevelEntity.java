package io.quarkus.hibernate.orm.multiplepersistenceunits.model.config.mixed;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import io.quarkus.hibernate.orm.PersistenceUnit;

// This entity belongs to a package assigned, through Quarkus configuration (.packages),
// to the "users" persistence unit. The class-level @PersistenceUnit("inventory") annotation
// below must be ignored, since mixing .packages configuration with @PersistenceUnit
// annotations (package-level or class-level) is not supported: .packages configuration wins.
@Entity
@PersistenceUnit("inventory")
public class MixedConfigAndClassLevelEntity {

    private long id;

    private String name;

    public MixedConfigAndClassLevelEntity() {
    }

    public MixedConfigAndClassLevelEntity(String name) {
        this.name = name;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "mixedSeq")
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "MixedConfigAndClassLevelEntity:" + name;
    }
}
