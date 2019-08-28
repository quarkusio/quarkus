package io.quarkus.it.spring.data.jpa;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity
public class Cat {

    @Id
    @GeneratedValue
    public Long id;

    private String breed;

    private String color;

    private boolean distinctive;

    public Cat() {
    }

    public Cat(String breed, String color) {
        this.breed = breed;
        this.color = color;
    }

    public Long getId() {
        return id;
    }

    public String getBreed() {
        return breed;
    }

    public String getColor() {
        return color;
    }

    public boolean isDistinctive() {
        return distinctive;
    }
}
