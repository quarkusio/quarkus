package io.quarkus.it.jpa.h2;

import jakarta.persistence.Entity;

@Entity
public class IndividualCustomer extends Customer {
    String firstname;
    String surname;
}
