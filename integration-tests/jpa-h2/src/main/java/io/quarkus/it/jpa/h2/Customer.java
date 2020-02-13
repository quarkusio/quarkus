package io.quarkus.it.jpa.h2;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity
public abstract class Customer {

    @Id
    @GeneratedValue
    private Integer id;

    String externalcode;

}
