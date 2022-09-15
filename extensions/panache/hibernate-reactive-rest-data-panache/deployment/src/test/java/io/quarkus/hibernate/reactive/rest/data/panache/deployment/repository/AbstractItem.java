package io.quarkus.hibernate.reactive.rest.data.panache.deployment.repository;

import jakarta.json.bind.annotation.JsonbTransient;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class AbstractItem<IdType extends Number> extends AbstractEntity<IdType> {

    private String name;

    @ManyToOne
    private Collection collection;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @JsonbTransient // Avoid infinite loop when serialising
    public Collection getCollection() {
        return collection;
    }

    public void setCollection(Collection collection) {
        this.collection = collection;
    }
}
