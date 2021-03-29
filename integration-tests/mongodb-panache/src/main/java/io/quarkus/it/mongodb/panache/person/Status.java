package io.quarkus.it.mongodb.panache.person;

public enum Status {
    DEAD("I'm a Zombie"),
    ALIVE("I alive!");

    private final String value;

    private Status(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return this.value;
    }
}
