package io.quarkus.it.jackson.model;

public class Whale implements Mammal {

    private final double swimSpeed;
    private final String color;

    public Whale(double swimSpeed, String color) {
        this.swimSpeed = swimSpeed;
        this.color = color;
    }

    public double getSwimSpeed() {
        return swimSpeed;
    }

    public String getColor() {
        return color;
    }
}
