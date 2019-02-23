package io.quarkus.example.panache;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import io.quarkus.panache.jpa.PanacheEntityBase;

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
}
