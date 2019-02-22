package io.quarkus.example.jpa.mariadb;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity
public class Address {

    private long id;
    private String street;

    public Address() {
    }

    public Address(String street) {
        this.street = street;
    }

    @Id
    @GeneratedValue
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String name) {
        this.street = name;
    }

    public void describeFully(StringBuilder sb) {
        sb.append("Address with id=").append(id).append(", street='").append(street).append("'");
    }
}
