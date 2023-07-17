package io.quarkus.it.jpa.h2;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@Entity
public abstract class Customer {

    @Id
    @GeneratedValue
    private Integer id;

    String externalcode;

}
