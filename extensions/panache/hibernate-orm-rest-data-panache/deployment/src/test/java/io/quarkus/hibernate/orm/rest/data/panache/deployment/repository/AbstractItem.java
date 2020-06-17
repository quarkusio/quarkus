package io.quarkus.hibernate.orm.rest.data.panache.deployment.repository;

import javax.json.bind.annotation.JsonbTransient;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class AbstractItem<IdType> {

    @Id
    @GeneratedValue
    private IdType id;

    private String name;

    @ManyToOne(optional = false)
    private Collection collection;

    public IdType getId() {
        return id;
    }

    public void setId(IdType id) {
        this.id = id;
    }

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
