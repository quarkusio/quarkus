package io.quarkus.spring.data.rest.paged;

import java.io.Serializable;

import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class AbstractEntity<IdType extends Serializable> {

    @Id
    @GeneratedValue
    private IdType id;

    public IdType getId() {
        return id;
    }
}
