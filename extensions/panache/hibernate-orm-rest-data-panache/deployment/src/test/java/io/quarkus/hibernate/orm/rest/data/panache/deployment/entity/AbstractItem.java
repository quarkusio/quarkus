package io.quarkus.hibernate.orm.rest.data.panache.deployment.entity;

import javax.json.bind.annotation.JsonbTransient;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class AbstractItem<IdType extends Number> extends AbstractEntity<IdType> {

    public String name;

    @ManyToOne(optional = false)
    public Collection collection;

    @JsonbTransient // Avoid infinite loop when serializing
    public Collection getCollection() {
        return collection;
    }
}
