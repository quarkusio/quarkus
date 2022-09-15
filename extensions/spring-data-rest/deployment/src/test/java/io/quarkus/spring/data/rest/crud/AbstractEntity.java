package io.quarkus.spring.data.rest.crud;

import java.io.Serializable;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class AbstractEntity<IdType extends Serializable> {

    @Id
    @GeneratedValue
    private IdType id;

    public IdType getId() {
        return id;
    }
}
