package io.quarkus.hibernate.orm.multiplepersistenceunits.model.annotation.override;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import io.quarkus.hibernate.orm.PersistenceUnit;

// This class lives in a package mapped to the "inventory" persistence unit through the
// package-level @PersistenceUnit annotation declared in package-info.java,
// but it carries its own class-level @PersistenceUnit annotation pointing to "users".
// The class-level annotation is expected to take precedence over the package-level one.
@Entity
@PersistenceUnit("users")
public class OverriddenEntity {

    private long id;

    private String name;

    public OverriddenEntity() {
    }

    public OverriddenEntity(String name) {
        this.name = name;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "overriddenEntitySeq")
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
}
