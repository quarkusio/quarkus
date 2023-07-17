package io.quarkus.it.hibernate.reactive.postgresql;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "Cow")
public class FriesianCow {

    @Id
    @GeneratedValue
    public Long id;

    public String name;

}
