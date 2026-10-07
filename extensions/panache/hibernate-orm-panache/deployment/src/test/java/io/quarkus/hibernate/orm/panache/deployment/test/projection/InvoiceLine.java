package io.quarkus.hibernate.orm.panache.deployment.test.projection;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

@Entity
public class InvoiceLine extends PanacheEntity {

    public String description;

    @ManyToOne
    public Invoice invoice;
}
