package io.quarkus.hibernate.reactive.rest.data.panache.deployment.repository;

import javax.json.bind.annotation.JsonbTransient;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;

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
