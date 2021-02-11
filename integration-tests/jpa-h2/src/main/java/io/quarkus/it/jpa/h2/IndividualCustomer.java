package io.quarkus.it.jpa.h2;

import javax.persistence.Entity;

@Entity
public class IndividualCustomer extends Customer {
    String firstname;
    String surname;
}
