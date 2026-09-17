package io.quarkus.hibernate.orm.multitenancy.discriminator;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import org.hibernate.annotations.TenantId;

@Entity
public class Product {

    @Id
    @GeneratedValue
    private Long id;

    // A non-String tenant identifier: the whole point of this test.
    @TenantId
    private Long tenantId;

    private String name;

    public Product() {
    }

    public Product(String name) {
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public String getName() {
        return name;
    }
}
