package io.quarkus.hibernate.orm.spi.pkgscan;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import io.quarkus.hibernate.orm.PersistenceUnit;

/**
 * An entity living in the same package as {@link PackageScannedEntity}, but explicitly assigned to a
 * persistence unit of its own through a class-level {@link PersistenceUnit} annotation.
 */
@Entity
@PersistenceUnit("someUserPU")
public class UserPersistenceUnitEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    public UserPersistenceUnitEntity() {
    }

    public UserPersistenceUnitEntity(String name) {
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
