package io.quarkus.hibernate.reactive.rest.data.panache.deployment.repository;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class AbstractEntity<IdType extends Number> {

    @Id
    @GeneratedValue
    private IdType id;

    public IdType getId() {
        return id;
    }
}
