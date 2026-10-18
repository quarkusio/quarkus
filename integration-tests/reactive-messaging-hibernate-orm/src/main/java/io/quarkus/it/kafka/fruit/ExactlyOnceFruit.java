package io.quarkus.it.kafka.fruit;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;

@Entity
public class ExactlyOnceFruit extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    public String name;

    public ExactlyOnceFruit(String name) {
        this.name = name;
    }

    public ExactlyOnceFruit() {
    }
}
