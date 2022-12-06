package io.quarkus.it.panache;

import jakarta.persistence.Entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

@Entity
public class Address extends PanacheEntity implements Comparable<Address> {

    public String street;

    public Address() {
    }

    public Address(String street) {
        this.street = street;
    }

    @Override
    public int compareTo(Address address) {
        return street.compareTo(address.street);
    }
}
