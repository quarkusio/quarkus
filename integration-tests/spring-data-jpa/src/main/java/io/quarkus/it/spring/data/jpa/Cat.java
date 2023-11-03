package io.quarkus.it.spring.data.jpa;

import jakarta.persistence.Entity;

@Entity
public class Cat extends Mammal {

    private String breed;

    private String color;

    private boolean distinctive;

    public Cat() {
    }

    public Cat(String breed, String color) {
        this.breed = breed;
        this.color = color;
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
