package io.quarkus.it.temporal;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Version;

import org.hibernate.annotations.Temporal;

@Temporal(rowStart = "valid_from", rowEnd = "valid_to")
@Entity
public class TemporalProduct {

    @Id
    long id;

    @Version
    int version;

    String name;

    int price;

    public TemporalProduct() {
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
    }
}
