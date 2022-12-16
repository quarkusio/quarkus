package io.quarkus.hibernate.orm.rest.data.panache.deployment.repository;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;

@Entity
public class EmptyListItem extends PanacheEntityBase {

    @Id
    @GeneratedValue
    private Long cid;

    public String name;

    @ManyToOne(optional = false)
    @JsonProperty(access = Access.WRITE_ONLY)
    public Collection collection;
}
