package io.quarkus.hibernate.orm.rest.data.panache.deployment.entity;

import javax.json.bind.annotation.JsonbTransient;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;

@Entity
public class EmptyListItem extends PanacheEntityBase {

    @Id
    @GeneratedValue
    private Long cid;

    public String name;

    @ManyToOne(optional = false)
    public Collection collection;

    public Long getCid() {
        return cid;
    }

    public void setCid(Long cid) {
        this.cid = cid;
    }

    @JsonbTransient // Avoid infinite loop when serializing
    public Collection getCollection() {
        return collection;
    }

}
