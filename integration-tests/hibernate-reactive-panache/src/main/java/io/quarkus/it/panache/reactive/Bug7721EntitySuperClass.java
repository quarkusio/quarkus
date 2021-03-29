package io.quarkus.it.panache.reactive;

import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;

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
