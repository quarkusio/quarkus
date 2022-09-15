package io.quarkus.it.panache;

import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

@MappedSuperclass
public class Bug7721EntitySuperClass extends PanacheEntity {

    @Column(nullable = false)
    public String superField = "default";

    public void setSuperField(String superField) {
        Objects.requireNonNull(superField);
        // should never be null
        Objects.requireNonNull(this.superField);
        this.superField = superField;
    }

}
