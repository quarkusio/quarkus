package io.quarkus.hibernate.orm.panache.deployment.test.projection;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

@Entity
public class Invoice extends PanacheEntity {

    public String number;

    @OneToMany(mappedBy = "invoice")
    public Set<InvoiceLine> lines = new HashSet<>();
}
