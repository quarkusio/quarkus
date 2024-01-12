package io.quarkus.it.jackson.model;

public record Elephant(
        int hornLength, String continent) implements Mammal {
}
