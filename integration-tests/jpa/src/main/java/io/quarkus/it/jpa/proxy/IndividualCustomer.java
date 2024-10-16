package io.quarkus.it.jpa.proxy;

import jakarta.persistence.Entity;

@Entity
public class IndividualCustomer extends Customer {
    String firstname;
    String surname;
}
