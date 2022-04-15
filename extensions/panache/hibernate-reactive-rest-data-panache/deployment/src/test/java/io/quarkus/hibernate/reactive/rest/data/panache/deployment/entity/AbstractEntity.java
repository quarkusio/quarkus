package io.quarkus.hibernate.reactive.rest.data.panache.deployment.entity;

import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

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
