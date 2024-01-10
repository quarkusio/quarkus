package io.quarkus.it.jackson.model;

public class Elephant implements Mammal {

    private final int hornLength;
    private final String continent;

    public Elephant(int hornLength, String continent) {
        this.hornLength = hornLength;
        this.continent = continent;
    }

    public int getHornLength() {
        return hornLength;
    }

    public String getContinent() {
        return continent;
    }
}
