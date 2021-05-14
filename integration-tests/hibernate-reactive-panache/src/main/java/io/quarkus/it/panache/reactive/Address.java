package io.quarkus.it.panache.reactive;

import javax.persistence.Entity;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;

@Entity
public class Address extends PanacheEntity<Address> implements Comparable<Address> {

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
