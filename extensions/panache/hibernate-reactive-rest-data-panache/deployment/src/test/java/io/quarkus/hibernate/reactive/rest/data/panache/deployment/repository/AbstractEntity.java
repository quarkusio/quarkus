package io.quarkus.hibernate.reactive.rest.data.panache.deployment.repository;

import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class AbstractEntity<IdType extends Number> {

    @Id
    @GeneratedValue
    private IdType id;

    public IdType getId() {
        return id;
    }
}
