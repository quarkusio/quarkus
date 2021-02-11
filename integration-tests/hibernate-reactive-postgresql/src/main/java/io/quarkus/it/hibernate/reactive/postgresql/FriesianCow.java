package io.quarkus.it.hibernate.reactive.postgresql;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "Cow")
public class FriesianCow {

    @Id
    @GeneratedValue
    public Long id;

    public String name;

}
