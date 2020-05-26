package io.quarkus.hibernate.orm.rest.data.panache.deployment.entity;

import javax.json.bind.annotation.JsonbTransient;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

@Entity
public class Item extends PanacheEntity {

    public String name;

    @ManyToOne(optional = false)
    public Collection collection;

    @JsonbTransient // Avoid infinite loop when serializing
    public Collection getCollection() {
        return collection;
    }
}
