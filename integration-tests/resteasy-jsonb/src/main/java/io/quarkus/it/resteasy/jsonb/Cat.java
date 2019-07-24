package io.quarkus.it.resteasy.jsonb;

// used to show that properties from the superclass are used
public class Cat extends Animal {

    private final String breed;

    public Cat(String color, int age, String breed) {
        super(color, age);
        this.breed = breed;
    }

    public String getBreed() {
        return breed;
    }

}
