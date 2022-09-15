package io.quarkus.hibernate.orm.rest.data.panache.deployment.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

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
