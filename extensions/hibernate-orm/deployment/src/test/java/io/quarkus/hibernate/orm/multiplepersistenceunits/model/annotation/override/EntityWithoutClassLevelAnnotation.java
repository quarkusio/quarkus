package io.quarkus.hibernate.orm.multiplepersistenceunits.model.annotation.override;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

// This class lives in the same package as OverriddenEntity, mapped to the "inventory"
// persistence unit through the package-level @PersistenceUnit annotation, but carries no
// class-level annotation of its own: the package-level assignment must still apply to it.
@Entity
public class EntityWithoutClassLevelAnnotation {

    private long id;

    private String name;

    public EntityWithoutClassLevelAnnotation() {
    }

    public EntityWithoutClassLevelAnnotation(String name) {
        this.name = name;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "entityWithoutClassLevelAnnotationSeq")
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
