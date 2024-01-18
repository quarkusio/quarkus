package io.quarkus.it.jackson.model;

public record Whale(
        double swimSpeed, String color) implements Mammal {
}
