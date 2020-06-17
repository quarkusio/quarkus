package io.quarkus.hibernate.orm.rest.data.panache.deployment.entity;

import javax.json.bind.annotation.JsonbTransient;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;

@MappedSuperclass
public abstract class AbstractItem<IdType> extends PanacheEntityBase {

    @Id
    @GeneratedValue
    public IdType id;

    public String name;

    @ManyToOne(optional = false)
    public Collection collection;

    @JsonbTransient // Avoid infinite loop when serializing
    public Collection getCollection() {
        return collection;
    }
}
