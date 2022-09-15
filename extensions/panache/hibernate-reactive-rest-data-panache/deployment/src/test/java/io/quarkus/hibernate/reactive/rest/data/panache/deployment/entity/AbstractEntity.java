package io.quarkus.hibernate.reactive.rest.data.panache.deployment.entity;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;

@MappedSuperclass
public abstract class AbstractEntity<IdType extends Number> extends PanacheEntityBase {

    @Id
    @GeneratedValue
    private IdType id;

    public IdType getId() {
        return id;
    }

    public void setId(IdType id) {
        this.id = id;
    }
}
