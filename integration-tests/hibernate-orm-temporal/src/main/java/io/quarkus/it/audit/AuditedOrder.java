package io.quarkus.it.audit;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.annotations.Audited;

@Audited
@Entity
public class AuditedOrder {

    @Id
    long id;

    String description;

    int quantity;

    public AuditedOrder() {
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}
