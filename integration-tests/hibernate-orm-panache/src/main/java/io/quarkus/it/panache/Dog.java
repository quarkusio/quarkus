package io.quarkus.it.panache;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;

// custom id type
@Entity
public class Dog extends PanacheEntityBase {

    @Id
    @GeneratedValue
    public Integer id;

    public String name;

    public String race;

    @ManyToOne
    public Person owner;

    public Dog(String name, String race) {
        this.name = name;
        this.race = race;
    }

    public Dog() {
    }

}
