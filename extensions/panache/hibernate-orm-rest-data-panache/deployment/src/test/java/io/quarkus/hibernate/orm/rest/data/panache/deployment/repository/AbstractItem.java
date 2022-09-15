package io.quarkus.hibernate.orm.rest.data.panache.deployment.repository;

import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;

@MappedSuperclass
public abstract class AbstractItem<IdType extends Number> extends AbstractEntity<IdType> {

    private String name;

    @ManyToOne(optional = false)
    @JsonProperty(access = Access.WRITE_ONLY)
    private Collection collection;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
